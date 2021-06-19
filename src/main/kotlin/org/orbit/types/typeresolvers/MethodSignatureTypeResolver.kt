package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation
import org.orbit.util.partial

class TypeExpressionTypeResolver(override val node: TypeExpressionNode, override val binding: Binding) : TypeResolver<TypeExpressionNode, TypeExpression>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context) : TypeExpression = when (node) {
        is TypeIdentifierNode -> {
            val type = context.getTypeByPath(node.getPath()) as Entity

            node.annotate(type, Annotations.Type)

            type
        }
        is MetaTypeNode -> {
            val typeConstructor = context.getTypeByPath(node.getPath()) as EntityConstructor
            // TODO - Convert this to a stream
            val typeParameters = node.typeParameters
                .map(partial(::TypeExpressionTypeResolver, binding))
                .map(partial(TypeExpressionTypeResolver::resolve, environment, context))
                .map(partial(TypeExpression::evaluate, context))
                .map { it as ValuePositionType }

            val type = MetaType(typeConstructor, typeParameters)

            node.annotate(type, Annotations.Type)

            type
        }

        else -> TODO("Unsupported type expression $node")
    }
}

class MethodSignatureTypeResolver(override val node: MethodSignatureNode, override val binding: Binding, private val enclosingTrait: Trait? = null) : TypeResolver<MethodSignatureNode, SignatureProtocol<out TypeProtocol>>,
    KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<MethodSignatureNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): SignatureProtocol<out TypeProtocol> {
        val receiver = node.receiverTypeNode
        val argTypes = mutableListOf<Parameter>()
        val parameterBindings = mutableListOf<String>()

        var isInstanceMethod = false
        val receiverType: ValuePositionType
        if (receiver.identifierNode.identifier == "Self") {
            TODO("Self")
        } else {
            isInstanceMethod = true
            // TODO - Handle Type methods (no instance receiver)
            receiverType = when (receiver.getPath()) {
                Path.self -> enclosingTrait ?: throw invocation.make<TypeChecker>("Using 'Self' type outside of a Trait definitions is not supported", node)
                else -> TypeExpressionTypeResolver(receiver.typeExpressionNode, binding)
                    .resolve(environment, context)
                    .evaluate(context) as ValuePositionType
            }

            receiver.annotate(receiverType, Annotations.Type)
            receiver.typeExpressionNode.annotate(receiverType, Annotations.Type)
            argTypes.add(Parameter(receiver.identifierNode.identifier, receiverType))
        }

        node.parameterNodes.forEach {
            val t = context.getTypeByPath(it.getPath())

            //context.bind(it.identifierNode.identifier, t)
            parameterBindings.add(it.identifierNode.identifier)

            argTypes.add(Parameter(it.identifierNode.identifier, t))
        }

        val returnType: ValuePositionType = if (node.returnTypeNode == null) {
            IntrinsicTypes.Unit.type
        } else {
            TypeExpressionTypeResolver(node.returnTypeNode, binding)
                .resolve(environment, context)
                .evaluate(context) as ValuePositionType
//            context.getTypeByPath(node.returnTypeNode.getPath()) as ValuePositionType
        }

        node.returnTypeNode?.annotate(returnType, Annotations.Type)

        val funcType = if (isInstanceMethod) {
            InstanceSignature(
                node.identifierNode.identifier,
                Parameter(receiver.identifierNode.identifier, receiverType),
                argTypes,
                returnType
            )
        } else {
            TypeSignature(node.identifierNode.identifier, receiverType, argTypes, returnType)
        }

        node.annotate(funcType, Annotations.Type)

        return funcType
    }
}