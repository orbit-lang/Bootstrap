package org.orbit.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.phase.Phase
import org.orbit.types.components.Trait
import org.orbit.types.components.Type

interface ErrorWriter {
    fun write(printer: Printer) : String
}

inline fun <reified P: Phase<*, *>> Invocation.error(errorWriter: ErrorWriter) : Exception {
    val printer = inject<Printer>(Printer::class.java).value

    return make<P>(errorWriter.write(printer), SourcePosition.unknown)
}

private enum class Keywords : KoinComponent {
    Type,
    Projection;

    private val printer: Printer by inject()

    override fun toString(): String {
        return printer.apply(name.lowercase(), PrintableKey.Keyword)
    }
}

private enum class Punctuation(private val text: String) : KoinComponent {
    Colon(":"),
    DoubleColon("::"),
    LBrace("{"),
    RBrace("}");

    private val printer: Printer by inject()

    override fun toString(): String {
        return printer.apply(text, PrintableKey.Punctuation)
    }
}

class MissingTypeProjection(private val trait: Trait, private val type: Type) : ErrorWriter {
    override fun write(printer: Printer): String {
        return """
            |${printer.apply("Type '${type.name}' does not conform to Trait '${trait.name}'. You can resolve this issue by adding a type projection.", PrintableKey.Error)}
            |${printer.apply("", PrintableKey.None)}
            |${Keywords.Type} ${Keywords.Projection} ${type.name} ${Punctuation.Colon} ${trait.name} ${Punctuation.LBrace}
            |   ${printer.apply("// IMPLEMENT PROPERTIES & METHODS HERE", PrintableKey.Italics)}
            |${Punctuation.RBrace}
        """.trimMargin()
    }
}
