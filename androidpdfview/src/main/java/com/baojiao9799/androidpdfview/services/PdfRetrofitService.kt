package com.baojiao9799.androidpdfview.services

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface PdfRetrofitService {
    @Streaming
    @GET
    suspend fun downloadPdf(@Url url: String): Response<ResponseBody>
}