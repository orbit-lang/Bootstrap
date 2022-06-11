package org.orbit.types.next.components

import org.orbit.core.components.SourcePosition
import org.orbit.util.Printer
import org.orbit.util.next.IAlias

sealed interface InternalControlType : TypeComponent, ITrait, IType, IAlias, ISignature {
    override val memberName: String get() = fullyQualifiedName
    override val type: TypeComponent get() = this

    override fun getMembers(): List<Member> = emptyList()
    override fun merge(ctx: Ctx, other: ITrait): ITrait = other
    override fun getSignature(printer: Printer): ISignature = Never("${toString(printer)} is not a Signature")
    override fun getName(): String = ""
    override fun getParameterTypes(): List<TypeComponent> = emptyList()
    override fun getReceiverType(): TypeComponent = Never
    override fun getReturnType(): TypeComponent = Never

    operator fun plus(other: InternalControlType): InternalControlType
}

object Anything : InternalControlType {
    override val fullyQualifiedName: String = "*"
    override val isSynthetic: Boolean = true
    override val trait: ITrait = this
    override val input: ITrait = this
    override val target: TypeComponent = this
    override val contracts: List<Contract<*>> = emptyList()
    override val kind: Kind = IntrinsicKinds.Type
    override val isInstanceMethod: Boolean = false

    override fun deriveTrait(ctx: Ctx): ITrait = Anything

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = TypeRelation.Same(this, other)

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult
        = ContractResult.Success(by, this)

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String {
        TODO("Not yet implemented")
    }

    override fun plus(other: InternalControlType) = when (other) {
        is Never -> other
        else -> this
    }

    override fun substitute(old: TypeComponent, new: TypeComponent): Contract<ITrait> = this
}

interface NeverType : InternalControlType {
    val message: String
    val position: SourcePosition

    override val isInstanceMethod: Boolean
        get() = false

    override val contracts: List<Contract<*>> get() = emptyList()
    override val kind: Kind get() = IntrinsicKinds.Type
    override fun getName(): String = fullyQualifiedName
}