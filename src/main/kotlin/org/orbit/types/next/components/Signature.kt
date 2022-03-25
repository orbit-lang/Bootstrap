package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.Printer

interface ISignature : DeclType {
    fun getSignature(printer: Printer) : ISignature
    fun getSignatureTypeParameters() : List<Parameter> = emptyList()
}

data class Signature(val relativeName: String, val receiver: TypeComponent, val parameters: List<TypeComponent>, val returns: TypeComponent, override val isSynthetic: Boolean = false) : ISignature {
    override val fullyQualifiedName: String
        get() = (Path(receiver.fullyQualifiedName, relativeName) + Path(relativeName) + parameters.map { Path(it.fullyQualifiedName) } + OrbitMangler.unmangle(returns.fullyQualifiedName))
            .toString(OrbitMangler)

    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (NominalEq.eq(ctx, this, other)) {
        true -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }

    override fun getSignature(printer: Printer): ISignature = this
}