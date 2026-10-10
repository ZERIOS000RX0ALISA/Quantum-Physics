package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

object ProjectFileManager {
    private const val TAG = "ProjectFileManager"

    fun getBaseDir(context: Context): File {
        // We use the external files directory to guarantee offline writeability on modern Android without permission prompt.
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val rootDir = File(base, "Quantum Physics")
        if (!rootDir.exists()) rootDir.mkdirs()
        return rootDir
    }

    fun initDirectoryStructure(context: Context) {
        val base = getBaseDir(context)
        val dirs = listOf("Projects", "Exports", "Templates", "Cache", "Settings")
        for (dirName in dirs) {
            val dir = File(base, dirName)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    fun getProjectsDir(context: Context): File {
        val dir = File(getBaseDir(context), "Projects")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExportsDir(context: Context): File {
        val dir = File(getBaseDir(context), "Exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createProjectFolder(context: Context, project: ProjectEntity): File {
        val folder = File(getProjectsDir(context), project.folderName)
        if (!folder.exists()) {
            folder.mkdirs()
            File(folder, "Graphs").mkdirs()
            File(folder, "Images").mkdirs()
            File(folder, "Notes").mkdirs()
        }
        return folder
    }

    fun writeProjectFiles(
        context: Context,
        project: ProjectEntity,
        samples: List<SensorSampleEntity>
    ): File? {
        try {
            val projectFolder = createProjectFolder(context, project)

            // 1. Write Project.rsproj (JSON specification)
            val rsprojFile = File(projectFolder, "Project.rsproj")
            val rsprojContent = generateRsprojJson(project, samples.size)
            rsprojFile.writeText(rsprojContent)

            // 2. Write Metadata.json
            val metadataFile = File(projectFolder, "Metadata.json")
            val metadataContent = generateMetadataJson(project)
            metadataFile.writeText(metadataContent)

            // 3. Write RawData.csv
            val csvFile = File(projectFolder, "RawData.csv")
            if (project.saveRawData) {
                writeCsvData(csvFile, samples)
            } else {
                csvFile.writeText("Timestamp_ms,Elapsed_ms,Sensor_Type,Value_X,Value_Y,Value_Z\n")
            }

            // 4. Write Research.pdf
            val pdfFile = File(projectFolder, "Research.pdf")
            generateResearchPdf(context, pdfFile, project, samples)

            return projectFolder
        } catch (e: Exception) {
            Log.e(TAG, "Error writing project files", e)
            return null
        }
    }

    private fun generateRsprojJson(project: ProjectEntity, sampleCount: Int): String {
        return """
        {
          "application": "Quantum Physics (Project RS)",
          "project_id": "${project.formattedId}",
          "project_name": "${project.name}",
          "description": "${project.description.replace("\"", "\\\"")}",
          "creation_timestamp": ${project.creationDate},
          "last_modified_timestamp": ${project.lastModified},
          "selected_sensors": [${project.selectedSensors.split(",").joinToString(",") { "\"$it\"" }}],
          "sampling_rate_hz": ${project.samplingRateHz},
          "experiment_duration_seconds": ${project.durationSeconds},
          "samples_recorded": $sampleCount,
          "has_raw_csv": ${project.saveRawData},
          "has_graphs": ${project.autoGenerateGraphs}
        }
        """.trimIndent()
    }

    private fun generateMetadataJson(project: ProjectEntity): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val formattedDate = dateFormat.format(Date(project.creationDate))
        val sensorArray = project.selectedSensors.split(",")
            .filter { it.isNotEmpty() }
            .joinToString(", ") { "\"$it\"" }

        return """
        {
          "projectId": "${project.formattedId}",
          "projectName": "${project.name}",
          "creationDate": "$formattedDate",
          "selectedSensors": [$sensorArray],
          "applicationVersion": "1.0",
          "language": "English"
        }
        """.trimIndent()
    }

    private fun writeCsvData(file: File, samples: List<SensorSampleEntity>) {
        val builder = StringBuilder()
        builder.append("Timestamp_ms,Elapsed_ms,Sensor_Type,Value_X,Value_Y,Value_Z\n")
        for (sample in samples) {
            builder.append("${sample.timestamp},${sample.elapsedMs},${sample.sensorType},${sample.valueX},${sample.valueY},${sample.valueZ}\n")
        }
        file.writeText(builder.toString())
    }

    private fun generateResearchPdf(
        context: Context,
        file: File,
        project: ProjectEntity,
        samples: List<SensorSampleEntity>
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        var pageNumber = 1

        // Paint setup
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(18, 48, 87) // Dark Slate Blue
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(80, 80, 80)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        val accentLinePaint = Paint().apply {
            color = Color.rgb(18, 48, 87)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val lightLinePaint = Paint().apply {
            color = Color.rgb(220, 220, 220)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        // --- PAGE 1: TITLE & EXPERIMENT INFO ---
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Decorative top bar
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 15f, Paint().apply { color = Color.rgb(18, 48, 87) })

        // Page margin constants
        val marginX = 50f
        var currentY = 60f

        // Title and Header
        canvas.drawText("QUANTUM PHYSICS RESEARCH REPORT", marginX, currentY, titlePaint)
        currentY += 15f
        canvas.drawText("Project Code: ${project.formattedId}", marginX, currentY, labelPaint)
        currentY += 15f
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, accentLinePaint)
        currentY += 30f

        // Metadata Fields
        canvas.drawText("PROJECT METADATA", marginX, currentY, headerPaint)
        currentY += 20f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val metadata = listOf(
            "Project Name:" to project.name,
            "Date Generated:" to dateFormat.format(Date()),
            "Sampling Rate:" to "${project.samplingRateHz} Hz",
            "Target Duration:" to "${project.durationSeconds} s",
            "Total Samples:" to "${samples.size} readings",
            "Save Raw CSV:" to if (project.saveRawData) "Enabled" else "Disabled"
        )

        for ((label, valText) in metadata) {
            canvas.drawText(label, marginX, currentY, textPaint.apply { typeface = Typeface.DEFAULT_BOLD })
            canvas.drawText(valText, marginX + 150f, currentY, textPaint.apply { typeface = Typeface.DEFAULT })
            currentY += 20f
        }
        currentY += 15f

        // Research Description
        canvas.drawText("RESEARCH DESCRIPTION", marginX, currentY, headerPaint)
        currentY += 20f

        val descriptionPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        // Wrap research description text
        val descText = project.description.ifEmpty { "No research description provided." }
        val words = descText.split(" ")
        var line = ""
        for (word in words) {
            if (descriptionPaint.measureText("$line $word") < (pageWidth - 2 * marginX)) {
                line = if (line.isEmpty()) word else "$line $word"
            } else {
                canvas.drawText(line, marginX, currentY, descriptionPaint)
                currentY += 15f
                line = word
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, marginX, currentY, descriptionPaint)
            currentY += 15f
        }
        currentY += 20f

        // Selected Sensors
        canvas.drawText("ACTIVE TRANSDUCERS / SENSORS", marginX, currentY, headerPaint)
        currentY += 20f
        val sensorList = project.selectedSensors.split(",").filter { it.isNotEmpty() }
        if (sensorList.isEmpty()) {
            canvas.drawText("No sensors selected.", marginX + 10f, currentY, descriptionPaint)
            currentY += 15f
        } else {
            for (sensor in sensorList) {
                canvas.drawRect(marginX + 5f, currentY - 8f, marginX + 12f, currentY - 1f, Paint().apply { color = Color.rgb(18, 48, 87) })
                canvas.drawText(sensor, marginX + 20f, currentY, descriptionPaint)
                currentY += 20f
            }
        }

        // Draw Footer
        drawFooter(canvas, pageWidth, pageHeight, pageNumber - 1)
        pdfDocument.finishPage(page)

        // --- PAGE 2: STATISTICAL ANALYSIS & MATHEMATICAL PROCESSING ---
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        currentY = 60f

        canvas.drawText("MATHEMATICAL ANALYSIS & STATISTICS", marginX, currentY, headerPaint)
        currentY += 15f
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, accentLinePaint)
        currentY += 30f

        // For each active sensor, compute statistics: Min, Max, Avg, StdDev
        val sensorsInDataset = samples.map { it.sensorType }.distinct()

        if (sensorsInDataset.isEmpty()) {
            canvas.drawText("No mathematical data captured during this session.", marginX, currentY, textPaint)
        } else {
            for (sensorType in sensorsInDataset) {
                if (currentY > pageHeight - 150) {
                    // Finish current page and start a new page
                    drawFooter(canvas, pageWidth, pageHeight, pageNumber - 1)
                    pdfDocument.finishPage(page)

                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 60f
                }

                canvas.drawText(sensorType.uppercase(Locale.US), marginX, currentY, textPaint.apply { typeface = Typeface.DEFAULT_BOLD; textSize = 12f })
                currentY += 15f

                val sensorSamples = samples.filter { it.sensorType == sensorType }
                val xVals = sensorSamples.map { it.valueX }
                val yVals = sensorSamples.map { it.valueY }
                val zVals = sensorSamples.map { it.valueZ }

                // Table structure
                val colWidth = 100f
                val startX = marginX + 10f

                // Draw Table Header
                canvas.drawRect(startX - 5f, currentY - 12f, startX + 4 * colWidth, currentY + 6f, Paint().apply { color = Color.rgb(240, 240, 240) })
                canvas.drawText("Dimension", startX, currentY, descriptionPaint.apply { typeface = Typeface.DEFAULT_BOLD })
                canvas.drawText("Min Value", startX + colWidth, currentY, descriptionPaint)
                canvas.drawText("Max Value", startX + 2 * colWidth, currentY, descriptionPaint)
                canvas.drawText("Average", startX + 3 * colWidth, currentY, descriptionPaint)
                currentY += 20f

                // Compute and Draw for X, Y, Z
                val axes = listOf("X-Axis" to xVals, "Y-Axis" to yVals, "Z-Axis" to zVals)
                for ((axisName, vals) in axes) {
                    if (vals.isEmpty()) continue
                    val min = vals.minOrNull() ?: 0f
                    val max = vals.maxOrNull() ?: 0f
                    val avg = vals.avg()
                    val std = vals.stdDev(avg)

                    canvas.drawText(axisName, startX, currentY, descriptionPaint.apply { typeface = Typeface.DEFAULT_BOLD })
                    canvas.drawText(String.format("%.4f", min), startX + colWidth, currentY, descriptionPaint.apply { typeface = Typeface.DEFAULT })
                    canvas.drawText(String.format("%.4f", max), startX + 2 * colWidth, currentY, descriptionPaint)
                    canvas.drawText(String.format("%.4f", avg), startX + 3 * colWidth, currentY, descriptionPaint)
                    currentY += 15f
                    canvas.drawText("  (Std Dev)", startX, currentY, descriptionPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) })
                    canvas.drawText(String.format("± %.4f", std), startX + 3 * colWidth, currentY, descriptionPaint.apply { typeface = Typeface.DEFAULT })
                    currentY += 20f
                    canvas.drawLine(startX - 5f, currentY - 10f, startX + 4 * colWidth, currentY - 10f, lightLinePaint)
                }
                currentY += 15f
            }
        }

        drawFooter(canvas, pageWidth, pageHeight, pageNumber - 1)
        pdfDocument.finishPage(page)

        // --- PAGE 3: DYNAMIC GRAPHS ---
        if (project.autoGenerateGraphs && sensorsInDataset.isNotEmpty()) {
            for (sensorType in sensorsInDataset) {
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 60f

                canvas.drawText("GRAPHICAL VISUALIZATION: ${sensorType.uppercase(Locale.US)}", marginX, currentY, headerPaint)
                currentY += 15f
                canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, accentLinePaint)
                currentY += 40f

                val sensorSamples = samples.filter { it.sensorType == sensorType }
                drawPdfGraph(canvas, sensorSamples, marginX, currentY, pageWidth - 2 * marginX, 300f, sensorType)

                // Add descriptive results notes below
                currentY += 340f
                canvas.drawText("OBSERVATIONS & SCIENTIFIC NOTES", marginX, currentY, headerPaint)
                currentY += 20f

                val notesPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    isAntiAlias = true
                }

                val notesList = listOf(
                    "• The system automatically rendered this graph from ${sensorSamples.size} consecutive measurement epochs.",
                    "• Sampling rate was set to ${project.samplingRateHz} Hz with target experiment duration of ${project.durationSeconds} s.",
                    "• Maximum deviations and statistical bounds are mapped in accordance with Newtonian kinetics and physical standard calibrations.",
                    "• External AI analyzers may consume this report's raw structural metadata to reconstruct spatial kinematics."
                )

                for (noteLine in notesList) {
                    canvas.drawText(noteLine, marginX, currentY, notesPaint)
                    currentY += 18f
                }

                drawFooter(canvas, pageWidth, pageHeight, pageNumber - 1)
                pdfDocument.finishPage(page)
            }
        }

        // Write the PDF
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
    }

    private fun drawFooter(canvas: Canvas, pageWidth: Int, pageHeight: Int, pageNum: Int) {
        val paint = Paint().apply {
            color = Color.rgb(120, 120, 120)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("Quantum Physics (Project RS) — Confidentially Generated Lab Report", 50f, pageHeight - 30f, paint)
        canvas.drawText("Page $pageNum", pageWidth - 100f, pageHeight - 30f, paint)
    }

    private fun drawPdfGraph(
        canvas: Canvas,
        samples: List<SensorSampleEntity>,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String
    ) {
        // Draw graph background box
        val bgPaint = Paint().apply {
            color = Color.rgb(248, 248, 250)
            style = Paint.Style.FILL
        }
        canvas.drawRect(x, y, x + width, y + height, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRect(x, y, x + width, y + height, borderPaint)

        if (samples.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = Color.GRAY
                textSize = 12f
                isAntiAlias = true
            }
            canvas.drawText("No sample data to render", x + width / 2f - 70f, y + height / 2f, emptyPaint)
            return
        }

        // Graph boundaries
        val paddingLeft = 40f
        val paddingRight = 20f
        val paddingTop = 30f
        val paddingBottom = 40f

        val graphX = x + paddingLeft
        val graphY = y + paddingTop
        val graphW = width - paddingLeft - paddingRight
        val graphH = height - paddingTop - paddingBottom

        // Compute bounds
        val minTime = samples.first().elapsedMs.toFloat()
        val maxTime = samples.last().elapsedMs.toFloat()
        val timeDelta = if (maxTime > minTime) maxTime - minTime else 1f

        val xVals = samples.map { it.valueX }
        val yVals = samples.map { it.valueY }
        val zVals = samples.map { it.valueZ }
        val allVals = xVals + yVals + zVals
        var minVal = allVals.minOrNull() ?: -10f
        var maxVal = allVals.maxOrNull() ?: 10f
        if (maxVal == minVal) {
            maxVal += 1f
            minVal -= 1f
        }
        val valDelta = maxVal - minVal

        // Grid lines & Axis labels
        val gridPaint = Paint().apply {
            color = Color.rgb(230, 230, 230)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8f
            isAntiAlias = true
        }

        // Draw horizontal grid lines (values)
        val gridCount = 4
        for (i in 0..gridCount) {
            val gridY = graphY + graphH - (i * (graphH / gridCount))
            canvas.drawLine(graphX, gridY, graphX + graphW, gridY, gridPaint)

            val gridVal = minVal + i * (valDelta / gridCount)
            canvas.drawText(String.format("%.2f", gridVal), x + 5f, gridY + 3f, labelPaint)
        }

        // Draw vertical grid lines (time)
        for (i in 0..gridCount) {
            val gridX = graphX + i * (graphW / gridCount)
            canvas.drawLine(gridX, graphY, gridX, graphY + graphH, gridPaint)

            val gridTime = (minTime + i * (timeDelta / gridCount)) / 1000f // in seconds
            canvas.drawText(String.format("%.1fs", gridTime), gridX - 10f, graphY + graphH + 15f, labelPaint)
        }

        // Paths for X, Y, Z
        val pathXPaint = Paint().apply {
            color = Color.rgb(220, 50, 50) // Red
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val pathYPaint = Paint().apply {
            color = Color.rgb(50, 180, 50) // Green
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val pathZPaint = Paint().apply {
            color = Color.rgb(50, 50, 220) // Blue
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        fun drawLineSeries(vals: List<Float>, paint: Paint) {
            if (vals.size < 2) return
            for (i in 0 until vals.size - 1) {
                val sample1 = samples[i]
                val sample2 = samples[i + 1]

                val px1 = graphX + ((sample1.elapsedMs - minTime) / timeDelta) * graphW
                val py1 = graphY + graphH - ((vals[i] - minVal) / valDelta) * graphH

                val px2 = graphX + ((sample2.elapsedMs - minTime) / timeDelta) * graphW
                val py2 = graphY + graphH - ((vals[i + 1] - minVal) / valDelta) * graphH

                // Clip within graph window boundaries safely
                if (px1 >= graphX && px2 <= graphX + graphW) {
                    canvas.drawLine(px1, py1, px2, py2, paint)
                }
            }
        }

        drawLineSeries(xVals, pathXPaint)
        drawLineSeries(yVals, pathYPaint)
        drawLineSeries(zVals, pathZPaint)

        // Draw Legend and Title
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("$title Measurements Over Time", graphX, y + 20f, titlePaint)

        // Draw Legend
        val legendPaint = Paint().apply {
            textSize = 8f
            isAntiAlias = true
        }
        val legendXStart = graphX + graphW - 140f
        val legendY = y + 18f

        canvas.drawRect(legendXStart, legendY - 6f, legendXStart + 10f, legendY, Paint().apply { color = Color.rgb(220, 50, 50) })
        canvas.drawText("X-Axis", legendXStart + 15f, legendY, legendPaint)

        canvas.drawRect(legendXStart + 45f, legendY - 6f, legendXStart + 55f, legendY, Paint().apply { color = Color.rgb(50, 180, 50) })
        canvas.drawText("Y-Axis", legendXStart + 60f, legendY, legendPaint)

        canvas.drawRect(legendXStart + 90f, legendY - 6f, legendXStart + 100f, legendY, Paint().apply { color = Color.rgb(50, 50, 220) })
        canvas.drawText("Z-Axis", legendXStart + 105f, legendY, legendPaint)
    }

    fun deleteProjectFolder(context: Context, project: ProjectEntity): Boolean {
        return try {
            val folder = File(getProjectsDir(context), project.folderName)
            if (folder.exists()) {
                folder.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting folder", e)
            false
        }
    }

    fun exportProjectZip(context: Context, project: ProjectEntity): File? {
        // Zip the project folder and place it in Exports/
        try {
            val projectFolder = File(getProjectsDir(context), project.folderName)
            if (!projectFolder.exists()) return null

            val exportsDir = getExportsDir(context)
            val zipFile = File(exportsDir, "${project.formattedId}_Export.zip")

            java.util.zip.ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                projectFolder.walkTopDown().forEach { file ->
                    val relativePath = file.relativeTo(projectFolder).path
                    if (relativePath.isNotEmpty()) {
                        val zipEntry = java.util.zip.ZipEntry(relativePath + if (file.isDirectory) "/" else "")
                        zos.putNextEntry(zipEntry)
                        if (file.isFile) {
                            file.inputStream().use { input -> input.copyTo(zos) }
                        }
                        zos.closeEntry()
                    }
                }
            }
            return zipFile
        } catch (e: Exception) {
            Log.e(TAG, "Error zipping project folder", e)
            return null
        }
    }
}

private fun List<Float>.avg() = if (isEmpty()) 0f else sum() / size
private fun List<Float>.stdDev(mean: Float): Float {
    if (size <= 1) return 0f
    val variance = map { (it - mean).pow(2) }.sum() / (size - 1)
    return sqrt(variance)
}

