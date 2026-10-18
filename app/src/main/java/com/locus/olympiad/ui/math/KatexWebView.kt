package com.locus.olympiad.ui.math

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

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
 * Custom WebView dedicated to rendering LaTeX mathematical equations and academic monographs
 * completely offline using bundled KaTeX assets and Computer Modern fonts.
 * Smoothly transitions opacity to prevent FOUC (flash of unstyled content) and dynamically
 * adjusts height using document.fonts.ready.
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
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            defaultTextEncodingName = "UTF-8"
        }

        // Configure Dark Mode support explicitly
        val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    settings,
                    WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                )
            }
        } else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
            }
        }

        setBackgroundColor(Color.parseColor("#121212"))
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    private var lastReportedHeightPx = -1

    private fun setupResizeBridge() {
        val bridge = MathResizeBridge { calculatedHeightDp ->
            post {
                val density = resources.displayMetrics.density
                val heightPx = (calculatedHeightDp * density).toInt()
                if (Math.abs(lastReportedHeightPx - heightPx) > 4) {
                    lastReportedHeightPx = heightPx
                    if (layoutParams != null) {
                        layoutParams.height = heightPx
                        requestLayout()
                    } else {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
                    }
                    onHeightChangedListener?.invoke(heightPx)
                }
            }
        }
        addJavascriptInterface(bridge, "Android")
    }

    fun setOnHeightChangedListener(listener: (Int) -> Unit) {
        this.onHeightChangedListener = listener
    }

    /**
     * Pre-processes LaTeX text and document-level environments into styled HTML elements
     * so that sections, theorems, lemmas, and lists render with academic typography.
     */
    private fun preprocessLatexDocument(input: String): String {
        return input
            // Map \section*{Title} or \section{Title}
            .replace(Regex("""\\section\*?\{([^}]+)\}""")) { match ->
                """<div class="latex-section">${match.groupValues[1]}</div>"""
            }
            // Map \subsection*{Title} or \subsection{Title}
            .replace(Regex("""\\subsection\*?\{([^}]+)\}""")) { match ->
                """<div class="latex-subsection">${match.groupValues[1]}</div>"""
            }
            // Map \begin{theorem}[Header] ... \end{theorem}
            .replace(Regex("""\\begin\{theorem\}\[([^\]]+)\]([\s\S]*?)\\end\{theorem\}""")) { match ->
                val title = match.groupValues[1]
                val body = match.groupValues[2].trim()
                """<div class="latex-theorem"><div class="latex-theorem-title">Theorem ($title)</div><div class="latex-theorem-body">$body</div></div>"""
            }
            // Map \begin{theorem} ... \end{theorem}
            .replace(Regex("""\\begin\{theorem\}([\s\S]*?)\\end\{theorem\}""")) { match ->
                val body = match.groupValues[1].trim()
                """<div class="latex-theorem"><div class="latex-theorem-title">Theorem</div><div class="latex-theorem-body">$body</div></div>"""
            }
            // Map \begin{lemma}[Header] ... \end{lemma}
            .replace(Regex("""\\begin\{lemma\}\[([^\]]+)\]([\s\S]*?)\\end\{lemma\}""")) { match ->
                val title = match.groupValues[1]
                val body = match.groupValues[2].trim()
                """<div class="latex-theorem"><div class="latex-theorem-title">Lemma ($title)</div><div class="latex-theorem-body">$body</div></div>"""
            }
            // Map \begin{lemma} ... \end{lemma}
            .replace(Regex("""\\begin\{lemma\}([\s\S]*?)\\end\{lemma\}""")) { match ->
                val body = match.groupValues[1].trim()
                """<div class="latex-theorem"><div class="latex-theorem-title">Lemma</div><div class="latex-theorem-body">$body</div></div>"""
            }
            // Map \begin{itemize} ... \end{itemize}
            .replace(Regex("""\\begin\{itemize\}([\s\S]*?)\\end\{itemize\}""")) { match ->
                val inner = match.groupValues[1]
                val items = inner.split(Regex("""\\item\s+""")).filter { it.isNotBlank() }
                val lis = items.joinToString("\n") { "<li>${it.trim()}</li>" }
                """<ul class="latex-itemize">$lis</ul>"""
            }
            // Map \textbf{...} in text to <strong>...</strong>
            .replace(Regex("""\\textbf\{([^}]+)\}""")) { match ->
                """<strong>${match.groupValues[1]}</strong>"""
            }
            // Map \textit{...} in text to <em>...</em>
            .replace(Regex("""\\textit\{([^}]+)\}""")) { match ->
                """<em>${match.groupValues[1]}</em>"""
            }
    }

    /**
     * Renders LaTeX content using the bundled KaTeX engine and CMU Serif configuration wrapper.
     */
    fun setLatexContent(latexContent: String) {
        lastReportedHeightPx = -1
        val processed = preprocessLatexDocument(latexContent)
        var wrappedHtml = HTML_TEMPLATE.replace("{LATEX_CONTENT_PLACEHOLDER}", processed).trim()
        if (!wrappedHtml.startsWith("<!DOCTYPE html>", ignoreCase = true)) {
            wrappedHtml = "<!DOCTYPE html>\n$wrappedHtml"
        }
        loadDataWithBaseURL(
            "file:///android_asset/katex/",
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
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">
    <script src="file:///android_asset/katex/katex.min.js"></script>
    <script src="file:///android_asset/katex/contrib/auto-render.min.js"></script>
    <style>
        * { box-sizing: border-box; }
        html, body {
            margin: 0;
            padding: 0;
            height: auto !important;
            min-height: 0 !important;
            display: inline-block;
            width: 100%;
            overflow: hidden;
        }
        body {
            margin: 0;
            padding: 0;
            height: auto !important;
            min-height: 0 !important;
            display: inline-block;
            overflow: hidden;
            opacity: 0;
            transition: opacity 0.25s ease-in;
            background-color: #121212;
            color: #FFFFFF;
            font-family: "KaTeX_Main", "Times New Roman", serif !important;
            font-size: 17px;
            line-height: 1.6;
        }
        @media (prefers-color-scheme: dark) {
            html, body {
                background-color: #121212 !important;
                color: #FFFFFF !important;
            }
        }
        .katex {
            font-family: "KaTeX_Main", "Times New Roman", serif !important;
        }
        #content {
            white-space: pre-wrap;
            word-wrap: break-word;
            margin: 0;
            padding: 2px 4px;
        }
        .katex-display {
            overflow-x: auto;
            overflow-y: hidden;
            padding: 4px 0;
            margin: 8px 0;
        }
        /* Structural document macros */
        .latex-section {
            font-size: 20px;
            font-weight: bold;
            color: #F8FAFC;
            border-bottom: 2px solid #8B5CF6;
            padding-bottom: 6px;
            margin-top: 18px;
            margin-bottom: 12px;
        }
        .latex-subsection {
            font-size: 16px;
            font-weight: bold;
            color: #C4B5FD;
            margin-top: 14px;
            margin-bottom: 8px;
        }
        .latex-theorem {
            background: #1E1B4B4D;
            border-left: 4px solid #8B5CF6;
            border-radius: 0 10px 10px 0;
            padding: 12px 16px;
            margin: 14px 0;
            font-style: italic;
        }
        .latex-theorem-title {
            font-weight: bold;
            font-style: normal;
            color: #A78BFA;
            margin-bottom: 6px;
            font-size: 14px;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }
        .latex-theorem-body {
            line-height: 1.6;
        }
        .latex-itemize {
            margin: 10px 0;
            padding-left: 20px;
        }
        .latex-itemize li {
            margin-bottom: 8px;
            line-height: 1.5;
        }
    </style>
</head>
<body>
    <div id="content">{LATEX_CONTENT_PLACEHOLDER}</div>
    <script>
        var lastSentHeight = 0;
        function updateAndSendHeight() {
            document.body.style.opacity = "1";
            var contentEl = document.getElementById("content");
            var height = 0;
            if (contentEl) {
                var rect = contentEl.getBoundingClientRect();
                height = Math.ceil(rect.height || contentEl.offsetHeight || contentEl.scrollHeight || 0);
            }
            if (!height || height === 0) {
                height = Math.ceil(document.body.offsetHeight || document.documentElement.offsetHeight || 0);
            }
            if (height > 0 && Math.abs(height - lastSentHeight) > 3) {
                lastSentHeight = height;
                if (window.Android && window.Android.setHeight) {
                    window.Android.setHeight(height);
                }
            }
        }

        document.addEventListener("DOMContentLoaded", function() {
            try {
                if (typeof renderMathInElement === "function") {
                    renderMathInElement(document.body, {
                        delimiters: [
                            {left: "$$", right: "$$", display: true},
                            {left: "\\[", right: "\\]", display: true},
                            {left: "$", right: "$", display: false},
                            {left: "\\(", right: "\\)", display: false}
                        ],
                        trust: true,
                        throwOnError: false,
                        macros: {
                            "\\section": "\\htmlClass{latex-section}{\\mathbf{#1}}",
                            "\\subsection": "\\htmlClass{latex-subsection}{\\mathbf{#1}}",
                            "\\theorem": "\\htmlClass{latex-theorem}{#1}",
                            "\\R": "\\mathbb{R}",
                            "\\N": "\\mathbb{N}",
                            "\\Z": "\\mathbb{Z}",
                            "\\Q": "\\mathbb{Q}",
                            "\\C": "\\mathbb{C}"
                        }
                    });
                }
            } catch (e) {
                console.error("KaTeX render error:", e);
            }

            var contentEl = document.getElementById("content");
            if (window.ResizeObserver && contentEl) {
                var ro = new ResizeObserver(function() {
                    updateAndSendHeight();
                });
                ro.observe(contentEl);
            }

            if (document.fonts && document.fonts.ready) {
                document.fonts.ready.then(function() {
                    updateAndSendHeight();
                }).catch(function() {
                    updateAndSendHeight();
                });
            } else {
                updateAndSendHeight();
            }

            setTimeout(updateAndSendHeight, 200);
            setTimeout(updateAndSendHeight, 500);
        });

        window.addEventListener("load", function() {
            updateAndSendHeight();
        });
    </script>
</body>
</html>"""
    }
}
