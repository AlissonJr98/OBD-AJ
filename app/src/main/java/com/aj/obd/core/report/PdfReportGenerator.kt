package com.aj.obd.core.report

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.aj.obd.domain.model.DtcFault
import com.aj.obd.domain.model.LiveSensor
import com.aj.obd.domain.model.Vehicle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    fun generateDiagnosticReport(
        vehicle: Vehicle,
        ecuName: String,
        faults: List<DtcFault>,
        sensors: List<LiveSensor>,
        workshopName: String = "OFICINA MECÂNICA ESPECIALIZADA",
        technicianName: String = "Técnico Especialista",
        clientPlate: String = "ABC-1234",
        odometerKm: String = "142.500 km"
    ): Result<File> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 (595 x 842 pt)
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.DKGRAY
            }

            // Cabeçalho Preto/Laranja Esportivo OBD AJ
            paint.color = Color.rgb(20, 20, 24)
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            paint.color = Color.rgb(245, 158, 11) // Amber Glow
            canvas.drawRect(0f, 86f, 595f, 90f, paint)

            // Título
            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("OBD AJ - LAUDO DE DIAGNÓSTICO VEICULAR", 30f, 42f, titlePaint)

            val subTitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(245, 158, 11)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("SISTEMA AVANÇADO DE TELEMETRIA E DIAGNÓSTICO ELETRÔNICO", 30f, 62f, subTitlePaint)

            val now = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            val datePaint = Paint().apply {
                isAntiAlias = true
                color = Color.LTGRAY
                textSize = 9f
            }
            canvas.drawText("Gerado em: $now", 30f, 78f, datePaint)

            var y = 120f

            // Seção 1: Dados da Oficina e do Veículo
            drawSectionHeader(canvas, "1. DADOS DA OFICINA & VEÍCULO", y)
            y += 24f

            textPaint.textSize = 10f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.color = Color.BLACK
            canvas.drawText("Oficina: $workshopName", 35f, y, textPaint)
            canvas.drawText("Placa: $clientPlate", 340f, y, textPaint)
            y += 16f

            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = Color.DKGRAY
            canvas.drawText("Responsável: $technicianName", 35f, y, textPaint)
            canvas.drawText("Km Atual: $odometerKm", 340f, y, textPaint)
            y += 16f

            canvas.drawText("Veículo: ${vehicle.brand} ${vehicle.name} (${vehicle.years})", 35f, y, textPaint)
            canvas.drawText("Motorização: ${vehicle.engineOptions}", 340f, y, textPaint)
            y += 16f

            canvas.drawText("Central Consultada: $ecuName", 35f, y, textPaint)
            canvas.drawText("Protocolo: ${vehicle.supportedEcus.firstOrNull()?.protocol?.name ?: "OBD2 / KWP2000"}", 340f, y, textPaint)
            y += 25f

            // Seção 2: Diagnóstico de Falhas (DTC)
            drawSectionHeader(canvas, "2. CÓDIGOS DE FALHA IDENTIFICADOS (DTC)", y)
            y += 22f

            if (faults.isEmpty()) {
                val greenPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(16, 185, 129)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("✔ NENHUMA FALHA REGISTRADA NA MEMÓRIA DA CENTRAL.", 35f, y, greenPaint)
                y += 16f
                textPaint.textSize = 9f
                textPaint.color = Color.GRAY
                canvas.drawText("Todos os sistemas e parâmetros operacionais estão dentro da conformidade.", 35f, y, textPaint)
                y += 26f
            } else {
                // Tabela de DTCs
                paint.color = Color.rgb(240, 240, 245)
                canvas.drawRect(30f, y - 10f, 565f, y + 8f, paint)

                val headerPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.BLACK
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("CÓDIGO", 35f, y + 2f, headerPaint)
                canvas.drawText("DESCRIÇÃO TÉCNICA", 110f, y + 2f, headerPaint)
                canvas.drawText("SEVERIDADE", 450f, y + 2f, headerPaint)
                canvas.drawText("STATUS", 520f, y + 2f, headerPaint)
                y += 18f

                faults.forEach { dtc ->
                    val codePaint = Paint().apply {
                        isAntiAlias = true
                        color = Color.rgb(220, 38, 38)
                        textSize = 10f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    }
                    canvas.drawText(dtc.code, 35f, y, codePaint)

                    val descPaint = Paint().apply {
                        isAntiAlias = true
                        color = Color.BLACK
                        textSize = 9f
                    }
                    val shortDesc = if (dtc.description.length > 55) dtc.description.take(52) + "..." else dtc.description
                    canvas.drawText(shortDesc, 110f, y, descPaint)

                    val sevPaint = Paint().apply {
                        isAntiAlias = true
                        color = Color.rgb(180, 83, 9)
                        textSize = 9f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    canvas.drawText(dtc.severity.label, 450f, y, sevPaint)

                    val statusPaint = Paint().apply {
                        isAntiAlias = true
                        color = Color.DKGRAY
                        textSize = 9f
                    }
                    canvas.drawText("Presente", 520f, y, statusPaint)

                    y += 16f
                }
                y += 15f
            }

            // Seção 3: Instantâneo dos Sensores de Telemetria
            drawSectionHeader(canvas, "3. LEITURAS DOS SENSORES EM TEMPO REAL", y)
            y += 22f

            val boxPaint = Paint().apply {
                color = Color.rgb(248, 249, 250)
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = Color.rgb(220, 224, 230)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            // Desenha cards para até 6 sensores
            var col = 0
            var rowY = y
            sensors.take(6).forEach { sensor ->
                val startX = 35f + col * 175f
                canvas.drawRect(startX, rowY, startX + 165f, rowY + 38f, boxPaint)
                canvas.drawRect(startX, rowY, startX + 165f, rowY + 38f, borderPaint)

                val sLabelPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.GRAY
                    textSize = 8f
                }
                canvas.drawText(sensor.shortName, startX + 8f, rowY + 14f, sLabelPaint)

                val sValPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.BLACK
                    textSize = 12f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText("%.1f %s".format(sensor.currentValue, sensor.unit), startX + 8f, rowY + 30f, sValPaint)

                col++
                if (col >= 3) {
                    col = 0
                    rowY += 44f
                }
            }
            y = rowY + 50f

            // Seção 4: Conclusão e Assinatura
            drawSectionHeader(canvas, "4. PARECER TÉCNICO & RECOMENDAÇÕES", y)
            y += 24f

            textPaint.textSize = 9f
            textPaint.color = Color.DKGRAY
            canvas.drawText("• Este laudo técnico reflete os parâmetros eletroeletrônicos no momento do escaneamento.", 35f, y, textPaint)
            y += 14f
            canvas.drawText("• Recomenda-se realizar teste de rodagem e nova conferência após as manutenções corretivas.", 35f, y, textPaint)
            y += 45f

            // Linha de Assinatura
            val linePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
            }
            canvas.drawLine(160f, y, 435f, y, linePaint)
            y += 14f

            val signPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Assinatura do Responsável Técnico / Oficina", 297f, y, signPaint)
            y += 12f
            signPaint.color = Color.GRAY
            canvas.drawText(workshopName, 297f, y, signPaint)

            // Rodapé
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.GRAY
                textSize = 8f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Documento emitido pelo aplicativo OBD AJ - Diagnóstico Automotivo Profissional", 297f, 825f, footerPaint)

            pdfDocument.finishPage(page)

            // Grava o arquivo PDF
            val reportsDir = File(context.getExternalFilesDir(null), "laudos_obdaj").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(reportsDir, "Laudo_OBDAJ_${clientPlate}_$timeStamp.pdf")

            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawSectionHeader(canvas: Canvas, title: String, y: Float) {
        val paint = Paint().apply {
            color = Color.rgb(235, 238, 242)
            style = Paint.Style.FILL
        }
        canvas.drawRect(30f, y - 12f, 565f, y + 4f, paint)

        val barPaint = Paint().apply {
            color = Color.rgb(245, 158, 11) // Amber Glow
            style = Paint.Style.FILL
        }
        canvas.drawRect(30f, y - 12f, 34f, y + 4f, barPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, 42f, y, titlePaint)
    }

    fun sharePdfReport(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Laudo Técnico de Diagnóstico - OBD AJ")
            putExtra(Intent.EXTRA_TEXT, "Segue anexo o Laudo Técnico de Diagnóstico Veicular gerado pelo OBD AJ.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Compartilhar Laudo em PDF via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
