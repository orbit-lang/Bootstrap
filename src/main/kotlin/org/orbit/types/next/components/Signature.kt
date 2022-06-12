package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

interface ISignature : DeclType, Member {
    val isInstanceMethod: Boolean

    fun getSignature(printer: Printer) : ISignature
    fun getSignatureTypeParameters() : List<AbstractTypeParameter> = emptyList()
    fun getReceiverType() : TypeComponent
    fun getName() : String
    fun getParameterTypes() : List<TypeComponent>
    fun getReturnType() : TypeComponent
}

fun ISignature.toFunc() : Func = Func(getParameterTypes(), getReturnType())

object SignatureSubstitutor : Substitutor<Signature> {
    override fun substitute(target: Signature, old: TypeComponent, new: TypeComponent): Signature
        = Signature(target.relativeName,
            target.receiver.substitute(old, new),
            target.parameters.map { it.substitute(old, new) },
            target.returns.substitute(old, new),
            target.isSynthetic,
            target.isInstanceMethod)
}

data class Signature(val relativeName: String, val receiver: TypeComponent, val parameters: List<TypeComponent>, val returns: TypeComponent, override val isSynthetic: Boolean = false, override val isInstanceMethod: Boolean) : ISignature {
    override val fullyQualifiedName: String
        get() = (Path(receiver.fullyQualifiedName, relativeName) + Path(relativeName) + parameters.map { Path(it.fullyQualifiedName) } + OrbitMangler.unmangle(returns.fullyQualifiedName))
            .toString(OrbitMangler)

    override val kind: Kind = IntrinsicKinds.Type
    override val memberName: String = relativeName
    override val type: TypeComponent = this

    fun withInferredReturnType(type: TypeComponent) : Signature
        = Signature(relativeName, receiver, parameters, type, isSynthetic, isInstanceMethod)

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (NominalEq.eq(ctx, this, other)) {
        true -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }

    override fun getSignature(printer: Printer): ISignature = this

    override fun getName(): String = relativeName
    override fun getReceiverType(): TypeComponent = receiver
    override fun getParameterTypes(): List<TypeComponent> = parameters
    override fun getReturnType(): TypeComponent = returns
    
    fun derive() : Trait {
        val trait = Trait(getPath(OrbitMangler) + "CallableInterface")
        val receiverContract = FieldContract(trait, Field("__receiver", receiver))
        val returnsContract = FieldContract(trait, Field("__returns", returns))
        val contracts = parameters.mapIndexed { idx, type -> FieldContract(trait, Field("$idx", type)) }

        return Trait(trait.fullyQualifiedName, contracts + receiverContract + returnsContract)
    }

    override fun toString(printer: Printer): String {
        val args = parameters.joinToString(", ") { it.toString(printer) }
        return "(${receiver.toString(printer)}) ${printer.apply(relativeName, PrintableKey.Bold, PrintableKey.Italics)} ($args) (${returns.toString(printer)})"
    }
}
