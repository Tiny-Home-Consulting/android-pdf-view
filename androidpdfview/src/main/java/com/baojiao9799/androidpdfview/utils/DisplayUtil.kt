package com.baojiao9799.androidpdfview.utils

import android.content.Context

data class PdfDimensions(
    val widthInPixels: Int,
    val heightInPixels: Int
)

enum class PdfFitType {
    FILL_SCREEN_WIDTH
}

class DisplayUtil {
    companion object {
        fun getPdfDimensions(
            context: Context,
            pageWidthInPixels: Int,
            pageHeightInPixels: Int,
            pdfFitType: PdfFitType = PdfFitType.FILL_SCREEN_WIDTH
        ): PdfDimensions {
            val screenWidth = context.resources.displayMetrics.widthPixels
            val scaledHeight = screenWidth.toFloat() * pageHeightInPixels.toFloat() / pageWidthInPixels.toFloat()

            return PdfDimensions(
                screenWidth,
                scaledHeight.toInt()
            )
        }
    }
}