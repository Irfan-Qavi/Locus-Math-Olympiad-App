package com.locus.olympiad.ui.math

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * JavaScript interface bridging HTML document scroll height calculation to Android layout parameters.
 */
class MathResizeBridge(private val onHeightCalculated: (Int) -> Unit) {
    @JavascriptInterface
    fun setHeight(height: Float) {
        // Run on UI thread to update the WebView layoutParams.height
        onHeightCalculated(height.toInt())
    }
}

/**
 * Custom WebView dedicated to rendering LaTeX mathematical equations using KaTeX and Computer Modern Serif (CMU Serif).
 * Dynamically adjusts height to prevent tall equations (fractions, summations, matrices) from clipping.
 */
@SuppressLint("SetJavaScriptEnabled")
class KatexWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var onHeightChangedListener: ((Int) -> Unit)? = null

    init {
        configureSettings()
        setupResizeBridge()
    }

    private fun configureSettings() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            defaultTextEncodingName = "UTF-8"
        }
        setBackgroundColor(Color.parseColor("#121212"))
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    private fun setupResizeBridge() {
        val bridge = MathResizeBridge { calculatedHeightDp ->
            post {
                val density = resources.displayMetrics.density
                val heightPx = (calculatedHeightDp * density).toInt()
                if (layoutParams != null) {
                    layoutParams.height = heightPx
                    requestLayout()
                } else {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
                }
                onHeightChangedListener?.invoke(heightPx)
            }
        }
        // Exactly "Android" identifier as invoked in the JS wrapper: Android.setHeight(height)
        addJavascriptInterface(bridge, "Android")
    }

    fun setOnHeightChangedListener(listener: (Int) -> Unit) {
        this.onHeightChangedListener = listener
    }

    /**
     * Renders LaTeX content using the exact KaTeX and CMU Serif configuration wrapper.
     */
    fun setLatexContent(latexContent: String) {
        val wrappedHtml = HTML_TEMPLATE.replace("{LATEX_CONTENT_PLACEHOLDER}", latexContent)
        loadDataWithBaseURL(
            "file:///android_asset/",
            wrappedHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    companion object {
        private const val HTML_TEMPLATE = """<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
    <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
    <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/dreampulse/computer-modern-web-font@master/fonts.css">
    <style>
        body {
            background-color: #121212; color: #FFFFFF;
            font-family: "Computer Modern Serif", serif !important;
            font-size: 18px; line-height: 1.6; margin: 0; padding: 16px;
            overflow-y: hidden;
        }
        .katex { font-family: "Computer Modern Serif", serif !important; }
        #content { padding-top: 16px; padding-bottom: 24px; }
    </style>
</head>
<body>
    <div id="content">{LATEX_CONTENT_PLACEHOLDER}</div>
    <script>
        document.addEventListener("DOMContentLoaded", function() {
            renderMathInElement(document.body, {
                delimiters: [{left: "$$", right: "$$", display: true}, {left: "\[", right: "\]", display: true}, {left: "$", right: "$", display: false}, {left: "\(", right: "\)", display: false}],
                throwOnError: false
            });
            setTimeout(function() {
                var height = document.documentElement.scrollHeight || document.body.scrollHeight;
                Android.setHeight(height); 
            }, 100); 
        });
    </script>
</body>
</html>"""
    }
}
