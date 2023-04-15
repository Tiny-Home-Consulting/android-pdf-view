package com.baojiao9799.androidpdfview.utils

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import java.io.*

class FileUtil {
    companion object {
        private class DownloadException(
            message: String,
            cause: Throwable? = null
        ) : RuntimeException(message, cause)

        fun createFileFromAsset(context: Context, targetFileName: String, assetFileName: String): File? {
            val assetManager: AssetManager = context.assets
            return try {
                val file = this.createTempFile(context, targetFileName)
                val inputStream = assetManager.open(assetFileName)
                inputStream.use { input -> FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }}

                file
            } catch (e: Exception) {
                Log.e("FileUtil", "$e")
                null
            }
        }

        fun createTempFile(context: Context, fileName: String): File = try {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            File.createTempFile("download_", fileName, directory)
        } catch (e: IOException) {
            throw DownloadException("Error while creating download file", e)
        }

        fun InputStream.copyTo(output: FileOutputStream) {
            val bufferSize = 4 * 1024
            val buffer = ByteArray(bufferSize)

            var byteCount = read(buffer)

            while (byteCount >= 0) {
                output.write(buffer, 0, byteCount)
                byteCount = read(buffer)
            }

            output.flush()
        }
    }
}