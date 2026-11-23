package com.purplehub.app

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.zip.ZipFile

/** Local-only extraction. Source files are copied to cache temporarily and deleted after analysis. */
internal object LocalAttachmentAnalyzer {
    private const val MAX_FILE_BYTES = 10L * 1024L * 1024L
    private const val MAX_TEXT_CHARS = 7_000
    private const val MAX_PDF_PAGES = 4
    private const val MAX_VISION_IMAGE_BYTES = 1_200_000

    suspend fun analyzeUri(context: Context, uri: Uri): ChatAttachment = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = queryName(context, uri) ?: "attachment"
        val mime = resolver.getType(uri) ?: guessMime(displayName)
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val temp = File.createTempFile("purplehub_attach_", if (extension.isBlank()) ".bin" else ".${extension.take(8)}", context.cacheDir)
        try {
            resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_FILE_BYTES) { "Файл больше 10 МБ. Выбери файл поменьше." }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: throw IllegalArgumentException("Не удалось открыть выбранный файл")
            analyzeFile(context, temp, displayName, mime)
        } finally {
            temp.delete()
        }
    }

    suspend fun analyzeCapturedFile(context: Context, file: File): ChatAttachment = withContext(Dispatchers.IO) {
        require(file.exists() && file.length() > 0) { "Камера не сохранила снимок." }
        require(file.length() <= MAX_FILE_BYTES) { "Снимок получился слишком большим." }
        analyzeFile(context, file, "Снимок_${System.currentTimeMillis()}.jpg", "image/jpeg")
    }

    private suspend fun analyzeFile(context: Context, file: File, name: String, mime: String): ChatAttachment {
        val extension = name.substringAfterLast('.', "").lowercase()
        var visionDataUrl: String? = null
        val analysis = when {
            mime.startsWith("image/") || extension in setOf("jpg", "jpeg", "png", "webp", "bmp", "heic") -> {
                val imageResult = analyzeImage(context, file)
                visionDataUrl = imageResult.second
                imageResult.first
            }
            extension == "pdf" || mime == "application/pdf" -> analyzePdf(context, file)
            extension == "docx" -> analyzeDocx(file)
            extension in setOf("txt", "md", "csv", "json", "xml", "html", "log") || mime.startsWith("text/") -> {
                val raw = file.readText(Charsets.UTF_8).replace("\u0000", "")
                "Текст извлечён локально:\n" + raw.take(MAX_TEXT_CHARS) + if (raw.length > MAX_TEXT_CHARS) "\n\n[Файл обрезан для контекста FAIRY.]" else ""
            }
            else -> "Файл добавлен, но формат .$extension пока не поддерживается для локального чтения. Поддерживаются изображения, PDF, DOCX, TXT, MD, CSV, JSON, XML и HTML."
        }
        return ChatAttachment(
            name = name.take(120), mimeType = mime.take(120),
            analysis = analysis.take(MAX_TEXT_CHARS), imageDataUrl = visionDataUrl
        )
    }

    private suspend fun analyzeImage(context: Context, file: File): Pair<String, String> {
        // Downsample large camera photos before OCR/labeling to avoid high RAM use on older phones.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Не удалось прочитать изображение." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: throw IllegalArgumentException("Формат изображения не поддерживается.")
        val oriented = try {
            val rotation = when (ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotation == 0f) decoded else Bitmap.createBitmap(
                decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true
            )
        } catch (_: Exception) { decoded }
        val input = InputImage.fromBitmap(oriented, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build())
        return try {
            val textTask = recognizer.process(input)
            val labelsTask = labeler.process(input)
            val detectedText = textTask.await().text.trim()
            val labels = labelsTask.await().take(6).joinToString(", ") { it.text }
            val analysis = buildString {
                append("Изображение обработано на устройстве.\n")
                if (labels.isNotBlank()) append("Предварительные метки объектов: ").append(labels).append(".\n")
                if (detectedText.isNotBlank()) append("Текст на изображении (OCR):\n").append(detectedText)
                if (labels.isBlank() && detectedText.isBlank()) append("Локально распознать объекты и текст не удалось.")
                append("\n\nПримечание: локальная разметка и OCR — вспомогательные функции. Для смыслового анализа самого изображения FAIRY Vision отправит сжатую копию только после отдельного подтверждения.")
                append(" OCR ориентирован на латинское письмо; кириллица на изображениях может распознаваться неточно.")
            }
            analysis to encodeVisionImage(oriented)
        } finally {
            recognizer.close()
            labeler.close()
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
    }

    /** Creates a small JPEG data URL for a vision-capable chat model. The bytes live only in memory. */
    private fun encodeVisionImage(source: Bitmap): String {
        val scale = minOf(1f, 1280f / maxOf(source.width, source.height).toFloat())
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
            source, (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1), true
        ) else source
        val rgb = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.RGB_565)
        try {
            Canvas(rgb).apply {
                drawColor(Color.WHITE)
                drawBitmap(scaled, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
            }
            var quality = 84
            var bytes: ByteArray
            do {
                val output = ByteArrayOutputStream()
                check(rgb.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "Не удалось сжать фото для FAIRY Vision." }
                bytes = output.toByteArray()
                if (bytes.size <= MAX_VISION_IMAGE_BYTES) break
                quality -= 10
            } while (quality >= 54)
            require(bytes.size <= MAX_VISION_IMAGE_BYTES) {
                "Фото слишком сложное для безопасной отправки. Выбери изображение поменьше."
            }
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            rgb.recycle()
            if (scaled !== source) scaled.recycle()
        }
    }

    private suspend fun analyzePdf(context: Context, file: File): String {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val pageCount = renderer.pageCount
            val extracted = StringBuilder()
            val limit = minOf(pageCount, MAX_PDF_PAGES)
            for (index in 0 until limit) {
                val page = renderer.openPage(index)
                try {
                    val scale = minOf(1.5f, 1200f / page.width.toFloat(), 1600f / page.height.toFloat()).coerceAtLeast(0.5f)
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
                        if (text.isNotBlank()) extracted.append("Страница ${index + 1}:\n").append(text).append("\n\n")
                    } finally { bitmap.recycle() }
                } finally { page.close() }
                if (extracted.length >= MAX_TEXT_CHARS) break
            }
            buildString {
                append("PDF обработан локально: страниц в документе — $pageCount, проверено — $limit.\n")
                if (extracted.isNotBlank()) append(extracted.toString().take(MAX_TEXT_CHARS))
                else append("Текст на страницах не распознан. Возможно, PDF содержит мелкий текст или скан низкого качества.")
                if (pageCount > MAX_PDF_PAGES) append("\n\nДля производительности обработаны только первые $MAX_PDF_PAGES страницы.")
                append("\nOCR ориентирован на латинское письмо; кириллица на сканах может распознаваться неточно.")
            }
        } finally {
            recognizer.close()
            renderer.close()
        }
    }

    private fun analyzeDocx(file: File): String {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml") ?: throw IllegalArgumentException("В DOCX не найден текст документа")
            val xmlBytes = zip.getInputStream(entry).use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0 || total >= 1_500_000) break
                    val safeCount = minOf(count, 1_500_000 - total)
                    output.write(buffer, 0, safeCount)
                    total += safeCount
                }
                output.toByteArray()
            }
            val xml = String(xmlBytes, Charsets.UTF_8)
            val text = Regex("<w:t(?:\\s+[^>]*)?>(.*?)</w:t>", setOf(RegexOption.DOT_MATCHES_ALL))
                .findAll(xml).joinToString(" ") { decodeXml(it.groupValues[1]) }
                .replace(Regex("\\s+"), " ").trim()
            if (text.isBlank()) return "DOCX открыт локально, но извлечь текст не удалось."
            return "Текст DOCX извлечён локально:\n" + text.take(MAX_TEXT_CHARS) + if (text.length > MAX_TEXT_CHARS) "\n\n[Документ обрезан для контекста FAIRY.]" else ""
        }
    }

    private fun decodeXml(value: String): String = value
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'")

    private fun queryName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) { null }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "txt", "md", "csv", "json", "xml", "html", "log" -> "text/plain"
        else -> "application/octet-stream"
    }
}
