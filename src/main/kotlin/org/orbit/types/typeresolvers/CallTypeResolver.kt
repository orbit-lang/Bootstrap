package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.CallNode
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.Function
import org.orbit.types.components.AnyEquality
import org.orbit.types.components.Context
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.types.components.TypeProtocol
import org.orbit.util.Invocation

class CallTypeResolver(override val node: CallNode, override val binding: Binding, private val expectedType: TypeProtocol? = null) : TypeResolver<CallNode, TypeProtocol>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context): TypeProtocol {
        val receiverType = TypeInferenceUtil.infer(context, node.receiverExpression)
        val functionType = TypeInferenceUtil.infer(context, node.messageIdentifier) as? Function
            ?: throw RuntimeException("Right-hand side of method call must resolve to a function type")

        // TODO - Infer parameter types from callNode
        val parameterTypes = listOf(receiverType) + node.parameterNodes.map {
            TypeInferenceUtil.infer(context, it)
        }

        val argumentTypes = functionType.inputTypes

        if (parameterTypes.size != argumentTypes.size) {
            // TODO - It would be nice to send these errors up to Invocation
            throw RuntimeException("Method '${node.messageIdentifier.identifier}' declares ${argumentTypes.size} arguments (including receiver), found ${parameterTypes.size}")
        }

        for ((idx, pair) in argumentTypes.zip(parameterTypes).withIndex()) {
            // TODO - Named parameters
            // NOTE - For now, parameters must match order of declared arguments 1-to-1
            val equalitySemantics = pair.first.equalitySemantics as AnyEquality
            if (!equalitySemantics.isSatisfied(context, pair.first, pair.second)) {
                throw RuntimeException("Method '${node.messageIdentifier.identifier}' declares a parameter of type '${pair.first.name}' at position $idx, found '${pair.second.name}'")
            }

        }

        return receiverType
    }
}