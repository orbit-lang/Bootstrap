package org.orbit.types.next.inference

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Node
import org.orbit.types.next.components.*
import org.orbit.util.Printer
import java.lang.RuntimeException

sealed interface InferenceResult {
    data class Success<T: TypeComponent>(val type: T) : InferenceResult
    data class Failure(val never: NeverType) : InferenceResult

    fun typeValue() : TypeComponent = when (this) {
        is Success<*> -> type
        is Failure -> never
    }
}

fun TypeComponent.inferenceResult() : InferenceResult = when (this) {
    is Never -> InferenceResult.Failure(this)
    else -> InferenceResult.Success(this)
}

interface ITypeRef : ValueType, ITrait, IType, ISignature {
    override val isInstanceMethod: Boolean
        get() = false

    override fun deriveTrait(ctx: Ctx): ITrait = when (val type = ctx.getTypeAs<IType>(fullyQualifiedName)) {
        null -> Never
        else -> type.deriveTrait(ctx)
    }

    override fun getMembers(): List<Member> = emptyList()

    override fun merge(ctx: Ctx, other: ITrait): ITrait {
        val t = ctx.getType(fullyQualifiedName) ?: return Never

        return when (t) {
            is ITrait -> t.merge(ctx, other)
            else -> Never
        }
    }

    override fun getSignature(printer: Printer): ISignature {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getParameterTypes(): List<TypeComponent> {
        TODO("Not yet implemented")
    }

    override fun getReceiverType(): TypeComponent {
        TODO("Not yet implemented")
    }

    override fun getReturnType(): TypeComponent {
        TODO("Not yet implemented")
    }

    override fun getSignatureTypeParameters(): List<AbstractTypeParameter> {
        return super.getSignatureTypeParameters()
    }
}

data class TypeReference(override val fullyQualifiedName: String, val originalType: TypeComponent? = null) : ITypeRef {
    constructor(path: Path, originalType: TypeComponent? = null) : this(path.toString(OrbitMangler), originalType)

    override val kind: Kind = IntrinsicKinds.Type
    override val memberName: String
        get() = TODO("Not yet implemented")
    override val type: TypeComponent = this

    companion object : ITypeRef {
        override val fullyQualifiedName: String
            get() = throw RuntimeException("FATAL - Naked Type Reference")
        override val isSynthetic: Boolean
            get() = throw RuntimeException("FATAL - Naked Type Reference")

        override val memberName: String
            get() = TODO("Not yet implemented")

        override val type: TypeComponent
            get() = TODO("Not yet implemented")

        override val kind: Kind = IntrinsicKinds.Type

        override val contracts: List<Contract<*>> = emptyList()

        override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
            throw RuntimeException("FATAL - Naked Type Reference")
        }

        override val trait: ITrait
            get() = throw RuntimeException("FATAL - Naked Type Reference")
        override val input: ITrait
            get() = throw RuntimeException("FATAL - Naked Type Reference")

        override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult {
            throw RuntimeException("FATAL - Naked Type Reference")
        }

        override fun getErrorMessage(printer: Printer, type: TypeComponent): String {
            throw RuntimeException("FATAL - Naked Type Reference")
        }
    }

    override val isSynthetic: Boolean = true

    override val trait: ITrait
        get() = throw RuntimeException("FATAL- Naked Type Reference")

    override val input: ITrait
        get() = throw RuntimeException("FATAL - Naked Type Reference")

    override val contracts: List<Contract<*>> = emptyList()

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (fullyQualifiedName) {
        other.fullyQualifiedName -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult {
        val trait = ctx.getTypeAs<Trait>(fullyQualifiedName) ?: return ContractResult.Failure(by, this)

        return trait.isImplemented(ctx, by)
    }

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String {
        throw RuntimeException("FATAL - Naked Type Reference")
    }
}

interface Inference<N: Node, T: TypeComponent> {
    fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: N) : InferenceResult
}

interface InferenceContext {
    val nodeType: Class<out Node>

    fun <N: Node> clone(clazz: Class<N>) : InferenceContext
}

data class AnyInferenceContext(override val nodeType: Class<out Node>) : InferenceContext {
    override fun equals(other: Any?): Boolean = when (other) {
        is AnyInferenceContext -> nodeType == other.nodeType
        else -> false
    }

    override fun hashCode(): Int = nodeType.hashCode()

    override fun <N : Node> clone(clazz: Class<N>) : InferenceContext {
        return AnyInferenceContext(clazz)
    }
}

