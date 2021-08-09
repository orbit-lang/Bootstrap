package org.orbit.integration

import org.orbit.util.Platform
import org.orbit.util.PrintableFactory
import org.orbit.util.PrintableKey

internal object TestPlatform : Platform, PrintableFactory {
    override val name: String = "Test"

    override fun getPrintableFactory(): PrintableFactory = this
    override fun getTerminator(): String = ""
    override fun getError(): String = ""
    override fun getWarning(): String = ""
    override fun getBold(): String = ""
    override fun getUnderlined(): String = ""
    override fun getSuccess(): String = ""
    override fun getItalics(): String = ""
    override fun getPrintable(vararg keys: PrintableKey): String = ""
}