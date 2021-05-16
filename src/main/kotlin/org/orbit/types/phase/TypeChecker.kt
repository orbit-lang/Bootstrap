package org.orbit.types.phase

import org.orbit.core.OrbitMangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.getPathOrNull
import org.orbit.core.getResult
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.core.phase.AdaptablePhase
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Scope
import org.orbit.types.components.Context
import org.orbit.types.components.Trait
import org.orbit.types.components.Type
import org.orbit.types.components.TypeProtocol
import org.orbit.types.typeresolvers.MethodTypeResolver
import org.orbit.types.typeresolvers.TraitDefTypeResolver
import org.orbit.types.typeresolvers.TypeDefTypeResolver
import org.orbit.types.typeresolvers.TypeResolver
import org.orbit.util.Invocation
import org.orbit.util.partial

fun <K: Binding.Kind> Scope.getBindingsByKind(kind: K) : List<Binding> {
    return bindings.filter { it.kind == kind }
}

fun <N: Node> List<Binding>.filterNodes(nodes: List<N>, filter: ((N, Binding) -> Boolean)? = null) : List<Pair<N, Binding>> = mapNotNull {
    val fn: (N) -> Boolean = when (filter) {
        null -> { n -> n.getPath() == it.path }
        else -> partial(filter, it)
    }

    when (val n = nodes.find(fn)) {
        null -> null
        else -> Pair(n, it)
    }
}

class TypeChecker(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<Environment, Context>() {
    override val inputType: Class<Environment> = Environment::class.java
    override val outputType: Class<Context> = Context::class.java

    @Suppress("CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION")
    override fun execute(input: Environment): Context {
        val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast
        val typeDefNodes = ast.search(TypeDefNode::class.java)
        val traitDefNodes = ast.search(TraitDefNode::class.java)
        val methodDefNodes = ast.search(MethodDefNode::class.java)

        typeDefNodes.forEach {
            context.add(Type(it.getPath()))
        }

        traitDefNodes.forEach {
            context.add(Trait(it.getPath()))
        }

        // NOTE - After several failed attempts, it looks like Kotlin's type system
        //  might be too limited to allow us to encapsulate this duplicated logic
        //  Specifically, it would be nice to pass non-default constructors around.
        //  I would like to be able to do this in Orbit.
        // Extra credit to anyone who can crack it in Kotlin (its not important, just cool)!

        input.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Type))
            .filterNodes(typeDefNodes)
            .map(::TypeDefTypeResolver)
            .map(partial(TypeDefTypeResolver::resolve, input, context))
            .forEach(context::add)

        input.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Trait))
            .filterNodes(traitDefNodes)
            .map(::TraitDefTypeResolver)
            .map(partial(TraitDefTypeResolver::resolve, input, context))
            .forEach(context::add)

        input.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Method))
            .filterNodes(methodDefNodes) { n, b -> n.signature.getPathOrNull() == b.path }
            .map(::MethodTypeResolver)
            .map(partial(MethodTypeResolver::resolve, input, context))
            .forEach { context.bind(it.toString(OrbitMangler), it) }

        invocation.storeResult(this::class.java.simpleName, context)

        return context
    }
}
