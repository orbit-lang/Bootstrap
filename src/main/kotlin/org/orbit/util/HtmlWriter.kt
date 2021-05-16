package org.orbit.util

interface HtmlWriter {
    fun toHtmlString(depth: Int) : String
}