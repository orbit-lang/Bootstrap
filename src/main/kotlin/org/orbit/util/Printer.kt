	package org.orbit.util

enum class PrintableKey {
	Bold, Warning, Error, Underlined, Success
}

interface PrintableFactory {
	fun getTerminator() : String
	fun getError() : String
	fun getWarning() : String
	fun getBold() : String
	fun getUnderlined() : String
	fun getSuccess() : String

	fun getPrintable(key: PrintableKey) : String = when (key) {
		PrintableKey.Bold -> getBold()
		PrintableKey.Warning -> getWarning()
		PrintableKey.Error -> getError()
		PrintableKey.Underlined -> getUnderlined()
		PrintableKey.Success -> getSuccess()
	}
}

class Printer(private val factory: PrintableFactory) {
	fun apply(text: String, key: PrintableKey) : String {
		val header = factory.getPrintable(key)
		
		return "$header$text${factory.getTerminator()}"
	}
}