package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

class ProjectRepository(private val dao: PhysicsDao) {

    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    fun getProjectById(id: Int): Flow<ProjectEntity?> = dao.getProjectById(id)

    fun getSamplesForProject(projectId: Int): Flow<List<SensorSampleEntity>> = dao.getSamplesForProject(projectId)

    suspend fun insertProject(project: ProjectEntity): Long {
        return dao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) {
        dao.updateProject(project)
    }

    suspend fun deleteProject(context: Context, project: ProjectEntity) {
        // Delete folder files first
        ProjectFileManager.deleteProjectFolder(context, project)
        // Delete raw database entries
        dao.deleteSamplesForProject(project.id)
        dao.deleteProjectById(project.id)
    }

    suspend fun duplicateProject(context: Context, projectId: Int): Long {
        val original = dao.getProjectByIdSync(projectId) ?: return -1L
        val originalSamples = dao.getSamplesForProjectSync(projectId)

        val duplicateEntity = ProjectEntity(
            name = "${original.name} (Copy)",
            description = original.description,
            selectedSensors = original.selectedSensors,
            samplingRateHz = original.samplingRateHz,
            durationSeconds = original.durationSeconds,
            saveRawData = original.saveRawData,
            autoGenerateGraphs = original.autoGenerateGraphs,
            notes = original.notes,
            creationDate = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        val newId = dao.insertProject(duplicateEntity).toInt()
        if (newId > 0) {
            val newProject = dao.getProjectByIdSync(newId)
            if (newProject != null) {
                // Duplicate samples in db
                val copiedSamples = originalSamples.map {
                    it.copy(id = 0, projectId = newId)
                }
                dao.insertSamples(copiedSamples)

                // Write duplicated project files
                ProjectFileManager.writeProjectFiles(context, newProject, copiedSamples)
            }
        }
        return newId.toLong()
    }

    suspend fun saveExperimentResult(
        context: Context,
        projectId: Int,
        samples: List<SensorSampleEntity>
    ): Boolean {
        val project = dao.getProjectByIdSync(projectId) ?: return false

        // 1. Insert samples into database
        dao.insertSamples(samples)

        // 2. Update last modified date
        val updatedProject = project.copy(lastModified = System.currentTimeMillis())
        dao.updateProject(updatedProject)

        // 3. Write physical folder structure, CSV, and PDF Lab Report
        val folder = ProjectFileManager.writeProjectFiles(context, updatedProject, samples)

        return folder != null
    }

    suspend fun rebuildProjectFiles(context: Context, projectId: Int) {
        val project = dao.getProjectByIdSync(projectId) ?: return
        val samples = dao.getSamplesForProjectSync(projectId)
        ProjectFileManager.writeProjectFiles(context, project, samples)
    }

    suspend fun exportPdfToPublic(context: Context, project: ProjectEntity): File? {
        val projectFolder = File(ProjectFileManager.getProjectsDir(context), project.folderName)
        val sourcePdf = File(projectFolder, "Research.pdf")
        if (!sourcePdf.exists()) {
            rebuildProjectFiles(context, project.id)
        }
        if (!sourcePdf.exists()) return null

        val publicExportsDir = ProjectFileManager.getExportsDir(context)
        val destPdf = File(publicExportsDir, "${project.formattedId}_Research_Report.pdf")
        sourcePdf.copyTo(destPdf, overwrite = true)
        return destPdf
    }

    suspend fun exportCsvToPublic(context: Context, project: ProjectEntity): File? {
        val projectFolder = File(ProjectFileManager.getProjectsDir(context), project.folderName)
        val sourceCsv = File(projectFolder, "RawData.csv")
        if (!sourceCsv.exists()) {
            rebuildProjectFiles(context, project.id)
        }
        if (!sourceCsv.exists()) return null

        val publicExportsDir = ProjectFileManager.getExportsDir(context)
        val destCsv = File(publicExportsDir, "${project.formattedId}_RawData.csv")
        sourceCsv.copyTo(destCsv, overwrite = true)
        return destCsv
    }

    fun exportZipToPublic(context: Context, project: ProjectEntity): File? {
        return ProjectFileManager.exportProjectZip(context, project)
    }
}
