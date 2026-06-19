package com.example.kvantroium.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.example.kvantroium.features.events.EventDocPreviewKind
import com.example.kvantroium.features.events.eventDocPreviewKind
import java.io.File

object EventDocumentFiles {
    fun readBytes(context: Context, uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Не удалось прочитать файл")
    }

    fun guessFileName(context: Context, uri: Uri, fallback: String = "document"): String {
        val fromProvider = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
        return fromProvider?.takeIf { it.isNotBlank() } ?: fallback
    }

    fun guessMimeType(context: Context, uri: Uri, fileName: String): String {
        return context.contentResolver.getType(uri)
            ?: when {
                fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "application/octet-stream"
            }
    }

    fun decodePreviewBitmap(bytes: ByteArray, mimeType: String, fileName: String): Bitmap? {
        return when (eventDocPreviewKind(mimeType, fileName)) {
            EventDocPreviewKind.IMAGE -> android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            EventDocPreviewKind.PDF -> renderPdfPreview(bytes)
            EventDocPreviewKind.OTHER -> null
        }
    }

    private fun renderPdfPreview(bytes: ByteArray): Bitmap? {
        return runCatching {
            val file = File.createTempFile("preview", ".pdf")
            try {
                file.writeBytes(bytes)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        if (renderer.pageCount <= 0) return@runCatching null
                        renderer.openPage(0).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width,
                                page.height,
                                Bitmap.Config.ARGB_8888
                            )
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            } finally {
                file.delete()
            }
        }.getOrNull()
    }

    fun saveToCache(context: Context, bytes: ByteArray, fileName: String): File {
        val dir = File(context.cacheDir, "event_docs").apply { mkdirs() }
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_")
        val target = File(dir, safeName)
        if (target.exists()) target.delete()
        target.writeBytes(bytes)
        return target
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Открыть файл"))
    }
}
