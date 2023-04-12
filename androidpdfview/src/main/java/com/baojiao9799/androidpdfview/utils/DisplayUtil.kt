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
        fun getPdfPageWidthInPixels(
            context: Context,
            fitType: PdfFitType = PdfFitType.FILL_SCREEN_WIDTH
        ): Int {
            return context.resources.displayMetrics.widthPixels
        }

        fun getPdfPageHeightInPixels(context: Context): Int {
            return context.resources.displayMetrics.heightPixels
        }
    }
}