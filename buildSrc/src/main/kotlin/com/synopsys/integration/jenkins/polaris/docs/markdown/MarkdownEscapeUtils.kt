/**
 * buildSrc
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.docs.markdown

import org.apache.commons.lang3.StringUtils

//Escapes using a backslash all supported characters in a markdown text literal based on: https://daringfireball.net/projects/markdown/syntax#backslash
class MarkdownEscapeUtils {
    companion object {
        private val characters: List<String>;

        init {
            characters = "\\`*_{}[]()#+-.!".toCharArray()
                    .map { c -> c.toString() }
                    .toList()
        }

        fun escape(text: String) : String {
            var cleanedText = text;
            for (c in characters) {
                cleanedText = StringUtils.replace(cleanedText, c, "\\" + c);
            }
            return cleanedText;
        }
    }
}