package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.INode
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance
import org.orbit.util.next.*

class InferenceUtil(private val typeMap: ITypeMap, private val bindingScope: IBindingScope, val self: TypeComponent? = null) : KoinComponent, ITypeMap by typeMap, IBindingScope by bindingScope {
    private val inferences = mutableMapOf<InferenceContext, Inference<*, *>>()
    private val invocation: Invocation by inject()

    fun <N: INode> registerInference(inference: Inference<N, *>, context: InferenceContext) {
        inferences[context] = inference
    }

    fun <N: INode> registerInference(inference: Inference<N, *>, nodeType: Class<N>) {
        inferences[AnyInferenceContext(nodeType)] = inference
    }

    private fun registerAllInferences(from: InferenceUtil) {
        inferences.putAll(from.inferences)
    }

    fun getTypeMap() : ITypeMapRead = typeMap

    fun derive(retainsTypeMap: Boolean = true, retainsBindingScope: Boolean = true, self: TypeComponent? = null) : InferenceUtil {
        val nTypeMap = when (retainsTypeMap) {
            true -> TypeMap(typeMap as TypeMap)
            else -> TypeMap()
        }

        val nBindingScope: IBindingScope = when (retainsBindingScope) {
            true -> (bindingScope as BindingScope).derive()
            else -> BindingScope.Leaf(BindingScope.Root)
        }

        val nInferenceUtil = InferenceUtil(nTypeMap, nBindingScope, self)

        nInferenceUtil.registerAllInferences(this)

        self?.let {
            nInferenceUtil.declare(Alias("Self", it))
        }

        return nInferenceUtil
    }

    fun <N: INode> infer(node: N, context: InferenceContext = AnyInferenceContext(node::class.java), autoCaptureType: Boolean = true) : TypeComponent {
        val t = typeMap.get(node)

        if (t != null) {
            return t
        }

        val inference = inferences[context] as? Inference<N, *>
            ?: inferences[AnyInferenceContext(node::class.java)] as? Inference<N, *>
            ?: throw invocation.compilerError<TypeSystem>("Inference class not registered for node: $node\nin context: $context", node)

        return when (val result = inference.infer(this, context, node)) {
            is InferenceResult.Success<*> -> result.type.apply {
                if (context is TypeAnnotatedInferenceContext<*>) {
                    if (!AnyEq.eq(toCtx(), context.typeAnnotation, this)) {
                        val printer: Printer = getKoinInstance()
                        return Never(
                            "Inferred Type ${this.toString(printer)} does not match Type Annotation ${
                                context.typeAnnotation.toString(
                                    printer
                                )
                            }", node.firstToken.position
                        )
                    }
                }

                if (autoCaptureType) typeMap.set(node, this)
            }

            is InferenceResult.Failure -> throw invocation.make<TypeSystem>(result.never.message, result.never.position)
        }
    }

    inline fun <N: INode, reified T: TypeComponent> inferAsOrNull(node: N, context: InferenceContext = AnyInferenceContext(node::class.java)) : T?
        = infer(node, context) as? T

    inline fun <N: INode, reified T: TypeComponent> inferAs(node: N, context: InferenceContext = AnyInferenceContext(node::class.java)) : T
        = inferAsOrNull(node, context)!!

    fun inferAll(nodes: List<INode>, context: InferenceContext) : List<TypeComponent> = nodes.map { infer(it, context) }

    inline fun <N: INode, reified T: TypeComponent> inferAllAs(nodes: List<N>, context: InferenceContext) : List<T>
        = nodes.map { inferAs(it, context) }
}