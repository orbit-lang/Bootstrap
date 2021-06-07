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
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation

class MethodSignatureTypeResolver(override val node: MethodSignatureNode, override val binding: Binding, private val enclosingTrait: Trait? = null) : TypeResolver<MethodSignatureNode, SignatureProtocol<out TypeProtocol>>,
    KoinComponent {
    private val invocation: Invocation by inject()

    constructor(pair: Pair<MethodSignatureNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): SignatureProtocol<out TypeProtocol> {
        val receiver = node.receiverTypeNode
        val argTypes = mutableListOf<Parameter>()
        val parameterBindings = mutableListOf<String>()

        var isInstanceMethod = false
        if (receiver.identifierNode.identifier != "Self") {
            isInstanceMethod = true
            // TODO - Handle Type methods (no instance receiver)
            val receiverType = when (val receiverPath = receiver.getPath()) {
                Path.self -> enclosingTrait ?: throw invocation.make<TypeChecker>("Using 'Self' type outside of a Trait definitions is not supported", node)
                else -> context.getType(receiverPath)
            }

            argTypes.add(Parameter(receiver.identifierNode.identifier, receiverType))
        }

        node.parameterNodes.forEach {
            val t = context.getType(it.getPath())

            //context.bind(it.identifierNode.identifier, t)
            parameterBindings.add(it.identifierNode.identifier)

            argTypes.add(Parameter(it.identifierNode.identifier, t))
        }

        val returnType: ValuePositionType = if (node.returnTypeNode == null) {
            IntrinsicTypes.Unit.type
        } else {
            context.getType(node.returnTypeNode.getPath()) as ValuePositionType
        }

        val receiverType = when (val receiverPath = receiver.getPath()) {
            Path.self -> enclosingTrait ?: throw invocation.make<TypeChecker>("Using 'Self' type outside of a Trait definition is not supported", node)
            else -> context.getType(receiverPath)
        } as ValuePositionType

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