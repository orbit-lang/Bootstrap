package org.orbit.types

import org.orbit.core.AdaptablePhase
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.getPathOrNull
import org.orbit.core.nodes.BlockNode
import org.orbit.core.nodes.InstanceMethodCallNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.ReturnStatementNode
import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.graph.exportTypes
import org.orbit.util.Invocation
import java.lang.RuntimeException

interface TypeResolver {
    fun resolve(environment: Environment, context: Context, binding: Binding) : Type
}

class TypeChecker(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<Environment, Context>() {
    override val inputType: Class<Environment> = Environment::class.java
    override val outputType: Class<Context> = Context::class.java

    override fun execute(input: Environment): Context {
        for (scope in input.scopes) {
            scope.exportTypes(context)
        }

        val methodResolver = MethodTypeResolver()
        val methods = input.scopes.flatMap { it.bindings.filter { b -> b.kind == Binding.Kind.Method } }

        methods.forEach { methodResolver.resolve(input, context, it) }

        invocation.storeResult(this::class.java.simpleName, input)

        return context
    }
}

class MethodTypeResolver : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding) : Type {
        val parameterBindings = mutableListOf<String>()

        try {
            val methodNodes = environment.ast.search(MethodDefNode::class.java)
                .filter {
                    it.signature.getPathOrNull() == binding.path
                }

            if (methodNodes.size > 1 || methodNodes.isEmpty()) {
                throw TODO("MethodTypeResolver:47")
            }

            val signature = methodNodes[0].signature
            val receiver = signature.receiverTypeNode
            val argTypes = mutableListOf<Type>()

            if (receiver.identifierNode.identifier != "Self") {
                // TODO - Handle Type methods (no instance receiver)
                val t = Entity(receiver.getPath().toString(OrbitMangler))

                context.bind(receiver.identifierNode.identifier, t)
                argTypes.add(t)
            }

            signature.parameterNodes.forEach {
                val t = Entity(it.getPath().toString(OrbitMangler))

                context.bind(it.identifierNode.identifier, t)
                parameterBindings.add(it.identifierNode.identifier)

                argTypes.add(t)
            }

            if (argTypes.isEmpty()) {
                argTypes.add(IntrinsicTypes.Unit.type)
            }

            val returnType = if (signature.returnTypeNode == null) {
                IntrinsicTypes.Unit.type
            } else {
                Entity(signature.returnTypeNode.getPath().toString(OrbitMangler))
            }

            val funcType = Function(argTypes, returnType)
            val body = methodNodes[0].body

            if (body.isEmpty) {
                // Return type is implied to be Unit, check signature agrees
                if (!NominalEquality(returnType, IntrinsicTypes.Unit.type).satisfied()) {
                    throw Exception("Method '${signature.identifierNode.identifier}' declares a return type of '${returnType.name}', found 'Unit'")
                }
            } else {
                val methodBodyTypeResolver = MethodBodyTypeResolver(body, returnType)

                methodBodyTypeResolver.resolve(environment, context, binding)
            }

            context.bind(signature.identifierNode.identifier, funcType)
            //context.add(funcType)

            return funcType
        } finally {
            // Garbage collect method parameter types
            context.removeAll(parameterBindings)
        }
    }
}

class MethodBodyTypeResolver(private val block: BlockNode, private val returnType: Type) : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding) : Type {
        val instanceCalls = block.search(InstanceMethodCallNode::class.java)

        for (call in instanceCalls) {
            TypeInferenceUtil.infer(context, call)
        }

        val returnStatements = block.search(ReturnStatementNode::class.java)

        for (returnStatement in returnStatements) {
            val varExpr = returnStatement.valueNode.expressionNode
            val varType = TypeInferenceUtil.infer(context, varExpr)

            if (!NominalEquality(returnType, varType).satisfied()) {
                throw Exception("Method '${binding.simpleName}' declares a return type of '${returnType.name}', found '${varType.name}'")
            }
        }

        // All return paths have been evaluated at this point. No conflicts were found,
        // which means its safe to just return the expected return type
        return returnType
    }
}

class InstanceMethodCallTypeResolver(private val callNode: InstanceMethodCallNode, private val expectedType: Type? = null) : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding): Type {
        val receiverType = TypeInferenceUtil.infer(context, callNode.receiverNode)
        val functionType = TypeInferenceUtil.infer(context, callNode.methodIdentifierNode) as? Function
            ?: throw RuntimeException("Right-hand side of method call must resolve to a function type")

        // TODO - Infer parameter types from callNode
        val parameterTypes = listOf(receiverType) + callNode.parameterNodes.map {
            TypeInferenceUtil.infer(context, it)
        }

        val argumentTypes = functionType.inputTypes

        if (parameterTypes.size != argumentTypes.size) {
            // TODO - It would be nice to send these errors up to Invocation
            throw RuntimeException("Method '${callNode.methodIdentifierNode.identifier}' declares ${argumentTypes.size} arguments (including receiver), found ${parameterTypes.size}")
        }

        for ((idx, pair) in argumentTypes.zip(parameterTypes).withIndex()) {
            // TODO - Nominal vs Structural should be programmable
            // TODO - Named parameters
            // NOTE - For now, parameters must match order of declared arguments 1-to-1
            if (!NominalEquality(pair.first, pair.second).satisfied()) {
                throw RuntimeException("Method '${callNode.methodIdentifierNode.identifier}' declares a parameter of type '${pair.first.name}' at position $idx, found '${pair.second.name}'")
            }

        }

        return receiverType
    }
}