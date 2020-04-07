package org.orbit.util

interface Platform {
	val name: String

	fun getPrintableFactory() : PrintableFactory
}

object Unix : Platform, PrintableFactory {
	override val name = "Unix"

	override fun getPrintableFactory()
		: PrintableFactory = this

	override fun getTerminator() : String
		= "\u001b[0m"
	
	override fun getBold() : String
		= "\u001b[37;1m"

	override fun getWarning() : String
		= "\u001b[93;1m"

	override fun getError() : String
		= "\u001b[31;1m"

	override fun getUnderlined() : String
		= "\u001b[4m"

	override fun getSuccess() : String
		= "\u001b[92;1m"
}