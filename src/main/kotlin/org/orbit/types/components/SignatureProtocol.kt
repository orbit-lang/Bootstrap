package org.orbit.types.components

import org.orbit.core.Mangler
import org.orbit.types.phase.AnyEqualityConstraint
import org.orbit.types.phase.NominalEqualityConstraint
import org.orbit.types.phase.StructuralEqualityConstraint
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

interface SignatureProtocol<T: TypeProtocol> : ValuePositionType {
    val receiver: T
    val parameters: List<Parameter>
    val returnType: ValuePositionType

    fun toString(mangler: Mangler) : String
    fun isReceiverSatisfied(by: Entity, context: ContextProtocol) : Boolean
    fun isReturnTypeSatisfied(by: Entity, context: ContextProtocol) : Boolean
        = AnyEqualityConstraint(returnType).checkConformance(context, by)

    fun isParameterListSatisfied(by: List<Parameter>, context: ContextProtocol) : Boolean {
        return parameters.count() == by.count() && parameters.zip(by).all {
            AnyEqualityConstraint(it.first.type)
                .checkConformance(context, it.second.type)
        }
    }

    fun isSatisfied(by: SignatureProtocol<*>, context: Context)
        = isReceiverSatisfied(by.receiver as Entity, context)
            && isReturnTypeSatisfied(by.returnType as Entity, context)
            && isParameterListSatisfied(by.parameters, context)

    override fun toString(printer: Printer): String {
        val params = parameters.joinToString(", ") { it.toString(printer) }

        return """
            (${receiver.toString(printer)}) ${printer.apply(name, PrintableKey.Italics)} ($params) (${returnType.toString(printer)})
        """.trimIndent()
    }
}