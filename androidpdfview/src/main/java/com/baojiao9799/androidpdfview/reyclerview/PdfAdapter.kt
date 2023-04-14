package com.baojiao9799.androidpdfview.reyclerview

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.baojiao9799.androidpdfview.databinding.PdfPageBinding

class PdfAdapter(
    private val pdfPages: List<Bitmap>
): RecyclerView.Adapter<PdfAdapter.PdfPageHolder>() {
    class PdfPageHolder(
        private val binding: PdfPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindPdfPage(pdfPage: Bitmap) {
            binding.pdfPageContainer.setImageBitmap(pdfPage)
        }
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): PdfPageHolder {
        val binding = PdfPageBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup,
            false
        )

        return PdfPageHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: PdfPageHolder, position: Int) {
        viewHolder.bindPdfPage(
            pdfPages[position]
        )
    }

    override fun getItemCount() = pdfPages.size
}