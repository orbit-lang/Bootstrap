package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation

class MethodSignatureTypeResolver(override val node: MethodSignatureNode, override val binding: Binding, private val enclosingType: Entity? = null) : TypeResolver<MethodSignatureNode, TypeSignature>,
    KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<MethodSignatureNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): TypeSignature {
        val receiver = node.receiverTypeNode
        val argTypes = mutableListOf<Parameter>()
        val parameterBindings = mutableListOf<String>()

        val typeParameters = when (val tp = node.typeParameters) {
            null -> emptyList()
            else -> tp.typeParameters.map(::TypeParameter)
        }

        typeParameters.forEach(context::add)

        val receiverType: ValuePositionType
        if (receiver.identifierNode.identifier == "Self") {
            receiverType = if (receiver.typeExpressionNode.value == "Self") {
                enclosingType
                    ?: throw invocation.make<TypeSystem>(
                        "Using 'Self' type outside of a Trait definition is not supported",
                        node
                    )
            } else {
                TypeExpressionTypeResolver(receiver.typeExpressionNode, binding)
                    .resolve(environment, context)
                    .evaluate(context) as ValuePositionType
            }
        } else {
            // TODO - Handle Type methods (no instance receiver)
            receiverType = when (receiver.getPath()) {
                Path.self -> enclosingType ?: throw invocation.make<TypeSystem>("Using 'Self' type outside of a Trait definition is not supported", node)
                else -> TypeExpressionTypeResolver(receiver.typeExpressionNode, binding)
                    .resolve(environment, context)
                    .evaluate(context) as ValuePositionType
            }

            receiver.annotate(receiverType, Annotations.Type)
            receiver.typeExpressionNode.annotate(receiverType, Annotations.Type)
            argTypes.add(Parameter(receiver.identifierNode.identifier, receiverType))
        }

        node.parameterNodes.forEach {
            val resolver = TypeExpressionTypeResolver(it.typeExpressionNode, Binding.empty)
            val t = resolver.resolve(environment, context)
                .evaluate(context)

            parameterBindings.add(it.identifierNode.identifier)

            argTypes.add(Parameter(it.identifierNode.identifier, t))
        }

        val returnType: ValuePositionType = if (node.returnTypeNode == null) {
            IntrinsicTypes.Unit.type
        } else {
            TypeExpressionTypeResolver(node.returnTypeNode, binding)
                .resolve(environment, context)
                .evaluate(context) as ValuePositionType
        }

        node.returnTypeNode?.annotate(returnType, Annotations.Type)

        // NOTE - Decided to erase the difference between Type & Instance methods here.
        //  From this point, the compiler sees instance methods as type methods with an extra "self" param
        val funcType = TypeSignature(node.identifierNode.identifier, receiverType, argTypes, returnType, typeParameters)

        node.annotate(funcType, Annotations.Type)

        return funcType
    }
}