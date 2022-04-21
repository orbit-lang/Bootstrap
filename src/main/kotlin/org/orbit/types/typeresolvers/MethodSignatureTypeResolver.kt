package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.*
import org.orbit.util.Invocation

class MethodSignatureTypeResolver(override val node: MethodSignatureNode, override val binding: Binding, private val enclosingType: Entity? = null) : TypeResolver<MethodSignatureNode, TypeSignature>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context): TypeSignature {
        val receiver = node.receiverTypeNode
        val argTypes = mutableListOf<Parameter>()
        val parameterBindings = mutableListOf<String>()

        val typeParameters = when (val tp = node.typeParameters) {
            null -> emptyList()
            else -> tp.typeParameters.map(::TypeParameter)
        }

        typeParameters.forEach(context::add)

        val receiverType = TypeExpressionTypeResolver(receiver, binding)
            .resolve(environment, context)
            .evaluate(context) as ValuePositionType

//        receiver.annotate(receiverType, Annotations.Type)

        node.parameterNodes.forEach {
            val resolver = TypeExpressionTypeResolver(it.typeExpressionNode, Binding.empty)
            val t = resolver.resolve(environment, context)
                .evaluate(context)

            parameterBindings.add(it.identifierNode.identifier)

            argTypes.add(Parameter(it.identifierNode.identifier, t))
        }

        val returnType: ValuePositionType = if (node.returnTypeNode == null) {
            IntrinsicTypes.Unit.type as ValuePositionType
        } else {
            TypeExpressionTypeResolver(node.returnTypeNode, binding)
                .resolve(environment, context)
                .evaluate(context) as ValuePositionType
        }

//        node.returnTypeNode?.annotate(returnType, Annotations.Type)

        // NOTE - Decided to erase the difference between Type & Instance methods here.
        //  From this point, the compiler sees instance methods as type methods with an extra "self" param
        val funcType = TypeSignature(node.identifierNode.identifier, receiverType, argTypes, returnType, typeParameters)

//        node.annotate(funcType, Annotations.Type)

        return funcType
    }
}