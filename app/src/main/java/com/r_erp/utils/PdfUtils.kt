package com.r_erp.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.r_erp.api.SupabaseBudget
import com.r_erp.api.SupabaseConfig
import com.r_erp.api.SupabaseOrder
import com.r_erp.api.SupabasePayable
import com.r_erp.api.SupabasePayableTotal
import com.r_erp.api.SupabaseReceivable
import com.r_erp.api.SupabaseReceivableTotal
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfUtils {

    private val localeBR = Locale.forLanguageTag("pt-BR")

    fun generateAndShareReceivablesReport(
        context: Context,
        items: List<SupabaseReceivable>,
        clientMap: Map<Int?, String?> = emptyMap(),
        totals: SupabaseReceivableTotal? = null
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()
        val boldPaint = Paint()

        var y = 50f
        val margin = 50f
        val pageWidth = pageInfo.pageWidth.toFloat()

        // Header
        titlePaint.textSize = 20f
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Relatório de Contas a Receber", pageWidth / 2, y, titlePaint)
        y += 30f

        // Totals Summary Header
        if (totals != null) {
            paint.textSize = 10f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Total Pendente: ${String.format(localeBR, "%.2f", totals.outstanding ?: 0.0)}", margin, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Total Pago: ${String.format(localeBR, "%.2f", totals.paid ?: 0.0)}", pageWidth - margin, y, paint)
            y += 25f
        }
        paint.textAlign = Paint.Align.LEFT

        // Table Header
        boldPaint.textSize = 10f
        canvas.drawText("ID", margin, y, boldPaint)
        canvas.drawText("Cliente", margin + 40f, y, boldPaint)
        canvas.drawText("Origem", margin + 170f, y, boldPaint)
        canvas.drawText("Venc.", margin + 300f, y, boldPaint)
        
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Valor", margin + 420f, y, boldPaint)
        canvas.drawText("Pago", pageWidth - margin, y, boldPaint)
        boldPaint.textAlign = Paint.Align.LEFT
        
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 20f

        // Content
        paint.textSize = 9f
        items.forEach { item ->
            if (y > 800f) {
                // Not handling multiple pages for simplicity
            }
            canvas.drawText(item.id?.toString() ?: "", margin, y, paint)

            val client = clientMap[item.clientId] ?: item.clientFullName ?: "N/A"
            canvas.drawText(if (client.length > 25) client.substring(0, 22) + "..." else client, margin + 40f, y, paint)
            
            val desc = item.origin ?: ""
            canvas.drawText(if (desc.length > 25) desc.substring(0, 22) + "..." else desc, margin + 170f, y, paint)
            
            canvas.drawText(formatDate(item.dueDate), margin + 300f, y, paint)
            
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format(localeBR, "%.2f", item.value ?: 0.0), margin + 420f, y, paint)
            canvas.drawText(String.format(localeBR, "%.2f", item.paidValue ?: 0.0), pageWidth - margin, y, paint)
            paint.textAlign = Paint.Align.LEFT
            
            y += 20f
        }

        pdfDocument.finishPage(page)

        val fileName = "relatorio_recebimentos.pdf"
        val file = File(context.cacheDir, fileName)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        shareFile(context, file, "Relatório de Recebimentos")
    }

    fun generateAndSharePayablesReport(
        context: Context,
        items: List<SupabasePayable>,
        supplierMap: Map<Int?, String?> = emptyMap(),
        totals: SupabasePayableTotal? = null
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()
        val boldPaint = Paint()

        var y = 50f
        val margin = 50f
        val pageWidth = pageInfo.pageWidth.toFloat()

        // Header
        titlePaint.textSize = 20f
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Relatório de Contas a Pagar", pageWidth / 2, y, titlePaint)
        y += 30f

        // Totals Summary Header
        if (totals != null) {
            paint.textSize = 10f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Total Pendente: ${String.format(localeBR, "%.2f", totals.outstanding ?: 0.0)}", margin, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Total Pago: ${String.format(localeBR, "%.2f", totals.paid ?: 0.0)}", pageWidth - margin, y, paint)
            y += 25f
        }
        paint.textAlign = Paint.Align.LEFT

        // Table Header
        boldPaint.textSize = 10f
        canvas.drawText("ID", margin, y, boldPaint)
        canvas.drawText("Fornecedor", margin + 40f, y, boldPaint)
        canvas.drawText("Origem", margin + 170f, y, boldPaint)
        canvas.drawText("Venc.", margin + 300f, y, boldPaint)
        
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Valor", margin + 420f, y, boldPaint)
        canvas.drawText("Pago", pageWidth - margin, y, boldPaint)
        boldPaint.textAlign = Paint.Align.LEFT
        
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 20f

        // Content
        paint.textSize = 9f
        items.forEach { item ->
            if (y > 800f) {
                // Not handling multiple pages for simplicity
            }
            canvas.drawText(item.id?.toString() ?: "", margin, y, paint)

            val supplier = supplierMap[item.supplierId] ?: item.supplierFullName ?: "N/A"
            canvas.drawText(if (supplier.length > 25) supplier.substring(0, 22) + "..." else supplier, margin + 40f, y, paint)
            
            val desc = item.origin ?: ""
            canvas.drawText(if (desc.length > 25) desc.substring(0, 22) + "..." else desc, margin + 170f, y, paint)
            
            canvas.drawText(formatDate(item.dueDate), margin + 300f, y, paint)
            
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format(localeBR, "%.2f", item.value ?: 0.0), margin + 420f, y, paint)
            canvas.drawText(String.format(localeBR, "%.2f", item.paidValue ?: 0.0), pageWidth - margin, y, paint)
            paint.textAlign = Paint.Align.LEFT
            
            y += 20f
        }

        pdfDocument.finishPage(page)

        val fileName = "relatorio_pagamentos.pdf"
        val file = File(context.cacheDir, fileName)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        shareFile(context, file, "Relatório de Pagamentos")
    }

    fun generateAndShareBudgetPdf(
        context: Context, 
        budget: SupabaseBudget, 
        clientName: String? = null, 
        viaWhatsapp: Boolean = false,
        config: SupabaseConfig? = null
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()
        val boldPaint = Paint()

        var y = 50f
        val margin = 50f
        val pageWidth = pageInfo.pageWidth.toFloat()

        // New Header with Config Info
        if (config != null) {
            decodeLogo(config.logo)?.let { logoBitmap ->
                val logoHeight = 60f
                val logoWidth = logoBitmap.width * (logoHeight / logoBitmap.height)
                val destRect = RectF(margin, y, margin + logoWidth, y + logoHeight)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
                
                val headerPaint = Paint().apply {
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val textX = margin + logoWidth + 10f
                canvas.drawText(config.companyName ?: "", textX, y + 15f, headerPaint)
                headerPaint.typeface = Typeface.DEFAULT
                headerPaint.textSize = 10f
                canvas.drawText(config.companyAddress ?: "", textX, y + 35f, headerPaint)
                canvas.drawText(config.companyPhone ?: "", textX, y + 55f, headerPaint)
                y += 80f
            } ?: run {
                // If logo is null, still draw text
                val headerPaint = Paint().apply {
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(config.companyName ?: "", margin, y + 15f, headerPaint)
                headerPaint.typeface = Typeface.DEFAULT
                headerPaint.textSize = 10f
                canvas.drawText(config.companyAddress ?: "", margin, y + 35f, headerPaint)
                canvas.drawText(config.companyPhone ?: "", margin, y + 55f, headerPaint)
                y += 80f
            }
        }

        // Header: Center, double font size
        titlePaint.textSize = 24f
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Orçamento nº ${budget.id}", pageWidth / 2, y, titlePaint)
        y += 40f

        // Client Info: Nome + fullname (bold, left)
        paint.textSize = 12f
        boldPaint.textSize = 12f
        boldPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        canvas.drawText("Nome: ", margin, y, paint)
        val nomeLabelWidth = paint.measureText("Nome: ")
        canvas.drawText(clientName ?: budget.clientName ?: "N/A", margin + nomeLabelWidth, y, boldPaint)
        y += 20f

        // Next line: phone (left) and "Local" (right)
        val phone = budget.phone ?: "N/A"
        val city = budget.city ?: ""
        val state = budget.state ?: ""
        val local = if (city.isNotEmpty() || state.isNotEmpty()) "$city - $state" else "N/A"
        canvas.drawText("Telefone: $phone", margin, y, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Local: $local", pageWidth - margin, y, paint)
        paint.textAlign = Paint.Align.LEFT // Reset for subsequent drawing
        y += 40f // Give a row space

        // Items Table Header
        boldPaint.textSize = 10f
        canvas.drawText("Qtd.", margin, y, boldPaint)
        canvas.drawText("Descrição", margin + 50f, y, boldPaint)
        
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Preço unit.", margin + 370f, y, boldPaint)
        canvas.drawText("Desc.", margin + 450f, y, boldPaint)
        canvas.drawText("Total", pageWidth - margin, y, boldPaint)
        boldPaint.textAlign = Paint.Align.LEFT // Reset
        
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 20f

        // Table Content
        paint.textSize = 10f
        budget.items?.forEach { item ->
            if (y > 750f) { 
                // Simple page break check (not fully implemented for multi-page)
            }
            
            val initialY = y
            val desc = item.description ?: ""
            val descWidth = 310f // Space between Desc column and Price column
            val lines = wrapText(desc, paint, descWidth)
            
            // Draw item data on the first line
            canvas.drawText(String.format(localeBR, "%.2f", item.quantity ?: 0.0), margin, y, paint)
            
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format(localeBR, "%.2f", item.price ?: 0.0), margin + 370f, y, paint)
            canvas.drawText(String.format(localeBR, "%.2f", item.discount ?: 0.0), margin + 450f, y, paint)
            canvas.drawText(String.format(localeBR, "%.2f", item.total ?: 0.0), pageWidth - margin, y, paint)
            paint.textAlign = Paint.Align.LEFT
            
            // Draw description lines
            lines.forEach { line ->
                canvas.drawText(line, margin + 50f, y, paint)
                y += 15f
            }
            
            // Add a small gap between items if description was multi-line, 
            // otherwise y already advanced by at least one line (15f).
            // Let's ensure a minimum height per item row.
            val rowHeight = y - initialY
            if (rowHeight < 20f) {
                y = initialY + 20f
            } else {
                y += 5f // Small extra gap after multi-line description
            }
        }

        y += 20f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 20f

        // Footer: Right aligned
        paint.textAlign = Paint.Align.RIGHT
        val rightMargin = pageWidth - margin
        canvas.drawText("Total Itens: ${String.format(localeBR, "%.2f", budget.totalItems ?: 0.0)}", rightMargin, y, paint)
        y += 20f
        canvas.drawText("Desconto: ${String.format(localeBR, "%.2f", budget.discount ?: 0.0)}", rightMargin, y, paint)
        y += 20f
        boldPaint.textAlign = Paint.Align.RIGHT
        boldPaint.textSize = 12f
        canvas.drawText("TOTAL: ${String.format(localeBR, "%.2f", budget.total ?: 0.0)}", rightMargin, y, boldPaint)
        y += 30f

        if (!budget.message.isNullOrBlank()) {
            paint.textAlign = Paint.Align.LEFT
            boldPaint.textSize = 14f
            canvas.drawText("Mensagem: ${budget.message}", margin, y, paint)
            y += 30f
        }

        // Validity: "Orçamento válido até " + valid_until (DD/MM/YYYY)
        paint.textAlign = Paint.Align.LEFT
        val validUntilFormatted = formatDate(budget.validUntil)
        canvas.drawText("Orçamento válido até $validUntilFormatted", margin, y, paint)

        pdfDocument.finishPage(page)

        // Save to file
        val fileName = "orcamento_${budget.id}.pdf"
        val file = File(context.cacheDir, fileName)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        // Share the PDF
        if (viaWhatsapp) {
            shareViaWhatsapp(context, file, budget.phone, isOrder = false)
        } else {
            shareFile(context, file)
        }
    }

    private fun shareViaWhatsapp(context: Context, file: File, phone: String?, isOrder: Boolean = false) {
        val uri: Uri = FileProvider.getUriForFile(context, "com.r_erp.fileprovider", file)
        
        // Format phone number: remove non-digits and leading zero
        var digits = phone?.filter { it.isDigit() } ?: ""
        if (digits.startsWith("0")) digits = digits.substring(1)
        
        val targetPhone = when (digits.length) {
            10, 11 -> "55$digits"
            else -> digits
        }

        val whatsappPackage = "com.whatsapp"
        val whatsappBusinessPackage = "com.whatsapp.w4b"
        
        val isWhatsappInstalled = isPackageInstalled(whatsappPackage, context)
        val isWhatsappBusinessInstalled = isPackageInstalled(whatsappBusinessPackage, context)

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        
        val typeLabel = if (isOrder) "pedido" else "orçamento"
        val message = "Segue $typeLabel conforme solicitado"
        // Try multiple ways to set the caption/legend
        intent.putExtra(Intent.EXTRA_TEXT, message)
        intent.putExtra("android.intent.extra.TEXT", message)
        intent.putExtra("caption", message)
        
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (targetPhone.isNotEmpty()) {
            val packageToUse = when {
                isWhatsappBusinessInstalled -> whatsappBusinessPackage
                isWhatsappInstalled -> whatsappPackage
                else -> null
            }
            
            if (packageToUse != null) {
                intent.setPackage(packageToUse)
                intent.putExtra("jid", "$targetPhone@s.whatsapp.net")
                intent.putExtra("android.intent.extra.PHONE_NUMBER", targetPhone)
            }
        }

        try {
            // Remove Chooser if package is set to encourage direct opening
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent.createChooser(intent, "Enviar Orçamento"))
        }
    }

    private fun isPackageInstalled(packageName: String, context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }


    private fun shareFile(context: Context, file: File, title: String = "Compartilhar Orçamento") {
        val uri: Uri = FileProvider.getUriForFile(context, "com.r_erp.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun generateAndShareOrderPdf(
        context: Context, 
        order: SupabaseOrder, 
        clientName: String? = null, 
        viaWhatsapp: Boolean = false,
        config: SupabaseConfig? = null
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()
        val boldPaint = Paint()

        var y = 50f
        val margin = 50f
        val pageWidth = pageInfo.pageWidth.toFloat()

        // New Header with Config Info
        if (config != null) {
            val logoBitmap = decodeLogo(config.logo)
            var textX = margin
            if (logoBitmap != null) {
                val logoHeight = 60f
                val logoWidth = logoBitmap.width * (logoHeight / logoBitmap.height)
                val destRect = RectF(margin, y, margin + logoWidth, y + logoHeight)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
                textX += logoWidth + 10f
            }
            
            val headerPaint = Paint().apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            
            // Company Name
            canvas.drawText(config.companyName ?: "", textX, y + 15f, headerPaint)
            
            // Address and Phone
            headerPaint.typeface = Typeface.DEFAULT
            headerPaint.textSize = 10f
            canvas.drawText(config.companyAddress ?: "", textX, y + 35f, headerPaint)
            canvas.drawText(config.companyPhone ?: "", textX, y + 55f, headerPaint)
            
            y += 80f
        }

        // Header: Center, double font size
        titlePaint.textSize = 24f
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Pedido nº ${order.id}", pageWidth / 2, y, titlePaint)
        y += 40f

        // Client Info: Nome + fullname (bold, left)
        paint.textSize = 12f
        boldPaint.textSize = 12f
        boldPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        canvas.drawText("Nome: ", margin, y, paint)
        val nomeLabelWidth = paint.measureText("Nome: ")
        canvas.drawText(clientName ?: order.clientName ?: "N/A", margin + nomeLabelWidth, y, boldPaint)
        y += 20f

        // Next line: phone (left) and "Local" (right)
        val phone = order.phone ?: "N/A"
        val city = order.city ?: ""
        val state = order.state ?: ""
        val local = if (city.isNotEmpty() || state.isNotEmpty()) "$city - $state" else "N/A"
        canvas.drawText("Telefone: $phone", margin, y, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Local: $local", pageWidth - margin, y, paint)
        paint.textAlign = Paint.Align.LEFT // Reset for subsequent drawing
        y += 40f // Give a row space

        // Items Table Header
        boldPaint.textSize = 10f
        canvas.drawText("Qtd.", margin, y, boldPaint)
        canvas.drawText("Descrição", margin + 50f, y, boldPaint)
        
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Preço unit.", margin + 370f, y, boldPaint)
        canvas.drawText("Desc.", margin + 450f, y, boldPaint)
        canvas.drawText("Total", pageWidth - margin, y, boldPaint)
        boldPaint.textAlign = Paint.Align.LEFT // Reset
        
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 20f

        // Table Content
        paint.textSize = 10f
        order.items?.forEach { item ->
            if (y > 750f) {
                // In a real app we would start a new page here
            }

            val initialY = y
            val desc = item.description ?: ""
            val descWidth = 310f // Space between Desc column and Price column
            val lines = wrapText(desc, paint, descWidth)

            // Draw item data on the first line
            canvas.drawText(String.format(localeBR, "%.2f", item.quantity ?: 0.0), margin, y, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format(localeBR, "%.2f", item.price ?: 0.0), margin + 370f, y, paint)
            canvas.drawText(String.format(localeBR, "%.2f", item.discount ?: 0.0), margin + 450f, y, paint)
            canvas.drawText(String.format(localeBR, "%.2f", item.total ?: 0.0), pageWidth - margin, y, paint)
            paint.textAlign = Paint.Align.LEFT

            // Draw description lines
            lines.forEach { line ->
                canvas.drawText(line, margin + 50f, y, paint)
                y += 15f
            }

            val rowHeight = y - initialY
            if (rowHeight < 20f) {
                y = initialY + 20f
            } else {
                y += 5f
            }
        }

        y += 20f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 20f

        // Footer: Right aligned
        paint.textAlign = Paint.Align.RIGHT
        val rightMargin = pageWidth - margin
        canvas.drawText("Total ítens: ${String.format(localeBR, "%.2f", order.totalItems ?: 0.0)}", rightMargin, y, paint)
        y += 20f
        canvas.drawText("Desconto: ${String.format(localeBR, "%.2f", order.discount ?: 0.0)}", rightMargin, y, paint)
        y += 20f
        boldPaint.textAlign = Paint.Align.RIGHT
        boldPaint.textSize = 12f
        canvas.drawText("TOTAL: ${String.format(localeBR, "%.2f", order.total ?: 0.0)}", rightMargin, y, boldPaint)
        y += 30f

        val footerMessage = buildString {
            append(order.message ?: "")
            if (!order.message.isNullOrBlank() && !config?.defaultMessageOrder.isNullOrBlank()) {
                append("\n")
            }
            append(config?.defaultMessageOrder ?: "")
        }

        if (footerMessage.isNotBlank()) {
            paint.textAlign = Paint.Align.LEFT
            val lines = wrapText("Mensagem: $footerMessage", paint, pageWidth - 2 * margin)
            lines.forEach { line ->
                if (y > 820f) return@forEach
                canvas.drawText(line, margin, y, paint)
                y += 15f
            }
        }

        pdfDocument.finishPage(page)

        // Save to file
        val fileName = "pedido_${order.id}.pdf"
        val file = File(context.cacheDir, fileName)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        // Share the PDF
        if (viaWhatsapp) {
            shareViaWhatsapp(context, file, order.phone, isOrder = true)
        } else {
            shareFile(context, file, "Compartilhar Pedido")
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        
        for (paragraph in paragraphs) {
            val words = paragraph.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                val width = paint.measureText(testLine)
                if (width <= maxWidth) {
                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
                } else {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                        currentLine = StringBuilder(word)
                    } else {
                        result.add(word)
                    }
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            } else if (paragraphs.size > 1) {
                // Handle empty lines between paragraphs
                result.add("")
            }
        }
        return if (result.isEmpty()) listOf("") else result
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", localeBR)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", localeBR)
            val date = inputFormat.parse(dateStr.take(10))
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun decodeLogo(logo: String?): Bitmap? {
        if (logo.isNullOrBlank()) return null
        return try {
            val bytes = if (logo.startsWith("\\x")) {
                val hex = logo.substring(2).filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                val b = ByteArray(hex.length / 2)
                for (i in 0 until (hex.length / 2) * 2 step 2) {
                    b[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
                }
                b
            } else {
                android.util.Base64.decode(logo, android.util.Base64.DEFAULT)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
