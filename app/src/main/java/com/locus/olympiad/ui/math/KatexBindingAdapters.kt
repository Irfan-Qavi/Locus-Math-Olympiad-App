package com.locus.olympiad.ui.math

import androidx.databinding.BindingAdapter

object KatexBindingAdapters {

    @JvmStatic
    @BindingAdapter("latexContent")
    fun setLatexContent(view: KatexWebView, content: String?) {
        if (!content.isNullOrBlank()) {
            view.setLatexContent(content)
        }
    }
}
