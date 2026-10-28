package com.phonehardware.diagnostic.hardware

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.phonehardware.diagnostic.core.DiagnosticResult
import com.phonehardware.diagnostic.data.ResultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** All exports use Storage Access Framework; app never needs external-storage permission. */
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ResultRepository
) {
    suspend fun export(id: String, uri: Uri, format: String) {
        val output = context.contentResolver.openOutputStream(uri) ?: error("Cannot open destination")
        output.use { os ->
            when(format) {
                "json" -> os.write(repository.exportJson(id).toByteArray(Charsets.UTF_8))
                "csv" -> os.write(repository.exportCsv(id).toByteArray(Charsets.UTF_8))
                "pdf" -> {
                    val doc = pdf(repository.results(id).first())
                    try {
                        doc.writeTo(os)
                    } finally {
                        doc.close()
                    }
                }
                else -> error("Unsupported export format")
            }
        }
    }
    private fun pdf(results: List<DiagnosticResult>): PdfDocument {
        val pdf = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.rgb(30,50,85); textSize=19f; isFakeBoldText=true }
        val head = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.BLACK; textSize=12f; isFakeBoldText=true }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.DKGRAY; textSize=10f }
        val pageWidth = 595; val pageHeight = 842
        var page: PdfDocument.Page? = null
        var y = 0f; var pageNum = 0
        fun next() {
            page?.let { pdf.finishPage(it) }
            pageNum++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth,pageHeight,pageNum).create())
            y=45f
            page!!.canvas.drawText("PHONE HARDWARE DIAGNOSTIC",35f,y,title)
            y+=20f
            page!!.canvas.drawText("Report • system reports and device measurements are labeled separately",35f,y,body)
            y+=29f
        }
        fun line(text: String, p: Paint = body, height: Float = 17f) {
            if(page==null || y>790f) next()
            val maxLength = if(p==head) 74 else 95
            text.chunked(maxLength).forEach { segment ->
                if(y>790f) next()
                page!!.canvas.drawText(segment.replace("\n","  "),35f,y,p)
                y+=height
            }
        }
        next()
        line("Generated on device. An absent option is not automatically a defect.")
        results.forEach { r ->
            line("${r.category} / ${r.name} — ${r.outcome}",head,19f)
            line("Source: ${r.source}; ${java.util.Date(r.timestamp)}")
            r.metrics.forEach { m -> line("  ${m.name}: ${m.value} ${m.unit} [${m.evidence}]") }
            if(r.warning.isNotBlank()) line("Note: ${r.warning}")
            y+=8f
        }
        page?.let { pdf.finishPage(it) }
        return pdf
    }
}
