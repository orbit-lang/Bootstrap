package org.orbit.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.print.Printable

enum class PrintableKey(val generator: (PrintableFactory) -> String) {
	Bold(PrintableFactory::getBold),
	Warning(PrintableFactory::getWarning),
	Error(PrintableFactory::getError),
	Underlined(PrintableFactory::getUnderlined),
	Success(PrintableFactory::getSuccess),
	Italics(PrintableFactory::getItalics);

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

	fun apply(text: String, keys: List<PrintableKey>) : String {
		return apply(text, *keys.toTypedArray())
	}
}

interface PrinterAware {
	val printer: Printer
}

object PrinterAwareImpl : PrinterAware, KoinComponent {
	override val printer: Printer by inject()

	fun bold(text: String) : String = printer.apply(text, PrintableKey.Bold)
	fun warning(text: String) : String = printer.apply(text, PrintableKey.Warning)
	fun error(text: String) : String = printer.apply(text, PrintableKey.Error)
	fun success(text: String) : String = printer.apply(text, PrintableKey.Success)
	fun underline(text: String) : String = printer.apply(text, PrintableKey.Underlined)
	fun italics(text: String) : String = printer.apply(text, PrintableKey.Italics)
}