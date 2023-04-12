package com.baojiao9799.androidpdfview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.baojiao9799.androidpdfview.databinding.PdfPageBinding
import com.baojiao9799.androidpdfview.utils.DisplayUtil
import com.baojiao9799.androidpdfview.utils.FileUtil

class AndroidPdfView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: PdfPageBinding

    init {
        binding = PdfPageBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun loadPdfFromAssets(assetFileName: String) {
        val file = FileUtil.createFileFromAsset(
            context,
            "sample.pdf",
            assetFileName
        )
        val pdf = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pdf)
        val bitmap = Bitmap.createBitmap(
            DisplayUtil.getPdfPageWidthInPixels(context),
            DisplayUtil.getPdfPageHeightInPixels(context),
            Bitmap.Config.ARGB_8888
        )
        val page = renderer.openPage(0)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        binding.pdfPageContainer.setImageBitmap(bitmap)
    }
}