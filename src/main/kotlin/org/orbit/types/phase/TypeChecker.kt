package org.orbit.types.phase

import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.core.phase.AdaptablePhase
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Scope
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.*
import org.orbit.types.typeresolvers.*
import org.orbit.util.Invocation
import org.orbit.util.dispose
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

class TypeChecker(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<NameResolverResult, Context>() {
    override val inputType: Class<NameResolverResult> = NameResolverResult::class.java
    override val outputType: Class<Context> = Context::class.java

    @Suppress("CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION")
    override fun execute(input: NameResolverResult): Context {
        val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast

        val moduleNodes = ast.search(ModuleNode::class.java)
        val apiNodes = ast.search(ApiDefNode::class.java)
        val typeDefNodes = ast.search(TypeDefNode::class.java)
        val traitDefNodes = ast.search(TraitDefNode::class.java)
        val typeAliasNodes = ast.search(TypeAliasNode::class.java)
        val methodDefNodes = ast.search(MethodDefNode::class.java)

        typeDefNodes.forEach {
            context.add(Type(it.getPath(), isRequired = it.isRequired))
        }

        traitDefNodes.forEach {
            context.add(Trait(it.getPath()))
        }

        // NOTE - After several failed attempts, it looks like Kotlin's type system
        //  might be too limited to allow us to encapsulate the following, duplicated logic
        //  Specifically, it would be nice to pass non-default constructors around.
        //  I would like to be able to do this in Orbit.
        // Extra credit to anyone who can crack it in Kotlin (its not important, just cool)!

        input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Module))
            .filterNodes(moduleNodes)
            .map(::ModuleTypeResolver)
            .map(partial(ModuleTypeResolver::resolve, input.environment, context))
            .forEach(context::add)

        input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Api))
            .filterNodes(apiNodes)
            .map(::ApiTypeResolver)
            .map(partial(ApiTypeResolver::resolve, input.environment, context))
            .forEach(context::add)

        input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Type))
            .filterNodes(typeDefNodes)
            .map(::TypeDefTypeResolver)
            .map(partial(TypeDefTypeResolver::resolve, input.environment, context))
            .forEach(context::add)

        input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Trait))
            .filterNodes(traitDefNodes)
            .map(::TraitDefTypeResolver)
            .map(partial(TraitDefTypeResolver::resolve, input.environment, context))
            .forEach(context::add)

        input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.TypeAlias))
            .filterNodes(typeAliasNodes)
            .map(::TypeAliasTypeResolver)
            .map(partial(TypeAliasTypeResolver::resolve, input.environment, context))
            .forEach(context::add)

        val m = input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Method))
            .filterNodes(methodDefNodes) { n, b ->
                n.signature.getPathOrNull() == b.path
            }

        m.map { Pair(it.first.signature, it.second) }
            .map(::MethodSignatureTypeResolver)
            .map(partial(MethodSignatureTypeResolver::resolve, input.environment, context))
            .forEach {
                context.bind(it.toString(OrbitMangler), it)
            }

        input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Trait))
            .filterNodes(traitDefNodes)
            .map(::TraitSignaturesTypeResolver)
            .map(partial(TraitSignaturesTypeResolver::resolve, input.environment, context))
            .forEach(context::add)

        // Now that Type, Trait & Method definitions are type resolved, we need to enforce explicit Trait Conformance
        input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Type))
            .filterNodes(typeDefNodes)
            .map(::TraitConformanceTypeResolver)
            .map(partial(TraitConformanceTypeResolver::resolve, input.environment, context))
            .forEach(context::add)

        // Ensure all modules that declare Api conformance(s) fulfill their contracts
        // TODO - This should probably be an separate phase
        input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Module))
            .filterNodes(moduleNodes)
            .map(::ApiConformanceTypeResolver)
            .forEach(dispose(partial(ApiConformanceTypeResolver::resolve, input.environment, context)))

        val allTypes = input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Type))
            .filterNodes(typeDefNodes)
            .map { it.first.getType()}

        val allTraits = input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Trait))
            .filterNodes(traitDefNodes)
            .mapNotNull { it.first.getType() as? Trait }

        val traitMethods = input.environment.scopes
            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Method))
            .filterNodes(methodDefNodes)
            .filter {
                val signature = it.first.signature.getType() as? SignatureProtocol<*>
                    ?: return@filter false

                when (signature) {
                    is InstanceSignature -> signature.receiver.type in allTraits
                    else -> signature.receiver in allTraits
                }
            }

        // We need to generate specialised copies of any method declared with a Trait receiver
        for (method in traitMethods) {
            val signature = method.first.signature.getType() as? SignatureProtocol<*>
                ?: continue

            when (signature) {
                is InstanceSignature -> {
                    val receiverType = signature.receiver.type

                    val conformingTypes = allTypes.filter {
                        (receiverType.equalitySemantics as AnyEquality).isSatisfied(context, receiverType, it)
                    }

                    for (ct in conformingTypes) {
                        val specialisedSignature = InstanceSignature(signature.name, Parameter(signature.receiver.name, ct), listOf(Parameter("self", ct)) + signature.parameters.subList(1, signature.parameters.size), signature.returnType)

                        context.bind(specialisedSignature.toString(OrbitMangler), specialisedSignature)
                    }
                }

                else -> {

                }
            }
        }

        m.map(::MethodTypeResolver)
            .forEach(dispose(partial(MethodTypeResolver::resolve, input.environment, context)))

        invocation.storeResult(this::class.java.simpleName, context)

        return context
    }
}
