/**
 * buildSrc
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.docs.markdown

import freemarker.core.CommonMarkupOutputFormat
import freemarker.core.CommonTemplateMarkupOutputModel

class MarkdownOutputModel(plainTextContent: String?, markupContent: String?) : CommonTemplateMarkupOutputModel<MarkdownOutputModel>(plainTextContent, markupContent) {
    override fun getOutputFormat(): CommonMarkupOutputFormat<MarkdownOutputModel> {
        return MarkdownOutputFormat.INSTANCE;
    }
}