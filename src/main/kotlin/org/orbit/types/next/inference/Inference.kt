package org.orbit.types.next.inference

import com.sun.xml.internal.bind.v2.model.core.TypeRef
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.Node
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Result
import org.orbit.util.next.*

sealed interface InferenceResult {
    data class Success<T: IType>(val type: T) : InferenceResult
    data class Failure(val never: NeverType) : InferenceResult
}

fun IType.inferenceResult() : InferenceResult = when (this) {
    is Never -> InferenceResult.Failure(this)
    else -> InferenceResult.Success(this)
}

data class TypeReference(override val fullyQualifiedName: String) : ValueType {
    override val isSynthetic: Boolean = true

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (fullyQualifiedName) {
        other.fullyQualifiedName -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}

interface Inference<N: Node, T: IType> {
    fun infer(inferenceUtil: InferenceUtil, node: N) : InferenceResult
}

class InferenceUtil(private val typeMap: ITypeMap, private val bindingScope: IBindingScope) : KoinComponent, ITypeMap by typeMap, IBindingScope by bindingScope {
    private val inferences = mutableMapOf<Class<out Node>, Inference<*, *>>()
    private val invocation: Invocation by inject()

    fun <N: Node> registerPathResolver(inference: Inference<N, *>, nodeType: Class<N>) {
        inferences[nodeType] = inference
    }

    private fun registerAllPathResolvers(from: InferenceUtil) {
        inferences.putAll(from.inferences)
    }

    fun getTypeMap() : ITypeMapRead = typeMap

    fun derive(retainsTypeMap: Boolean, retainsBindingScope: Boolean) : InferenceUtil {
        val nTypeMap = when (retainsTypeMap) {
            true -> typeMap
            else -> TypeMap()
        }

        val nBindingScope: IBindingScope = when (retainsBindingScope) {
            true -> BindingScope.Leaf(bindingScope)
            else -> BindingScope.Leaf(BindingScope.Root)
        }

        val nInferenceUtil = InferenceUtil(nTypeMap, nBindingScope)

        nInferenceUtil.registerAllPathResolvers(this)

        return nInferenceUtil
    }

    fun <N: Node> infer(node: N, autoCaptureType: Boolean = true) : IType {
        val t = typeMap.get(node)

        if (t != null) {
            return t
        }

        val inference = inferences[node::class.java] as? Inference<N, *>
            ?: throw invocation.make<TypeSystem>("Inference class not registered for node: $node", node)

        return when (val result = inference.infer(this, node)) {
            is InferenceResult.Success<*> -> result.type.apply {
                if (autoCaptureType) typeMap.set(node, this)
            }

            is InferenceResult.Failure -> throw invocation.make<TypeSystem>(result.never.message, result.never.position)
        }
    }

    inline fun <N: Node, reified T: IType> inferAsOrNull(node: N) : T?
        = infer(node) as? T

    inline fun <N: Node, reified T: IType> inferAs(node: N) : T
        = inferAsOrNull(node)!!

    fun inferAll(nodes: List<Node>) : List<IType> = nodes.map(::infer)

    inline fun <N: Node, reified T: IType> inferAllAs(nodes: List<N>) : List<T>
        = nodes.map { inferAs(it) }
}