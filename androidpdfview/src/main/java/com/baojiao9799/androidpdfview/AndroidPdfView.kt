package com.baojiao9799.androidpdfview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.ScaleGestureDetector
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewCompat
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
    // The current viewport. This rectangle represents the currently visible
    // chart domain and range.
    private val mCurrentViewport = RectF(
        0f,
        0f,
        context.resources.displayMetrics.widthPixels.toFloat(),
        context.resources.displayMetrics.heightPixels.toFloat()
    )

    // The current destination rectangle (in pixel coordinates) into which the
    // chart data should be drawn.
    private val mContentRect: Rect? = null


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
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Scrolling uses math based on the viewport (as opposed to math using pixels).

            mContentRect?.apply {
                // Pixel offset is the offset in screen pixels, while viewport offset is the
                // offset within the current viewport.
                val viewportOffsetX = distanceX * mCurrentViewport.width() / width()
                val viewportOffsetY = -distanceY * mCurrentViewport.height() / height()


                // Updates the viewport, refreshes the display.
                setViewportBottomLeft(
                    mCurrentViewport.left + viewportOffsetX,
                    mCurrentViewport.bottom + viewportOffsetY
                )
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

    /**
     * Sets the current viewport (defined by mCurrentViewport) to the given
     * X and Y positions. Note that the Y value represents the topmost pixel position,
     * and thus the bottom of the mCurrentViewport rectangle.
     */
    private fun setViewportBottomLeft(x: Float, y: Float) {
        /*
         * Constrains within the scroll range. The scroll range is simply the viewport
         * extremes (AXIS_X_MAX, etc.) minus the viewport size. For example, if the
         * extremes were 0 and 10, and the viewport size was 2, the scroll range would
         * be 0 to 8.
         */

        val curWidth: Float = mCurrentViewport.width()
        val curHeight: Float = mCurrentViewport.height()
        val newX: Float = Math.max(0f, Math.min(x, context.resources.displayMetrics.widthPixels - curWidth))
        val newY: Float = Math.max(0f + curHeight, Math.min(y, context.resources.displayMetrics.heightPixels.toFloat()))

        mCurrentViewport.set(newX, newY - curHeight, newX + curWidth, newY)

        // Invalidates the View to update the display.
        ViewCompat.postInvalidateOnAnimation(this)
    }
}