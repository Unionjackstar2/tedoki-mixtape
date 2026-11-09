package de.tedoki.mixtape.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import de.tedoki.mixtape.model.Cassette
import java.io.File
import java.util.Locale

data class AudioFileInfo(
    val file: File?,
    val fileName: String,
    val absolutePath: String,
    val exists: Boolean,
    val canRead: Boolean,
    val sizeBytes: Long,
    val formattedSize: String,
    val statusText: String,
    val isValidAudioFile: Boolean,
    val mimeType: String,
    val isInternalStorage: Boolean
)

object AudioFileUtils {

    private const val TAG = "AudioFileUtils"

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 Bytes"
        val locale = Locale.GERMAN
        return if (bytes < 1000 * 1000) {
            val kbDecimal = bytes / 1000.0
            val kibBinary = bytes / 1024.0
            String.format(locale, "%.1f KB (%.1f KiB, %,d Bytes)", kbDecimal, kibBinary, bytes)
        } else {
            val mbDecimal = bytes / (1000.0 * 1000.0)
            val mibBinary = bytes / (1024.0 * 1024.0)
            String.format(locale, "%.2f MB (%.2f MiB, %,d Bytes)", mbDecimal, mibBinary, bytes)
        }
    }

    fun determineMimeType(file: File?): String {
        if (file == null) return "audio/mpeg"
        val extension = file.extension.lowercase(Locale.ROOT)
        return when (extension) {
            "mp3" -> "audio/mpeg"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            else -> {
                val mimeFromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (!mimeFromMap.isNullOrBlank() && mimeFromMap.startsWith("audio/")) {
                    mimeFromMap
                } else {
                    "audio/mpeg"
                }
            }
        }
    }

    fun inspectAudioFile(context: Context, cassette: Cassette): AudioFileInfo {
        val resolvedFile = resolveAudioFile(context, cassette)
        val filesDir = context.filesDir

        if (resolvedFile == null) {
            val targetName = if (cassette.filePath.isNotBlank()) {
                File(cassette.filePath).name
            } else {
                "${cassette.id}.mp3"
            }
            val expectedPath = if (cassette.filePath.isNotBlank()) {
                cassette.filePath
            } else {
                File(File(filesDir, "recordings"), targetName).absolutePath
            }
            return AudioFileInfo(
                file = null,
                fileName = targetName,
                absolutePath = expectedPath,
                exists = false,
                canRead = false,
                sizeBytes = 0L,
                formattedSize = "0 Bytes",
                statusText = "Datei fehlt oder wurde noch nicht aufgenommen",
                isValidAudioFile = false,
                mimeType = "audio/mpeg",
                isInternalStorage = expectedPath.startsWith(filesDir.absolutePath)
            )
        }

        val exists = resolvedFile.exists()
        val canRead = exists && resolvedFile.canRead()
        val sizeBytes = if (exists) resolvedFile.length() else 0L
        val formatted = formatFileSize(sizeBytes)
        val mimeType = determineMimeType(resolvedFile)
        val isInternal = resolvedFile.absolutePath.startsWith(filesDir.absolutePath)

        val statusText = when {
            !exists -> "Datei nicht gefunden (Pfad existiert nicht)"
            !canRead -> "Datei existiert, aber keine Leseberechtigung"
            sizeBytes == 0L -> "Datei existiert, ist jedoch leer (0 Bytes)"
            else -> "Vorhanden & lesbar"
        }

        val isValid = exists && canRead && sizeBytes > 0L

        return AudioFileInfo(
            file = resolvedFile,
            fileName = resolvedFile.name,
            absolutePath = resolvedFile.absolutePath,
            exists = exists,
            canRead = canRead,
            sizeBytes = sizeBytes,
            formattedSize = formatted,
            statusText = statusText,
            isValidAudioFile = isValid,
            mimeType = mimeType,
            isInternalStorage = isInternal
        )
    }

    fun resolveAudioFile(context: Context, cassette: Cassette): File? {
        if (cassette.filePath.isNotBlank()) {
            val direct = File(cassette.filePath)
            if (direct.exists()) return direct
        }
        val defaultRecording = File(File(context.filesDir, "recordings"), "${cassette.id}.mp3")
        if (defaultRecording.exists()) {
            return defaultRecording
        }
        if (cassette.filePath.isNotBlank()) {
            return File(cassette.filePath)
        }
        return defaultRecording
    }

    sealed class OpenResult {
        object Success : OpenResult()
        data class Error(val message: String, val throwable: Throwable? = null) : OpenResult()
    }

    fun openInExternalPlayer(context: Context, cassette: Cassette): OpenResult {
        val info = inspectAudioFile(context, cassette)
        val file = info.file

        if (file == null || !info.exists) {
            return OpenResult.Error("Die Audiodatei für \"${cassette.title}\" wurde auf dem Speicher nicht gefunden.")
        }
        if (!info.canRead) {
            return OpenResult.Error("Keine Leseberechtigung für die Datei \"${info.fileName}\".")
        }
        if (info.sizeBytes <= 0L) {
            return OpenResult.Error("Die Audiodatei ist leer (0 Bytes) und kann nicht wiedergegeben werden.")
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, info.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "Audiodatei öffnen mit…").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            OpenResult.Success
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity found to handle audio intent", e)
            OpenResult.Error("Kein geeigneter externer Audioplayer auf diesem Gerät installiert.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening external player for ${file.absolutePath}", e)
            OpenResult.Error("Fehler beim Öffnen des externen Players: ${e.localizedMessage ?: "Unbekannter Fehler"}", e)
        }
    }
}
