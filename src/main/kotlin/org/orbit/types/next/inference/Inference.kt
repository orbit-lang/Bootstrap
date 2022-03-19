package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.Node
import org.orbit.types.next.components.IType
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.next.BindingScope
import org.orbit.util.next.IBindingScope
import org.orbit.util.next.ITypeMap
import org.orbit.util.next.TypeMap

interface Inference<N: Node> {
    fun infer(inferenceUtil: InferenceUtil, node: N) : IType
}

class InferenceUtil(private val typeMap: ITypeMap, private val bindingScope: IBindingScope) : KoinComponent, ITypeMap by typeMap, IBindingScope by bindingScope {
    private val inferences = mutableMapOf<Class<out Node>, Inference<*>>()
    private val invocation: Invocation by inject()

    fun <N: Node> registerPathResolver(inference: Inference<N>, nodeType: Class<N>) {
        inferences[nodeType] = inference
    }

    private fun registerAllPathResolvers(from: InferenceUtil) {
        inferences.putAll(from.inferences)
    }

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

    fun <N: Node> infer(node: N) : IType {
        val inference = inferences[node::class.java] as? Inference<N>
            ?: throw invocation.make<TypeSystem>("Inference class not registered for node: $node", node)

        return inference.infer(this, node)
    }
}