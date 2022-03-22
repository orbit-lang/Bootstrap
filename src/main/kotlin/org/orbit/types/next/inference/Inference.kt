package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Node
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.next.*
import java.lang.RuntimeException

sealed interface InferenceResult {
    data class Success<T: TypeComponent>(val type: T) : InferenceResult
    data class Failure(val never: NeverType) : InferenceResult
}

fun TypeComponent.inferenceResult() : InferenceResult = when (this) {
    is Never -> InferenceResult.Failure(this)
    else -> InferenceResult.Success(this)
}

interface ITypeRef : ValueType, ITrait, IType {
    override fun deriveTrait(ctx: Ctx): ITrait = when (val type = ctx.getTypeAs<IType>(fullyQualifiedName)) {
        null -> Never
        else -> type.deriveTrait(ctx)
    }

    override fun getFields(): List<Field> = emptyList()
}

data class TypeReference(override val fullyQualifiedName: String) : ITypeRef {
    constructor(path: Path) : this(path.toString(OrbitMangler))

    companion object : ITypeRef {
        override val fullyQualifiedName: String
            get() = throw RuntimeException("FATAL - Naked Type Reference")
        override val isSynthetic: Boolean
            get() = throw RuntimeException("FATAL - Naked Type Reference")

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
    fun infer(inferenceUtil: InferenceUtil, node: N) : InferenceResult
}

interface InferenceContext {
    val nodeType: Class<out Node>
}

data class AnyInferenceContext(override val nodeType: Class<out Node>) : InferenceContext {
    override fun equals(other: Any?): Boolean = when (other) {
        is AnyInferenceContext -> nodeType == other.nodeType
        else -> false
    }

    override fun hashCode(): Int = nodeType.hashCode()
}

class InferenceUtil(private val typeMap: ITypeMap, private val bindingScope: IBindingScope, val self: TypeComponent? = null) : KoinComponent, ITypeMap by typeMap, IBindingScope by bindingScope {
    private val inferences = mutableMapOf<InferenceContext, Inference<*, *>>()
    private val invocation: Invocation by inject()

    fun <N: Node> registerInference(inference: Inference<N, *>, context: InferenceContext) {
        inferences[context] = inference
    }

    fun <N: Node> registerInference(inference: Inference<N, *>, nodeType: Class<N>) {
        inferences[AnyInferenceContext(nodeType)] = inference
    }

    private fun registerAllInferences(from: InferenceUtil) {
        inferences.putAll(from.inferences)
    }

    fun getTypeMap() : ITypeMapRead = typeMap

    fun derive(retainsTypeMap: Boolean, retainsBindingScope: Boolean, self: TypeComponent? = null) : InferenceUtil {
        val nTypeMap = when (retainsTypeMap) {
            true -> typeMap
            else -> TypeMap()
        }

        val nBindingScope: IBindingScope = when (retainsBindingScope) {
            true -> BindingScope.Leaf(bindingScope)
            else -> BindingScope.Leaf(BindingScope.Root)
        }

        val nInferenceUtil = InferenceUtil(nTypeMap, nBindingScope, self)

        nInferenceUtil.registerAllInferences(this)

        return nInferenceUtil
    }

    fun <N: Node> infer(node: N, context: InferenceContext = AnyInferenceContext(node::class.java), autoCaptureType: Boolean = true) : TypeComponent {
        val t = typeMap.get(node)

        if (t != null) {
            return t
        }

        val inference = inferences[context] as? Inference<N, *>
            ?: throw invocation.compilerError<TypeSystem>("Inference class not registered for node: $node", node)

        return when (val result = inference.infer(this, node)) {
            is InferenceResult.Success<*> -> result.type.apply {
                if (autoCaptureType) typeMap.set(node, this)
            }

            is InferenceResult.Failure -> throw invocation.make<TypeSystem>(result.never.message, result.never.position)
        }
    }

    inline fun <N: Node, reified T: TypeComponent> inferAsOrNull(node: N, context: InferenceContext = AnyInferenceContext(node::class.java)) : T?
        = infer(node, context) as? T

    inline fun <N: Node, reified T: TypeComponent> inferAs(node: N, context: InferenceContext = AnyInferenceContext(node::class.java)) : T
        = inferAsOrNull(node, context)!!

    fun inferAll(nodes: List<Node>, context: InferenceContext) : List<TypeComponent> = nodes.map { infer(it, context) }

    inline fun <N: Node, reified T: TypeComponent> inferAllAs(nodes: List<N>, context: InferenceContext) : List<T>
        = nodes.map { inferAs(it, context) }
}