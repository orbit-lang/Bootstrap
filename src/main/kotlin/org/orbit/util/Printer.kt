package org.orbit.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path

enum class PrintableKey(val generator: (PrintableFactory) -> String) {
	Bold(PrintableFactory::getBold),
	Warning(PrintableFactory::getWarning),
	Error(PrintableFactory::getError),
	Underlined(PrintableFactory::getUnderlined),
	Success(PrintableFactory::getSuccess),
	Italics(PrintableFactory::getItalics),
	None(PrintableFactory::getNone),
	Framed(PrintableFactory::getFramed),
	Punctuation(PrintableFactory::getPunctuation),
	Keyword(PrintableFactory::getKeyword);

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
	fun getItalics() : String
	fun getNone() : String
	fun getFramed() : String
	fun getKeyword() : String
	fun getPunctuation() : String

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

	fun apply(path: Path, vararg keys: PrintableKey) : String
		= apply(path.toString(OrbitMangler), *keys)

	fun apply(text: String, keys: List<PrintableKey>) : String {
		return apply(text, *keys.toTypedArray())
	}
}

interface AnyPrintable {
	fun toString(printer: Printer) : String
}

interface PrinterAware {
	val printer: Printer
}