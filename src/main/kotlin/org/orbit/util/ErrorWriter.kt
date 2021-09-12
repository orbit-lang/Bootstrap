package org.orbit.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.phase.Phase
import org.orbit.types.components.Property
import org.orbit.types.components.Trait
import org.orbit.types.components.Type
import org.orbit.types.components.TraitPropertyResult

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

class DuplicateTraitConformance(private val trait: Trait, private val type: Type) : ErrorWriter {
    override fun write(printer: Printer): String {
        return """
            |Type '${type.name}' declares conformance to Trait '${trait.name}' more than once.
        """.trimMargin()
    }
}

class DuplicateTraitProperty(private val type: Type, private val traitA: Trait, private val traitB: Trait, private val propertyA: Property, private val propertyB: Property) : ErrorWriter {
    override fun write(printer: Printer): String {
        return """
            |Type ${type.toString(printer)} conforms to Traits ${traitA.toString(printer)} and ${traitB.toString(printer)}, which declare conflicting properties:
            |       1. ${traitA.toString(printer)} -- ${propertyA.toString(printer)}
            |       2. ${traitB.toString(printer)} -- ${propertyB.toString(printer)}
            |       
            |   The bootstrap compiler does not currently support conflicting properties across multiple Traits - even if the types are different.
        """.trimMargin()
    }
}

class TraitEnforcerPropertyErrors(private val result: TraitPropertyResult, private val isImplicitConformance: Boolean = false) : ErrorWriter {
    private fun writeMissingProperty(printer: Printer, result: TraitPropertyResult.Missing) : String = when (isImplicitConformance) {
        // TODO - Better error message when isImplicitConformance
        true -> "Type ${result.type.toString(printer)} implicitly declares conformance to Trait ${result.trait.toString(printer)} because it is passed as a Type Parameter to a Type Constructor. However, it does not implement property ${result.property.toString(printer)}"
        false -> "Type ${result.type.toString(printer)} declares conformance to Trait ${result.trait.toString(printer)} but does not implement property ${result.property.toString(printer)}"
    }

    private fun writeDuplicateProperty(printer: Printer, result: TraitPropertyResult.Duplicate) : String {
        val propertyOwners = result.type
            .traitConformance
            .filter { it.properties.contains(result.property) }

        val traits = propertyOwners.joinToString(", ") { it.toString(printer) }
        val conflicts = propertyOwners.mapIndexed { idx, elem ->
            val property = elem.properties.first { prop -> prop.name == result.property.name }
            "\t${idx + 1}. ${elem.toString(printer)} -- ${property.toString(printer)}"
        }.joinToString("\n")

        return """
        |Type ${result.type.toString(printer)} conforms to Traits $traits, which declare conflicting properties:
        |$conflicts
        |     
        |${printer.apply("The bootstrap compiler does not currently support conflicting properties across multiple Traits - even if the types are different.", PrintableKey.Error, PrintableKey.Italics)}
        """.trimMargin()
    }

    private fun writeFailureGroup(printer: Printer, result: TraitPropertyResult.FailureGroup) : String {
        return result.results
            .map(::TraitEnforcerPropertyErrors)
            .joinToString("\n\n") { it.write(printer) }
    }

    override fun write(printer: Printer): String = when (result) {
        is TraitPropertyResult.Missing -> writeMissingProperty(printer, result)
        is TraitPropertyResult.Duplicate -> writeDuplicateProperty(printer, result)
        is TraitPropertyResult.FailureGroup -> writeFailureGroup(printer, result)
        else -> ""
    }
}
