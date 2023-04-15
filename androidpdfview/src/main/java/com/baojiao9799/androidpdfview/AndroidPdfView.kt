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
import com.baojiao9799.androidpdfview.reyclerview.PageSpacer
import com.baojiao9799.androidpdfview.reyclerview.PdfAdapter
import com.baojiao9799.androidpdfview.utils.DisplayUtil
import com.baojiao9799.androidpdfview.utils.FileUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

class AndroidPdfView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: AndroidPdfViewBinding

    private var paddingHorizontal = 0
    private var paddingVertical = 0
    private var pageSpacing = 0
    private var showScrollBar = true

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AndroidPdfView,
            defStyleAttr,
            0
        ).apply{
            try {
                paddingHorizontal = getDimensionPixelSize(
                    R.styleable.AndroidPdfView_paddingHorizontal,
                    0
                )
                paddingVertical = getDimensionPixelSize(
                    R.styleable.AndroidPdfView_paddingVertical,
                    0
                )
                pageSpacing = getDimensionPixelSize(
                    R.styleable.AndroidPdfView_pageSpacing,
                    0
                )
                showScrollBar = getBoolean(
                    R.styleable.AndroidPdfView_showScrollbar,
                    true
                )
            } finally {
                recycle()
            }
        }

        binding = AndroidPdfViewBinding.inflate(LayoutInflater.from(context), this, true)

        binding.pdfPages.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        binding.pdfPages.setPadding(
            paddingHorizontal,
            paddingVertical,
            paddingHorizontal,
            paddingVertical
        )

        if (pageSpacing != 0) {
            binding.pdfPages.addItemDecoration(PageSpacer(pageSpacing))
        }

        if (!showScrollBar) {
            binding.pdfPages.scrollBarSize = 0
        }
    }

    fun loadPdfFromFile(file: File) {
        val pdfPageList = getPdfPageList(file)

        binding.pdfPages.adapter = PdfAdapter(pdfPageList)
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
            val pdfDimensions = DisplayUtil.getPdfDimensions(
                context,
                page.width,
                page.height
            )
            val bitmap = Bitmap.createBitmap(
                pdfDimensions.widthInPixels,
                pdfDimensions.heightInPixels,
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