package com.baojiao9799.androidpdfview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.baojiao9799.androidpdfview.databinding.PdfPageBinding

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
}