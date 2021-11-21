package org.orbit.types.components

import org.orbit.core.Mangler
import org.orbit.types.phase.AnyEqualityConstraint

data class TypeSignature(
    override val name: String,
    override val receiver: ValuePositionType,
    override val parameters: List<Parameter>,
    override val returnType: ValuePositionType,
    val typeParameters: List<TypeParameter> = emptyList()
) : SignatureProtocol<ValuePositionType> {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = SignatureEquality
    override val isEphemeral: Boolean = false
    override val kind: TypeKind = FunctionKind

    override fun toString(mangler: Mangler): String {
        return mangler.mangle(this)
    }

    override fun isReceiverSatisfied(by: Entity, context: ContextProtocol): Boolean
        = AnyEqualityConstraint(receiver).checkConformance(context, by)
}