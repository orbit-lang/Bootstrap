package org.orbit.util

import java.awt.print.Printable

enum class PrintableKey(val generator: (PrintableFactory) -> String) {
	Bold(PrintableFactory::getBold),
	Warning(PrintableFactory::getWarning),
	Error(PrintableFactory::getError),
	Underlined(PrintableFactory::getUnderlined),
	Success(PrintableFactory::getSuccess);

	operator fun plus(other: PrintableKey) : List<PrintableKey> {
		return listOf(this, other)
	}
}

fun <T, U> partial(arg: T, fn: (T) -> U) : () -> U {
	return {
		fn(arg)
	}
}

interface PrintableFactory {
	fun getTerminator() : String
	fun getError() : String
	fun getWarning() : String
	fun getBold() : String
	fun getUnderlined() : String
	fun getSuccess() : String

	private fun appendIfPresent(keys: Array<out PrintableKey>, key: PrintableKey, fn: () -> String) : String? {
		return when (keys.contains(key)) {
			true -> fn()
			else -> null
		}
	}

	fun getPrintable(vararg keys: PrintableKey) : String {
		return keys.mapNotNull { appendIfPresent(keys, it, partial(this, it.generator)) }
			.joinToString("")
	}
}

class Printer(private val factory: PrintableFactory) {
	fun apply(text: String, vararg keys: PrintableKey) : String {
		val headers = factory.getPrintable(*keys)
		
		return "$headers$text${factory.getTerminator()}"
	}
}