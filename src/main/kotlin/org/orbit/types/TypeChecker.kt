package org.orbit.types

import org.orbit.core.AdaptablePhase
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.getPathOrNull
import org.orbit.core.nodes.MethodDefNode
import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.graph.exportTypes
import org.orbit.util.Invocation

interface TypeResolver {
    fun resolve(environment: Environment, context: Context, binding: Binding)
}

class TypeChecker(override val invocation: Invocation) : AdaptablePhase<Environment, Context>() {
    override val inputType: Class<Environment> = Environment::class.java
    override val outputType: Class<Context> = Context::class.java

    override fun execute(input: Environment): Context {
        val context = Context()

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

            argTypes.add(t)
        }

        if (argTypes.isEmpty()) {
            argTypes.add(Entity("Unit"))
        }

        val returnType = if (signature.returnTypeNode == null) {
            Entity("Unit")
        } else {
            Entity(signature.returnTypeNode.getPath().toString(OrbitMangler))
        }

        context.add(Function(argTypes, returnType))
    }
}