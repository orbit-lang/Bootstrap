package org.orbit.integration

import org.orbit.util.Platform
import org.orbit.util.PrintableFactory

internal object TestPlatform : Platform, PrintableFactory {
    override val name: String = "Test"

    override fun getPrintableFactory(): PrintableFactory = this
    override fun getTerminator(): String = ""
    override fun getError(): String = ""
    override fun getWarning(): String = ""
    override fun getBold(): String = ""
    override fun getUnderlined(): String = ""
    override fun getSuccess(): String = ""
}