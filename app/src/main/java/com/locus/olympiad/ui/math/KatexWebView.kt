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
        onHeightCalculated(height.toInt())
    }
}

/**
 * Custom WebView dedicated to rendering LaTeX mathematical equations using KaTeX and Computer Modern Serif (CMU Serif).
 * Dynamically adjusts height using document.fonts.ready to prevent tall equations (fractions, summations, matrices) from clipping.
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
        addJavascriptInterface(bridge, "Android")
    }

    fun setOnHeightChangedListener(listener: (Int) -> Unit) {
        this.onHeightChangedListener = listener
    }

    /**
     * Sanitizes raw LaTeX string to ensure HTML special characters (<, >, &)
     * are properly escaped before KaTeX auto-rendering, preventing broken DOM trees.
     */
    private fun sanitizeLatexForHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    /**
     * Renders LaTeX content using the KaTeX and CMU Serif configuration wrapper.
     */
    fun setLatexContent(latexContent: String) {
        val sanitized = sanitizeLatexForHtml(latexContent)
        val wrappedHtml = HTML_TEMPLATE.replace("{LATEX_CONTENT_PLACEHOLDER}", sanitized)
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
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
    <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
    <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/dreampulse/computer-modern-web-font@master/fonts.css">
    <style>
        * { box-sizing: border-box; }
        html, body {
            background-color: #121212;
            color: #FFFFFF;
            font-family: "Computer Modern Serif", serif !important;
            font-size: 17px;
            line-height: 1.6;
            margin: 0;
            padding: 12px 14px 28px 14px;
            overflow-y: hidden;
            overflow-x: auto;
        }
        .katex {
            font-family: "Computer Modern Serif", serif !important;
        }
        #content {
            white-space: pre-wrap;
            word-wrap: break-word;
            padding-bottom: 24px;
        }
        .katex-display {
            overflow-x: auto;
            overflow-y: hidden;
            padding: 6px 0;
            margin: 10px 0;
        }
    </style>
</head>
<body>
    <div id="content">{LATEX_CONTENT_PLACEHOLDER}</div>
    <script>
        function sendHeight() {
            var content = document.getElementById("content");
            var height = Math.max(
                document.documentElement.scrollHeight || 0,
                document.body.scrollHeight || 0,
                content ? content.offsetHeight + 40 : 0
            );
            if (window.Android && window.Android.setHeight) {
                window.Android.setHeight(height);
            }
        }

        document.addEventListener("DOMContentLoaded", function() {
            renderMathInElement(document.body, {
                delimiters: [
                    {left: "$$", right: "$$", display: true},
                    {left: "\[", right: "\]", display: true},
                    {left: "$", right: "$", display: false},
                    {left: "\(", right: "\)", display: false}
                ],
                throwOnError: false
            });

            sendHeight();

            if (document.fonts && document.fonts.ready) {
                document.fonts.ready.then(function() {
                    sendHeight();
                    setTimeout(sendHeight, 150);
                });
            } else {
                setTimeout(sendHeight, 300);
            }
        });

        window.addEventListener("load", function() {
            sendHeight();
        });
    </script>
</body>
</html>"""
    }
}
