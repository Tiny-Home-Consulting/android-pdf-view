package com.baojiao9799.androidpdfview.activities

import android.app.Activity
import android.os.Bundle
import com.baojiao9799.androidpdfview.databinding.PdfActivityBinding

class PdfActivity : Activity() {
    private lateinit var binding: PdfActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PdfActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.samplePdfView.loadPdfFromAssets("single-page-pdf.pdf")
    }
}