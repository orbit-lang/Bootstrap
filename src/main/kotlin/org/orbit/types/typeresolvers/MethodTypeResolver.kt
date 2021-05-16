package org.orbit.types.typeresolvers

import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*

class MethodSignatureTypeResolver(override val node: MethodSignatureNode, override val binding: Binding) : TypeResolver<MethodSignatureNode, SignatureProtocol<out TypeProtocol>> {
    constructor(pair: Pair<MethodSignatureNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): SignatureProtocol<out TypeProtocol> {
        val receiver = node.receiverTypeNode
        val argTypes = mutableListOf<Parameter>()
        val parameterBindings = mutableListOf<String>()

        var isInstanceMethod = false
        if (receiver.identifierNode.identifier != "Self") {
            isInstanceMethod = true
            // TODO - Handle Type methods (no instance receiver)
            val t = context.getType(receiver.getPath())

            //context.bind(receiver.identifierNode.identifier, t)
            argTypes.add(Parameter(receiver.identifierNode.identifier, t))
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

        val receiverType = context.getType(receiver.getPath()) as ValuePositionType

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

class MethodTypeResolver(override val node: MethodDefNode, override val binding: Binding) : TypeResolver<MethodDefNode, SignatureProtocol<out TypeProtocol>> {
    constructor(pair: Pair<MethodDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context) : SignatureProtocol<out TypeProtocol> {
        val parameterBindings = mutableListOf<String>()

        try {
            val signature = node.signature.getType() as SignatureProtocol<TypeProtocol>
            val body = node.body

            signature.parameters.forEach { context.bind(it.name, it.type) }

            if (body.isEmpty) {
                // Return type is implied to be Unit, check signature agrees
                val equalitySemantics = signature.returnType.equalitySemantics as AnyEquality
                if (!equalitySemantics.isSatisfied(context, signature.returnType, IntrinsicTypes.Unit.type)) {
                    throw Exception("Method '${signature.name}' declares a return type of '${signature.returnType.name}', found 'Unit'")
                }
            } else {
                val methodBodyTypeResolver = MethodBodyTypeResolver(body, binding, signature.returnType)

                methodBodyTypeResolver.resolve(environment, context)
            }

//            context.bind(funcType.toString(OrbitMangler), funcType)

            return signature
        } finally {
            // Garbage collect method parameter types
            context.removeAll(parameterBindings)
        }
    }
}