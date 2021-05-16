package org.orbit.types.typeresolvers

import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.getPathOrNull
import org.orbit.core.nodes.MethodDefNode
import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.types.components.*

class MethodTypeResolver : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding) : TypeProtocol {
        val parameterBindings = mutableListOf<String>()

        try {
            val methodNodes = environment.ast.search(MethodDefNode::class.java)
                .filter {
                    it.signature.getPathOrNull() == binding.path
                }

            if (methodNodes.size > 1 || methodNodes.isEmpty()) {
                // TODO - Methods are not currently namespaced to their enclosing Container
                throw TODO("MethodTypeResolver:47")
            }

            val signature = methodNodes[0].signature
            val receiver = signature.receiverTypeNode
            val argTypes = mutableListOf<Parameter>()

            var isInstanceMethod = false
            if (receiver.identifierNode.identifier != "Self") {
                isInstanceMethod = true
                // TODO - Handle Type methods (no instance receiver)
                val t = context.getType(receiver.getPath())

                context.bind(receiver.identifierNode.identifier, t)
                argTypes.add(Parameter(receiver.identifierNode.identifier, t))
            }

            signature.parameterNodes.forEach {
                val t = context.getType(it.getPath())

                context.bind(it.identifierNode.identifier, t)
                parameterBindings.add(it.identifierNode.identifier)

                argTypes.add(Parameter(it.identifierNode.identifier, t))
            }

            val returnType: ValuePositionType = if (signature.returnTypeNode == null) {
                IntrinsicTypes.Unit.type
            } else {
                context.getType(signature.returnTypeNode.getPath()) as ValuePositionType
            }

            val receiverType = context.getType(receiver.getPath()) as ValuePositionType
            val funcType = if (isInstanceMethod) {
                InstanceSignature(
                    signature.identifierNode.identifier,
                    Parameter(receiver.identifierNode.identifier, receiverType),
                    argTypes,
                    returnType
                )
            } else {
                TypeSignature(signature.identifierNode.identifier, receiverType, argTypes, returnType)
            }

            val body = methodNodes[0].body

            if (body.isEmpty) {
                // Return type is implied to be Unit, check signature agrees
                val equalitySemantics = returnType.equalitySemantics as AnyEquality
                if (!equalitySemantics.isSatisfied(context, returnType, IntrinsicTypes.Unit.type)) {
                    throw Exception("Method '${signature.identifierNode.identifier}' declares a return type of '${returnType.name}', found 'Unit'")
                }
            } else {
                val methodBodyTypeResolver = MethodBodyTypeResolver(body, returnType)

                methodBodyTypeResolver.resolve(environment, context, binding)
            }

            context.bind(funcType.toString(OrbitMangler), funcType)

            return funcType
        } finally {
            // Garbage collect method parameter types
            context.removeAll(parameterBindings)
        }
    }
}