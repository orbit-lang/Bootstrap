package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.ParameterNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.*
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object TypeDefInference : Inference<TypeDefNode, Type>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeDefNode): InferenceResult {
        val type = Type(node.getPath())

        val fields = node.properties.map {
            if (it.typeNode.getPath().toString(OrbitMangler) == type.fullyQualifiedName) {
                val pretty = printer.apply(it.typeNode.getPath(), PrintableKey.Bold)
                return@infer Never("Type ${type.toString(printer)} must not declare fields of its own type, found field '${it.identifierNode.identifier}' of type $pretty",it.firstToken.position)
                    .inferenceResult()
            }

            val field = inferenceUtil.inferAs<ParameterNode, Field>(it)

            field
        }

        val nType = Type(node.getPath(), fields)

        inferenceUtil.declare(nType)

        return nType.inferenceResult()
    }
}