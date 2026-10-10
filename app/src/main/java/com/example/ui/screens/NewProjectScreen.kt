package com.example.ui.screens

import android.hardware.Sensor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.sensors.SensorHelper
import com.example.ui.translation.AppLanguage
import com.example.ui.translation.Translation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    viewModel: MainViewModel,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val name by viewModel.formName.collectAsState()
    val desc by viewModel.formDescription.collectAsState()
    val selectedSensors by viewModel.formSelectedSensors.collectAsState()
    val samplingRate by viewModel.formSamplingRate.collectAsState()
    val duration by viewModel.formDuration.collectAsState()
    val saveRaw by viewModel.formSaveRaw.collectAsState()
    val autoGraphs by viewModel.formAutoGraphs.collectAsState()

    var showAdvancedSensors by remember { mutableStateOf(false) }

    val allDeviceSensors = viewModel.availableSensors
    // Distinct sensors by scientific name
    val uniqueSensors = allDeviceSensors.distinctBy { SensorHelper.getScientificName(it.type) }
    val primarySensors = uniqueSensors.filter { SensorHelper.isPrimarySensor(it.type) }
    val advancedSensors = uniqueSensors.filter { !SensorHelper.isPrimarySensor(it.type) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Translation.getString("new_project_rs", lang).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = Translation.getString("scientific_form", lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.formName.value = it },
                label = { Text(Translation.getString("project_name", lang)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_project_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                ),
                singleLine = true
            )

            // Description Field
            OutlinedTextField(
                value = desc,
                onValueChange = { viewModel.formDescription.value = it },
                label = { Text(Translation.getString("research_desc", lang)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("input_project_desc"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                ),
                maxLines = 4
            )

            // Sensor Selection List
            Text(
                text = Translation.getString("select_sensors", lang).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Primary Sensors
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                primarySensors.forEach { sensor ->
                    SensorRow(
                        sensor = sensor,
                        isSelected = selectedSensors.contains(sensor.type),
                        onToggle = { viewModel.toggleSensorSelection(sensor.type) }
                    )
                }
            }

            // Advanced Sensors (Hidden under expandable section)
            if (advancedSensors.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAdvancedSensors = !showAdvancedSensors }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ADVANCED SENSORS",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (showAdvancedSensors) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Advanced Sensors",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (showAdvancedSensors) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        advancedSensors.forEach { sensor ->
                            SensorRow(
                                sensor = sensor,
                                isSelected = selectedSensors.contains(sensor.type),
                                onToggle = { viewModel.toggleSensorSelection(sensor.type) }
                            )
                        }
                    }
                }
            }

            // Sampling Rate (Hz)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${Translation.getString("sampling_rate", lang).uppercase()}: $samplingRate Hz",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 20, 50, 100).forEach { rate ->
                        SegmentedButton(
                            text = "$rate Hz",
                            isSelected = samplingRate == rate,
                            onClick = { viewModel.formSamplingRate.value = rate },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Duration (seconds)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val durationText = if (duration == 0) "INFINITE" else "$duration s"
                Text(
                    text = "${Translation.getString("duration", lang).uppercase()}: $durationText",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 30, 60, 300, 0).forEach { time ->
                        val btnText = if (time == 0) "INF" else "${time}s"
                        SegmentedButton(
                            text = btnText,
                            isSelected = duration == time,
                            onClick = { viewModel.formDuration.value = time },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Save Raw & Auto Graphs Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translation.getString("save_raw", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = saveRaw,
                    onCheckedChange = { viewModel.formSaveRaw.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translation.getString("auto_graphs", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = autoGraphs,
                    onCheckedChange = { viewModel.formAutoGraphs.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start Experiment Button
            Button(
                onClick = { viewModel.startExperiment() },
                enabled = name.isNotEmpty() && selectedSensors.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_start_experiment"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translation.getString("start_experiment", lang).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SensorRow(
    sensor: Sensor,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val displayScientificName = SensorHelper.getScientificName(sensor.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = displayScientificName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sensor.vendor,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SegmentedButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
