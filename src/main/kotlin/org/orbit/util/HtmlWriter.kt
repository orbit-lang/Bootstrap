package org.orbit.util

import org.orbit.frontend.Lexer
import org.orbit.frontend.TokenTypes

interface HtmlWriter {
    fun toHtmlString(depth: Int) : String
}