package com.baojiao9799.androidpdfview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.baojiao9799.androidpdfview.databinding.AndroidPdfViewBinding
import com.baojiao9799.androidpdfview.reyclerview.PageSpacer
import com.baojiao9799.androidpdfview.reyclerview.PdfAdapter
import com.baojiao9799.androidpdfview.utils.DisplayUtil
import com.baojiao9799.androidpdfview.utils.FileUtil
import java.io.File

@SuppressLint("ClickableViewAccessibility")
class AndroidPdfView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: AndroidPdfViewBinding

    // Custom attributes
    private var paddingHorizontal = 0
    private var paddingVertical = 0
    private var pageSpacing = 0
    private var showScrollBar = true
    private var supportPinchZoom = true
    private var supportTapZoom = true

    private val tapZoomMinScale = 1f
    private val tapZoomMaxScale = 2f

    private var currentScaleFactor = 1f

    private var mTranslationX = 0f
    private var mTranslationY = 0f
    private var currentLeftX = 0f
    private var currentRightX = context.resources.displayMetrics.widthPixels.toFloat()
    private var currentTopY = 0f
    private var currentBottomY = context.resources.displayMetrics.widthPixels.toFloat()
    private val maxX get() = context.resources.displayMetrics.widthPixels.toFloat() * currentScaleFactor

    private val pinchZoomListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentScaleFactor *= detector.scaleFactor

            // Don't let the object get too small or too large.
            currentScaleFactor = 1f.coerceAtLeast(
                currentScaleFactor.coerceAtMost(5.0f)
            )

            invalidate()
            return true
        }
    }

    private val doubleTapListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDoubleTap(event: MotionEvent): Boolean {
            currentScaleFactor = when {
                currentScaleFactor >= tapZoomMaxScale -> {
                    tapZoomMinScale
                }
                else -> {
                    tapZoomMaxScale
                }
            }
            invalidate()
            return true
        }

        override fun onScroll(
            downEvent: MotionEvent,
            moveEvent: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            mTranslationX = moveEvent.x - downEvent.x
            mTranslationY = moveEvent.y - downEvent.y

            currentLeftX -= mTranslationX
            currentTopY -= mTranslationY
            currentRightX -= mTranslationX

            if (currentLeftX < 0) {
                mTranslationX += currentLeftX
                currentLeftX = 0f
                currentRightX = maxX
            }

            if (currentTopY < 0) {
                mTranslationY += currentTopY
                currentTopY = 0f
            }

            if (currentRightX > maxX) {
                mTranslationX += (currentRightX - maxX)
                currentLeftX = 0f
                currentRightX = maxX
            }

            return true
        }
    }

    private val pinchZoomDetector = ScaleGestureDetector(context, pinchZoomListener)
    private val doubleTapDetector = GestureDetector(context, doubleTapListener)

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
                supportPinchZoom = getBoolean(
                    R.styleable.AndroidPdfView_supportPinchZoom,
                    true
                )
                supportTapZoom = getBoolean(
                    R.styleable.AndroidPdfView_supportTapZoom,
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

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (supportPinchZoom) {
            pinchZoomDetector.onTouchEvent(event)
        }

        if (supportTapZoom) {
            doubleTapDetector.onTouchEvent(event)
        }

        return super.dispatchTouchEvent(event)
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.scale(currentScaleFactor, currentScaleFactor)
        canvas.translate(mTranslationX, mTranslationY)
        super.dispatchDraw(canvas)
        canvas.restore()
        invalidate()
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