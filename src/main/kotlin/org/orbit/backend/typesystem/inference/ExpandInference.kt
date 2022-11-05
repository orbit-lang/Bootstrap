package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.core.nodes.ExpandNode
import org.orbit.core.nodes.IdentifierNode
import org.orbit.core.nodes.IntLiteralNode
import org.orbit.util.Invocation

object ExpandInference : ITypeInference<ExpandNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private fun inferIntLiteral(node: IntLiteralNode) : IntValue {
        return IntValue(node.value.second)
    }

    override fun infer(node: ExpandNode, env: ITypeEnvironment): AnyType = when (node.expressionNode) {
        is IntLiteralNode -> inferIntLiteral(node.expressionNode)
        is IdentifierNode -> env.getBinding(node.expressionNode.identifier)?.type ?: IType.Never("`${node.expressionNode.identifier}` is not defined in the current context")
        is ConstructorInvocationNode -> {
            val type = TypeInferenceUtils.infer(node.expressionNode, env).flatten(env)
            val struct = type as? IType.Struct
                ?: throw invocation.make<TypeSystem>("Cannot construct compile-time instance of non-Structural Type $type", node.expressionNode.typeExpressionNode)
            val args = TypeInferenceUtils.inferAll(node.expressionNode.parameterNodes, env)
            val pMap = struct.members
                .zip(args).withIndex()
                .associate {
                    val nth = when (it.index) {
                        1 -> "1st"
                        else -> "${it.index}th"
                    }
                    val constValue = it.value.second as? IValue<*, *>
                        ?: throw invocation.make<TypeSystem>("In order to construct a compile-time instance of Structural Type $struct, all constructor arguments must also be compile-time values, found runtime value of type ${it.value.second} at argument index ${it.index}\n\tSuggestion: expand $nth constructor argument", node.expressionNode)

                    it.value.first.first to constValue
                }

            InstanceValue(type, pMap)
        }
        else -> throw invocation.make<TypeSystem>("Cannot expand value at compile-time: `${node.expressionNode}`", node.expressionNode)
    }
}