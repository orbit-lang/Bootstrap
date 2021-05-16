package org.orbit.types.phase

import org.orbit.core.AdaptablePhase
import org.orbit.core.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.getResult
import org.orbit.core.nodes.TypeDefNode
import org.orbit.frontend.Parser
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.Context
import org.orbit.types.components.Type
import org.orbit.types.typeresolvers.MethodTypeResolver
import org.orbit.types.typeresolvers.TypeDefTypeResolver
import org.orbit.util.Invocation

class TypeChecker(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<Environment, Context>() {
    override val inputType: Class<Environment> = Environment::class.java
    override val outputType: Class<Context> = Context::class.java

    override fun execute(input: Environment): Context {
        val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast
        val typeDefNodes = ast.search(TypeDefNode::class.java)

        typeDefNodes.forEach {
            context.add(Type(it.getPath()))
        }

        input.scopes
            .flatMap { it.bindings }
            .filter { it.kind is Binding.Kind.Entity }
            .forEach {
                val typeDefNode = typeDefNodes.find { n -> n.getPath() == it.path }
                    ?: TODO("@TypeChecker:31")

                val resolver = TypeDefTypeResolver(typeDefNode)

                resolver.resolve(input, context, it)
            }

        val methodResolver = MethodTypeResolver()
        val methods = input.scopes.flatMap { it.bindings.filter { b -> b.kind == Binding.Kind.Method } }

        methods.forEach { methodResolver.resolve(input, context, it) }

        invocation.storeResult(this::class.java.simpleName, context)

        return context
    }
}
