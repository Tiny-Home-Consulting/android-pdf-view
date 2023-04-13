package com.baojiao9799.androidpdfview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.baojiao9799.androidpdfview.databinding.AndroidPdfViewBinding
import com.baojiao9799.androidpdfview.reycleradapters.PdfAdapter
import com.baojiao9799.androidpdfview.utils.FileUtil
import java.io.File

class AndroidPdfView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: AndroidPdfViewBinding

    init {
        binding = AndroidPdfViewBinding.inflate(LayoutInflater.from(context), this, true)

        binding.pdfPages.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
    }

    fun loadPdfFromAssets(assetFileName: String) {
        val file = FileUtil.createFileFromAsset(
            context,
            "sample.pdf",
            assetFileName
        ) ?: return

        val pdfPageList = getPdfPageList(file)

        binding.pdfPages.adapter = PdfAdapter(pdfPageList)
    }

    private fun getPdfPageList(file: File): List<Bitmap> {
        val pdfPages: MutableList<Bitmap> = arrayListOf()

        val pdf = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pdf)
        val pageCount = renderer.pageCount

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(
                page.width,
                page.height,
                Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfPages.add(bitmap)
            page.close()
        }

        renderer.close()

        return pdfPages
    }
}