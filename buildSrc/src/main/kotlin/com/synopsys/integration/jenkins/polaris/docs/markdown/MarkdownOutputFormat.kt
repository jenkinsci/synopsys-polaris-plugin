/**
 * buildSrc
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.docs.markdown

import freemarker.core.CommonMarkupOutputFormat
import java.io.Writer

//This class was made by following the guideline of RTFOutputFormat as proper docs on how to do this could not be found. This may be incorrect, but seems to work.
class MarkdownOutputFormat : CommonMarkupOutputFormat<MarkdownOutputModel>() {
    override fun getName(): String {
        return "Markdown";
    }

    override fun output(textToEsc: String?, out: Writer?) {
        textToEsc?.let { out?.write(MarkdownEscapeUtils.escape(it)) }
    }

    override fun newTemplateMarkupOutputModel(plainTextContent: String?, markupContent: String?): MarkdownOutputModel {
        return MarkdownOutputModel(plainTextContent, markupContent);
    }

    override fun isLegacyBuiltInBypassed(builtInName: String?): Boolean {
        return builtInName == "markdown"
    }

    override fun getMimeType(): String {
        return "text/markdown"
    }

    override fun escapePlainText(plainTextContent: String?): String {
        return MarkdownEscapeUtils.escape(plainTextContent ?: "")
    }

    companion object {
        val INSTANCE = MarkdownOutputFormat()
    }
}