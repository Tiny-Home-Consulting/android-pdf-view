package com.baojiao9799.androidpdfview.services

import android.content.Context
import com.baojiao9799.androidpdfview.utils.FileUtil
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import com.baojiao9799.androidpdfview.utils.FileUtil.Companion.copyTo

interface IPdfDownloadService {
    suspend fun downloadPdf(context: Context, url: String, fileName: String): File
}

class PdfDownloadService(): IPdfDownloadService {
    override suspend fun downloadPdf(context: Context, url: String, fileName: String): File {
        val file = FileUtil.createTempFile(context, fileName)

        val response = buildRetrofitService().downloadPdf(url)

        val inputStream = response.body()?.byteStream() ?: return file
        inputStream.use { input -> FileOutputStream(file).use {
            output -> input.copyTo(output)
        }}

        return file
    }

    private fun buildRetrofitService(): PdfRetrofitService {
        val retrofit = Retrofit.Builder()
            .build()
        return retrofit.create(PdfRetrofitService::class.java)
    }
}