package com.partyplanner.util

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import com.partyplanner.PartyPlannerApp

actual fun saveCsvToDevice(content: String, filename: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    val resolver = PartyPlannerApp.appContext.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, filename)
        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
    resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
    values.clear()
    values.put(MediaStore.Downloads.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
}
