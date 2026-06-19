package com.example.kvantroium.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun copyTextToClipboard(context: Context, label: String, value: String, toastMessage: String = "Скопировано") {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        Toast.makeText(context, "Нечего копировать", Toast.LENGTH_SHORT).show()
        return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, trimmed))
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}
