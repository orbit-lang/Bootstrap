package org.orbit.types

import org.orbit.core.AdaptablePhase
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.getPathOrNull
import org.orbit.core.nodes.*
import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.graph.exportTypes
import org.orbit.util.Invocation
import java.lang.RuntimeException

interface TypeResolver {
    fun resolve(environment: Environment, context: Context, binding: Binding)
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

        invocation.storeResult(this, input)

        return context
    }
}

class MethodTypeResolver : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding) {
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

            }

            signature.parameterNodes.forEach {
                val t = Entity(it.getPath().toString(OrbitMangler))

                context.bind(it.identifierNode.identifier, t)
                parameterBindings.add(it.identifierNode.identifier)

                argTypes.add(t)
            }

            if (argTypes.isEmpty()) {
                argTypes.add(Entity.unit)
            }

            val returnType = if (signature.returnTypeNode == null) {
                Entity.unit
            } else {
                Entity(signature.returnTypeNode.getPath().toString(OrbitMangler))
            }

            val funcType = Function(argTypes, returnType)
            val body = methodNodes[0].body

            if (body.isEmpty) {
                // Return type is implied to be Unit, check signature agrees
                if (!NominalEquality(returnType, Entity.unit).satisfied()) {
                    throw Exception("Method '${signature.identifierNode.identifier}' declares a return type of '${returnType.name}', found 'Unit'")
                }
            } else {
                val methodBodyTypeResolver = MethodBodyTypeResolver(body, returnType)

                methodBodyTypeResolver.resolve(environment, context, binding)
            }

            context.add(funcType)
        } finally {
            // Garbage collect method parameter types
            context.removeAll(parameterBindings)
        }
    }
}

class MethodBodyTypeResolver(private val block: BlockNode, private val returnType: Type) : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding) {
        val returnStatements = block.search(ReturnStatementNode::class.java)

        returnStatements.forEach {
            val varExpr = it.valueNode.expressionNode
            val varType = TypeInferenceUtil.infer(context, varExpr)

            if (!NominalEquality(returnType, varType).satisfied()) {
                throw Exception("Method '${binding.simpleName}' declares a return type of '${returnType.name}', found '${varType.name}'")
            }
        }
    }
}