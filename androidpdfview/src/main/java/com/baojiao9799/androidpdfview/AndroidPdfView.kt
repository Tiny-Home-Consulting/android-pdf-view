package com.baojiao9799.androidpdfview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.util.Log
import android.view.*
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

    private val windowManager = context.getSystemService(WindowManager::class.java) as WindowManager

    private val screenWidth get() = context.resources.displayMetrics.widthPixels.toFloat()
    private val screenHeight get() = context.resources.displayMetrics.heightPixels.toFloat()

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
    private var currentLeftX = 0f
    private var currentRightX = screenWidth
    private val minX = 0f
    private val maxX get() = screenWidth * currentScaleFactor

    private var previousViewport = RectF(minX, 0f, screenWidth, screenHeight)
    private val mCurrentViewport = RectF(minX, 0f, screenWidth, screenHeight)

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
            Log.d("translationX", mTranslationX.toString())
            Log.d("moveEventX", moveEvent.x.toString())
            Log.d("downEventX", downEvent.x.toString())
            // Pixel offset is the offset in screen pixels, while viewport offset is the
            // offset within the current viewport.
            val viewportOffsetX = distanceX * mCurrentViewport.width() / screenWidth
            val viewportOffsetY = -distanceY * mCurrentViewport.height() / screenHeight

            // Updates the viewport, refreshes the display.
            setViewportBottomLeft(
                mCurrentViewport.left + distanceX,
                mCurrentViewport.bottom + viewportOffsetY
            )

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

    private var initialTouchX = 0f
    private var initialTouchY = 0f

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
        val matrix = Matrix()
        matrix.postTranslate(mTranslationX, 0f)
        val rect = RectF(0f, 0f, screenWidth, screenHeight)
        matrix.mapRect(mCurrentViewport)
        canvas.concat(matrix)
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
        val newX: Float = Math.max(0f, Math.min(x, maxX - curWidth))
        val newY: Float = Math.max(0f + curHeight, Math.min(y, screenHeight))

        previousViewport = RectF(mCurrentViewport)

        mCurrentViewport.set(x, newY - curHeight, x + curWidth, newY)

        // Invalidates the View to update the display.
        //ViewCompat.postInvalidateOnAnimation(this)
    }
}