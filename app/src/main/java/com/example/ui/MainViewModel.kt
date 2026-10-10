package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PhysicsDatabase
import com.example.data.ProjectEntity
import com.example.data.ProjectFileManager
import com.example.data.ProjectRepository
import com.example.data.SensorSampleEntity
import com.example.ui.translation.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

sealed class Screen {
    object Home : Screen()
    object NewProject : Screen()
    data class ActiveExperiment(val projectId: Int) : Screen()
    data class ProjectDetails(val projectId: Int) : Screen()
    object Library : Screen()
    object Settings : Screen()
    object About : Screen()
}

enum class ExperimentStatus {
    IDLE, RUNNING, PAUSED, COMPLETED
}

enum class SortOption {
    DATE, NAME, ID
}

class MainViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val database = PhysicsDatabase.getDatabase(application)
    private val repository = ProjectRepository(database.physicsDao())
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Initialize directories
    init {
        ProjectFileManager.initDirectoryStructure(application)
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.ProjectDetails) {
            loadProjectDetails(screen.projectId)
        }
    }

    // Settings State (Persisted in-memory/preferences)
    private val prefs = application.getSharedPreferences("quantum_physics_prefs", Context.MODE_PRIVATE)
    private val _language = MutableStateFlow(AppLanguage.valueOf(prefs.getString("language", "EN") ?: "EN"))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _useSiUnits = MutableStateFlow(prefs.getBoolean("si_units", true))
    val useSiUnits: StateFlow<Boolean> = _useSiUnits.asStateFlow()

    val storageLocation: String
        get() = "${getApplication<Application>().getExternalFilesDir(null)?.absolutePath ?: "Internal Storage"}/Quantum Physics"

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString("language", lang.name).apply()
    }

    fun setTheme(dark: Boolean) {
        _isDarkTheme.value = dark
        prefs.edit().putBoolean("is_dark", dark).apply()
    }

    fun setSiUnits(si: Boolean) {
        _useSiUnits.value = si
        prefs.edit().putBoolean("si_units", si).apply()
    }

    // Projects State
    val projectsList: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Library Search & Sort
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(
        projectsList,
        _searchQuery,
        _sortOption
    ) { list, query, sort ->
        val filtered = if (query.isEmpty()) {
            list
        } else {
            list.filter { it.name.contains(query, ignoreCase = true) || it.formattedId.contains(query, ignoreCase = true) }
        }

        when (sort) {
            SortOption.DATE -> filtered.sortedByDescending { it.creationDate }
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.ID -> filtered.sortedBy { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    // New Project Form State
    var formName = MutableStateFlow("")
    var formDescription = MutableStateFlow("")
    var formSelectedSensors = MutableStateFlow<Set<Int>>(emptySet())
    var formSamplingRate = MutableStateFlow(20) // default 20 Hz
    var formDuration = MutableStateFlow(10) // default 10 seconds
    var formSaveRaw = MutableStateFlow(true)
    var formAutoGraphs = MutableStateFlow(true)

    // Device available sensors
    val availableSensors: List<Sensor> by lazy {
        sensorManager.getSensorList(Sensor.TYPE_ALL) ?: emptyList()
    }

    fun toggleSensorSelection(sensorType: Int) {
        val current = formSelectedSensors.value.toMutableSet()
        if (current.contains(sensorType)) {
            current.remove(sensorType)
        } else {
            current.add(sensorType)
        }
        formSelectedSensors.value = current
    }

    // Active Experiment Recording State
    private val _experimentStatus = MutableStateFlow(ExperimentStatus.IDLE)
    val experimentStatus: StateFlow<ExperimentStatus> = _experimentStatus.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount.asStateFlow()

    private val bufferedSamples = Collections.synchronizedList(mutableListOf<SensorSampleEntity>())
    private var experimentJob: Job? = null
    private var activeProjectId: Int = -1
    private var experimentStartTime = 0L
    private var pausedAccumulatedTime = 0L

    // Current Project Details State (For View Screen)
    private val _selectedProject = MutableStateFlow<ProjectEntity?>(null)
    val selectedProject: StateFlow<ProjectEntity?> = _selectedProject.asStateFlow()

    private val _selectedProjectSamples = MutableStateFlow<List<SensorSampleEntity>>(emptyList())
    val selectedProjectSamples: StateFlow<List<SensorSampleEntity>> = _selectedProjectSamples.asStateFlow()

    var projectNotesText = MutableStateFlow("")

    fun startExperiment() {
        if (formName.value.isEmpty()) return
        val sensorsString = formSelectedSensors.value.joinToString(",") { it.toString() }

        viewModelScope.launch(Dispatchers.IO) {
            val project = ProjectEntity(
                name = formName.value,
                description = formDescription.value,
                selectedSensors = sensorsString,
                samplingRateHz = formSamplingRate.value,
                durationSeconds = formDuration.value,
                saveRawData = formSaveRaw.value,
                autoGenerateGraphs = formAutoGraphs.value
            )

            val projectId = repository.insertProject(project).toInt()
            activeProjectId = projectId
            bufferedSamples.clear()
            _sampleCount.value = 0
            _elapsedMs.value = 0L
            experimentStartTime = System.currentTimeMillis()
            pausedAccumulatedTime = 0L

            _experimentStatus.value = ExperimentStatus.RUNNING

            // Register Sensor Listeners
            val delayUs = 1_000_000 / formSamplingRate.value
            launch(Dispatchers.Main) {
                formSelectedSensors.value.forEach { type ->
                    val sensor = sensorManager.getDefaultSensor(type)
                    if (sensor != null) {
                        sensorManager.registerListener(this@MainViewModel, sensor, delayUs)
                    }
                }
                _currentScreen.value = Screen.ActiveExperiment(projectId)
            }

            // Launch Chronometer Timer
            startTimer()
        }
    }

    private fun startTimer() {
        experimentJob?.cancel()
        experimentJob = viewModelScope.launch(Dispatchers.Default) {
            val durationMs = formDuration.value * 1000L
            val isInfinite = formDuration.value == 0

            while (_experimentStatus.value == ExperimentStatus.RUNNING) {
                val now = System.currentTimeMillis()
                val activeElapsed = now - experimentStartTime
                val totalElapsed = pausedAccumulatedTime + activeElapsed
                _elapsedMs.value = totalElapsed

                // Automatically stop if duration reached
                if (!isInfinite && totalElapsed >= durationMs) {
                    stopExperiment()
                    break
                }
                delay(50)
            }
        }
    }

    fun pauseExperiment() {
        if (_experimentStatus.value != ExperimentStatus.RUNNING) return
        _experimentStatus.value = ExperimentStatus.PAUSED
        experimentJob?.cancel()

        // Accumulate elapsed time and pause sensor triggers
        pausedAccumulatedTime += System.currentTimeMillis() - experimentStartTime
        sensorManager.unregisterListener(this)
    }

    fun resumeExperiment() {
        if (_experimentStatus.value != ExperimentStatus.PAUSED) return
        _experimentStatus.value = ExperimentStatus.RUNNING
        experimentStartTime = System.currentTimeMillis()

        // Re-register sensor listeners
        val delayUs = 1_000_000 / formSamplingRate.value
        formSelectedSensors.value.forEach { type ->
            val sensor = sensorManager.getDefaultSensor(type)
            if (sensor != null) {
                sensorManager.registerListener(this, sensor, delayUs)
            }
        }

        startTimer()
    }

    fun stopExperiment() {
        if (_experimentStatus.value == ExperimentStatus.IDLE || _experimentStatus.value == ExperimentStatus.COMPLETED) return

        sensorManager.unregisterListener(this)
        experimentJob?.cancel()

        _experimentStatus.value = ExperimentStatus.COMPLETED

        viewModelScope.launch(Dispatchers.IO) {
            // Save measurements to database & file system
            val snapshot = bufferedSamples.toList()
            repository.saveExperimentResult(getApplication(), activeProjectId, snapshot)

            // Reset form fields
            formName.value = ""
            formDescription.value = ""
            formSelectedSensors.value = emptySet()

            // Navigate to details of finished experiment
            launch(Dispatchers.Main) {
                navigateTo(Screen.ProjectDetails(activeProjectId))
            }
        }
    }

    // SensorEventListener overrides
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || _experimentStatus.value != ExperimentStatus.RUNNING) return

        val sensorName = com.example.ui.sensors.SensorHelper.getScientificName(event.sensor.type)
        val timestamp = System.currentTimeMillis()
        val currentElapsed = pausedAccumulatedTime + (timestamp - experimentStartTime)

        val x = event.values.getOrNull(0) ?: 0f
        val y = event.values.getOrNull(1) ?: 0f
        val z = event.values.getOrNull(2) ?: 0f

        val sample = SensorSampleEntity(
            projectId = activeProjectId,
            timestamp = timestamp,
            elapsedMs = currentElapsed,
            sensorType = sensorName,
            valueX = x,
            valueY = y,
            valueZ = z
        )

        bufferedSamples.add(sample)
        _sampleCount.value = bufferedSamples.size
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for scientific logging
    }

    // Load Project details for viewer
    private fun loadProjectDetails(projectId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getProjectById(projectId).collect { project ->
                if (project != null) {
                    _selectedProject.value = project
                    projectNotesText.value = project.notes

                    repository.getSamplesForProject(projectId).collect { samples ->
                        _selectedProjectSamples.value = samples
                    }
                }
            }
        }
    }

    fun saveProjectNotes() {
        val project = _selectedProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = project.copy(notes = projectNotesText.value, lastModified = System.currentTimeMillis())
            repository.updateProject(updated)
            _selectedProject.value = updated

            // Rebuild research report PDF with the new notes included
            repository.rebuildProjectFiles(getApplication(), project.id)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(getApplication(), project)
            launch(Dispatchers.Main) {
                navigateTo(Screen.Library)
            }
        }
    }

    fun duplicateProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.duplicateProject(getApplication(), project.id)
        }
    }

    fun exportPdf(project: ProjectEntity, onCompleted: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = repository.exportPdfToPublic(getApplication(), project)
            launch(Dispatchers.Main) { onCompleted(file) }
        }
    }

    fun exportCsv(project: ProjectEntity, onCompleted: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = repository.exportCsvToPublic(getApplication(), project)
            launch(Dispatchers.Main) { onCompleted(file) }
        }
    }

    fun exportZip(project: ProjectEntity, onCompleted: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = repository.exportZipToPublic(getApplication(), project)
            launch(Dispatchers.Main) { onCompleted(file) }
        }
    }
}
