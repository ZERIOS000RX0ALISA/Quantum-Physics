package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SensorSampleEntity
import java.util.Locale

@Composable
fun ScientificChart(
    samples: List<SensorSampleEntity>,
    sensorType: String,
    modifier: Modifier = Modifier
) {
    val sensorSamples = samples.filter { it.sensorType == sensorType }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        if (sensorSamples.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded dataset available for calibration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sensorType.uppercase(Locale.US),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    // Legend
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.sizeMarker(Color.Red))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("X", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.sizeMarker(Color.Green))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Y", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.sizeMarker(Color.Blue))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Z", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // The Canvas drawing
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val paddingLeft = 50f
                    val paddingRight = 20f
                    val paddingTop = 20f
                    val paddingBottom = 40f

                    val chartWidth = canvasWidth - paddingLeft - paddingRight
                    val chartHeight = canvasHeight - paddingTop - paddingBottom

                    // Draw Axes
                    drawLine(
                        color = onSurfaceColor.copy(alpha = 0.3f),
                        start = Offset(paddingLeft, paddingTop),
                        end = Offset(paddingLeft, canvasHeight - paddingBottom),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = onSurfaceColor.copy(alpha = 0.3f),
                        start = Offset(paddingLeft, canvasHeight - paddingBottom),
                        end = Offset(canvasWidth - paddingRight, canvasHeight - paddingBottom),
                        strokeWidth = 2f
                    )

                    val minTime = sensorSamples.first().elapsedMs.toFloat()
                    val maxTime = sensorSamples.last().elapsedMs.toFloat()
                    val timeDelta = if (maxTime > minTime) maxTime - minTime else 1f

                    val xVals = sensorSamples.map { it.valueX }
                    val yVals = sensorSamples.map { it.valueY }
                    val zVals = sensorSamples.map { it.valueZ }
                    val allVals = xVals + yVals + zVals

                    var minVal = allVals.minOrNull() ?: -10f
                    var maxVal = allVals.maxOrNull() ?: 10f
                    if (maxVal == minVal) {
                        maxVal += 1f
                        minVal -= 1f
                    }
                    val valDelta = maxVal - minVal

                    // Draw vertical and horizontal grids
                    val gridCount = 4
                    for (i in 0..gridCount) {
                        // Horizontal Grid lines & labels
                        val gridY = paddingTop + chartHeight - (i * (chartHeight / gridCount))
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, gridY),
                            end = Offset(canvasWidth - paddingRight, gridY),
                            strokeWidth = 1f
                        )

                        val gridVal = minVal + i * (valDelta / gridCount)
                        // Label values on Y-axis
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1f", gridVal),
                            5f,
                            gridY + 4f,
                            android.graphics.Paint().apply {
                                color = onSurfaceColor.hashCode()
                                textSize = 22f
                            }
                        )

                        // Vertical Grid lines & labels
                        val gridX = paddingLeft + i * (chartWidth / gridCount)
                        drawLine(
                            color = gridColor,
                            start = Offset(gridX, paddingTop),
                            end = Offset(gridX, canvasHeight - paddingBottom),
                            strokeWidth = 1f
                        )

                        val gridTimeS = (minTime + i * (timeDelta / gridCount)) / 1000f
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1fs", gridTimeS),
                            gridX - 20f,
                            canvasHeight - 10f,
                            android.graphics.Paint().apply {
                                color = onSurfaceColor.hashCode()
                                textSize = 22f
                            }
                        )
                    }

                    // Render lines for X, Y, Z
                    fun drawLineSeries(vals: List<Float>, color: Color) {
                        if (vals.size < 2) return
                        val path = Path()
                        for (i in vals.indices) {
                            val sample = sensorSamples[i]
                            val px = paddingLeft + ((sample.elapsedMs - minTime) / timeDelta) * chartWidth
                            val py = paddingTop + chartHeight - ((vals[i] - minVal) / valDelta) * chartHeight

                            if (i == 0) {
                                path.moveTo(px, py)
                            } else {
                                path.lineTo(px, py)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 3f)
                        )
                    }

                    drawLineSeries(xVals, Color.Red)
                    drawLineSeries(yVals, Color.Green)
                    drawLineSeries(zVals, Color.Blue)
                }
            }
        }
    }
}

@Composable
private fun Modifier.sizeMarker(color: Color): Modifier = this
    .width(12.dp)
    .height(12.dp)
    .background(color = color, shape = RoundedCornerShape(2.dp))
