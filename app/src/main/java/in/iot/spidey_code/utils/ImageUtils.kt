package `in`.iot.spidey_code.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

fun saveImageToGallery(context: Context, uriString: String?) {
    if (uriString == null) return

    try {
        val sourceUri = Uri.parse(uriString)
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "SpideyCode_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SpideyCode")
        }

        val destinationUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (destinationUri != null) {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(destinationUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
    }
}

fun shareImage(context: Context, uriString: String?) {
    if (uriString == null) return

    try {
        val parsedUri = Uri.parse(uriString)

        val shareUri = if (parsedUri.scheme == "file") {
            val file = File(parsedUri.path!!)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } else {
            parsedUri
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share your photo via"))

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun saveVideoToGallery(context: Context, uriString: String?) {
    if (uriString == null) return

    try {
        val sourceUri = Uri.parse(uriString)
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "SpideyCode_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SpideyCode")
        }

        val destinationUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (destinationUri != null) {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(destinationUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save video", Toast.LENGTH_SHORT).show()
    }
}

fun shareVideo(context: Context, uriString: String?) {
    if (uriString == null) return

    try {
        val parsedUri = Uri.parse(uriString)

        val shareUri = if (parsedUri.scheme == "file") {
            val file = File(parsedUri.path!!)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } else {
            parsedUri
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share your video via"))

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}