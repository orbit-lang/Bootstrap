package org.orbit.types.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.core.phase.AdaptablePhase
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Scope
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.*
import org.orbit.types.typeresolvers.*
import org.orbit.util.*

class Stack<T> {
    private val storage = mutableListOf<T>()
    val size: Int
        get() = storage.count()

    fun push(item: T) {
        storage.add(item)
    }

    fun insert(item: T, index: Int) {
        storage.add(index, item)
    }

    fun pop() : T = storage.removeLast()
    fun peek() : T? = storage.lastOrNull()
    fun isEmpty() = storage.isEmpty()
    fun isNotEmpty() = storage.isNotEmpty()
}

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

interface TypeAction {
    fun execute(context: Context)
    fun describe(printer: Printer) : String
}

interface CreateStub<N: Node, T: TypeProtocol> : TypeAction {
    val node: N
    val constructor: (N) -> T

    override fun execute(context: Context) {
        context.add(constructor(node))
    }

    override fun describe(printer: Printer): String
        = "Create stub ${printer.apply(node.getPath().toString(OrbitMangler), PrintableKey.Bold)}"
}

class CreateModuleStub(override val node: ModuleNode) : CreateStub<ModuleNode, Module> {
    override val constructor: (ModuleNode) -> Module = ::Module
}

class CreateTypeStub(override val node: TypeDefNode) : CreateStub<TypeDefNode, Type> {
    override val constructor: (TypeDefNode) -> Type = ::Type
}

class CreateTraitStub(override val node: TraitDefNode) : CreateStub<TraitDefNode, Trait> {
    override val constructor: (TraitDefNode) -> Trait = ::Trait
}

class ResolveEntityProperties<N: EntityDefNode, E: Entity>(private val node: N) : TypeAction {
    private var properties: List<Property> = emptyList()

    override fun execute(context: Context) {
        val stub = context.getTypeByPath(node.getPath())
        val propertyTypes = node.propertyPairs.map {
            val pType = context.getTypeByPath(it.typeExpressionNode.getPath())

            Property(it.identifierNode.identifier, pType)
        }

        properties = propertyTypes

        val nType = when (node) {
            is TypeDefNode -> Type(stub.name, properties = propertyTypes)
            is TraitDefNode -> Trait(stub.name, properties = propertyTypes)
            else -> TODO("Unreachable")
        }

        // Update the type definition
        context.remove(stub.name)
        context.add(nType)
    }

    override fun describe(printer: Printer): String {
        return """
            Resolve properties for entity ${node.getPath().toString(printer)}
                    (${properties.joinToString(", ", transform = { it.toString(printer) })})
        """.trimIndent()
    }
}

class ResolverTraitConformance(private val node: TypeDefNode) : TypeAction {
    private var type: Type? = null
    private var traits: List<Trait> = emptyList()

    override fun execute(context: Context) {
        type = context.getTypeByPath(node.getPath()) as Type
        traits = node.traitConformances.map { context.getTypeByPath(it.getPath()) as Trait }

        val nType = Type(node.getPath(), type!!.typeParameters, type!!.properties, traits, type!!.equalitySemantics, false)

        context.remove(type!!.name)
        context.add(nType)
    }

    override fun describe(printer: Printer): String {
        return "Resolving trait conformance for type ${type!!.name}:\n\t\t(${traits!!.joinToString(", ", transform = { it.toString(printer) })})"
    }
}

class CreateMethodSignature(private val node: MethodSignatureNode, private val moduleNode: ModuleNode) : TypeAction, KoinComponent {
    private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)
    private var result: SignatureProtocol<*>? = null

    override fun execute(context: Context) {
        val module = context.getTypeByPath(moduleNode.getPath()) as Module
        val typeResolver = MethodSignatureTypeResolver(node, Binding.Self, null)
        result = typeResolver.resolve(nameResolverResult.environment, context)

        context.bind(node.getPath().toString(OrbitMangler), result!!)

        val nModule = Module(module.name, entities = module.entities, signatures = module.signatures + result!!)

        context.remove(nModule.name)
        context.add(nModule)
    }

    override fun describe(printer: Printer): String {
        return "Create method signature type\n\t\t${result!!.toString(printer)}"
    }
}

class FinaliseModule(private val node: ModuleNode) : TypeAction {
    private var ownedTypes = emptyList<Entity>()
    private var result: Module? = null

    override fun execute(context: Context) {
        val module = context.getTypeByPath(node.getPath()) as Module

        ownedTypes = context.types
            .filterIsInstance<Entity>()
            .map { OrbitMangler.unmangle(it.name) }
            .filter { node.getPath().isAncestor(it) }
            .map { context.getTypeByPath(it) as Entity }

        result = Module(node.getPath(), entities = ownedTypes, signatures = module.signatures)

        context.remove(module.name)
        context.add(result!!)
    }

    override fun describe(printer: Printer): String {
        return """
            |Finalise module type ${node.getPath().toString(printer)}
            |${result!!.toString(printer)}
        """.trimMargin()
    }
}

class TypeAssistant(private val context: Context) : KoinComponent {
    private val printer: Printer by inject()
    private val actions = mutableListOf<TypeAction>()

    fun perform(action: TypeAction) {
        actions.add(action)
        action.execute(context)
    }

    fun dump() : String {
        return """
            |${printer.apply("Type Assistant:", PrintableKey.Italics, PrintableKey.Bold)}
            |    ${actions.joinToString("\n\t", transform = { it.describe(printer) })}
        """.trimMargin()
    }
}

class TypeInitialisation(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<NameResolverResult, Context>() {
    override val inputType: Class<NameResolverResult> = NameResolverResult::class.java
    override val outputType: Class<Context> = Context::class.java

    private val typeAssistant = TypeAssistant(context)

    private fun <N: Node, T: TypeProtocol> createStubs(nodes: List<N>, stubConstructor: (N) -> CreateStub<N, T>) {
        nodes.map(stubConstructor)
            .forEach(typeAssistant::perform)
    }

    private fun <N: EntityDefNode, E: Entity> resolveEntityProperties(nodes: List<N>) {
        nodes.map { ResolveEntityProperties<N, E>(it) }
            .forEach(typeAssistant::perform)
    }

    private fun resolveTraitConformance(nodes: List<TypeDefNode>) {
        nodes.map(::ResolverTraitConformance)
            .forEach(typeAssistant::perform)
    }

    private fun createMethodSignatures(nodes: List<ModuleNode>) {
        for (node in nodes) {
            val signatures = node.search(MethodSignatureNode::class.java)

            for (sig in signatures) {
                val typeAction = CreateMethodSignature(sig, node)
                typeAssistant.perform(typeAction)
            }
        }
    }

    private fun finaliseModules(nodes: List<ModuleNode>) {
        nodes.map(::FinaliseModule)
            .forEach(typeAssistant::perform)
    }

    override fun execute(input: NameResolverResult) : Context {
        val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast

        // Start by creating type "stubs" for all modules
        val moduleDefs = ast.search(ModuleNode::class.java)

        createStubs(moduleDefs, ::CreateModuleStub)

        // Next, create "stubs" for all types & traits
        val typeDefs = ast.search(TypeDefNode::class.java)
        val traitDefs = ast.search(TraitDefNode::class.java)

        createStubs(typeDefs, ::CreateTypeStub)
        createStubs(traitDefs, ::CreateTraitStub)

        // We now have enough information to resolve the types of properties for each type & trait
        resolveEntityProperties<TypeDefNode, Type>(typeDefs)
        resolveEntityProperties<TraitDefNode, Trait>(traitDefs)

        resolveTraitConformance(typeDefs)

        createMethodSignatures(moduleDefs)
        finaliseModules(moduleDefs)

        invocation.storeResult(CompilationSchemeEntry.typeInitialisation, context)
        invocation.storeResult("__type_assistant__", typeAssistant)

        return context
    }
}

sealed class TraitPropertyResult : Semigroup<TraitPropertyResult> {
    object None : TraitPropertyResult()
    data class Exists(val property: Property) : TraitPropertyResult()
    data class Missing(val type: Type, val trait: Trait, val property: Property) : TraitPropertyResult()
    data class Duplicate(val type: Type, val property: Property) : TraitPropertyResult()
    data class SuccessGroup(val properties: List<Property>) : TraitPropertyResult()
    data class FailureGroup(val results: List<TraitPropertyResult>) : TraitPropertyResult()

    // NOTE - Exists results get erased if you try to add them to a failure case because they will never be used
    //  if something is wrong with Trait conformance for this type
    override fun plus(other: TraitPropertyResult): TraitPropertyResult = when {
        this is None && other is None -> None
        other is None -> this
        this is None -> other

        this is Duplicate && other is Duplicate -> when (this) {
            other -> this
            else -> FailureGroup(listOf(this, other))
        }

        this is Exists && other is Exists -> SuccessGroup(listOf(this.property, other.property))
        this is Exists -> FailureGroup(listOf(other))

        this is SuccessGroup && other is Exists -> SuccessGroup(this.properties + other.property)
        this is SuccessGroup -> other

        this is FailureGroup && other is Exists -> this
        this is FailureGroup -> FailureGroup(this.results + other)

        else -> FailureGroup(listOf(this, other))
    }
}

class TraitEnforcer : AdaptablePhase<Context, Context>(), KoinComponent {
    override val inputType: Class<Context> = Context::class.java
    override val outputType: Class<Context> = Context::class.java

    override val invocation: Invocation by inject()

    private fun mapResult(type: Type, pair: Pair<Trait, Property>) : TraitPropertyResult {
        val matches = type.properties.filter { it.name == pair.second.name }

        return when (matches.count()) {
            0 -> TraitPropertyResult.Missing(type, pair.first, pair.second)
            1 -> TraitPropertyResult.Exists(pair.second)
            else -> TraitPropertyResult.Duplicate(type, pair.second)
        }
    }

    private fun enforce(type: Type, module: Module) {
        // Get the superset of distinct pairs of Trait0.properties x TraitN.properties
        val allProperties = type.traitConformance
            .flatPairMap(Trait::properties)

        val cartesianProperties = allProperties
            .cartesian()
            .filter { it.first != it.second }

        // Ensure there are no conflicting property definitions across multiple Traits
        for (pair in cartesianProperties) {
            // TODO - For now, a conflict is any two distinct properties with the same name
            // NOTE - This is a different error than implementing a required property more than once for a single type!
            //  This is specifically if 2 or more Traits declare identical properties, e.g.:
            //      trait A(x Int)
            //      trait B(x Int)
//            if (pair.first.second.name == pair.second.second.name) {
//                throw invocation.error<TraitEnforcer>(DuplicateTraitProperty(type, pair.first.first, pair.second.first, pair.first.second, pair.second.second))
//            }
        }

        val propertiesResult = allProperties
            .map(partialReverse(::mapResult, type))
            .fold(TraitPropertyResult.None)

        when (propertiesResult) {
            is TraitPropertyResult.FailureGroup, is TraitPropertyResult.Missing, is TraitPropertyResult.Duplicate ->
                throw invocation.error<TraitEnforcer>(TraitEnforcerPropertyErrors(propertiesResult))

            else -> {}
        }
    }

    private fun enforceAll(module: Module) {
        /**
         * The rules here are simple for now; if a Type A declares conformance to a Trait B, then:
         *      1. A's set of declared properties must contain AT LEAST all of those declared by B; and
         *      2. A must implement all methods declared in B
         */
        module.entities.filterIsInstance<Type>()
            .forEach { enforce(it, module) }
    }

    override fun execute(input: Context): Context {
        input.types.filterIsInstance<Module>()
            .forEach(::enforceAll)

        return input
    }
}

//class TypeChecker(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<NameResolverResult, Context>() {
//    override val inputType: Class<NameResolverResult> = NameResolverResult::class.java
//    override val outputType: Class<Context> = Context::class.java
//
//    @Suppress("CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION")
//    override fun execute(input: NameResolverResult): Context {
//        val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast
//
//        val moduleNodes = ast.search(ModuleNode::class.java)
//        val apiNodes = ast.search(ApiDefNode::class.java)
//        val typeDefNodes = ast.search(TypeDefNode::class.java)
//        val traitDefNodes = ast.search(TraitDefNode::class.java)
//        val typeAliasNodes = ast.search(TypeAliasNode::class.java)
//        val typeConstructorNodes = ast.search(TypeConstructorNode::class.java)
//        val traitConstructorNodes = ast.search(TraitConstructorNode::class.java)
//        val methodDefNodes = ast.search(MethodDefNode::class.java)
//        val typeProjectionNodes = ast.search(TypeProjectionNode::class.java)
//
//        val nodeStack = Stack<Node>()
//
//        typeDefNodes.forEach(nodeStack::push)
//        traitDefNodes.forEach(nodeStack::push)
//        moduleNodes.forEach(nodeStack::push)
//        apiNodes.forEach(nodeStack::push)
//        typeAliasNodes.forEach(nodeStack::push)
//        typeConstructorNodes.forEach(nodeStack::push)
//        traitConstructorNodes.forEach(nodeStack::push)
//        typeProjectionNodes.forEach(nodeStack::push)
//        methodDefNodes.forEach(nodeStack::push)
//
//        typeDefNodes.forEach {
//            context.add(Type(it.getPath(), isRequired = it.isRequired))
//        }
//
//        traitDefNodes.forEach {
//            context.add(Trait(it.getPath()))
//        }
//
//        val maxIterations = nodeStack.size
//        var iterations = 0
//        while (nodeStack.isNotEmpty()) {
//            if (iterations > maxIterations && nodeStack.size >= maxIterations) {
//                // We've iterated through every node once and not made any progress, something is wrong
//                throw RuntimeException("Cyclic dependency found in type checker")
//            }
//
//            val node = nodeStack.pop()
//
//            val result = when (node) {
//                is ModuleNode -> typeCheck(node, moduleNodes, Binding.Kind.Module, input.environment, context)
//                is ApiDefNode -> typeCheck(node, apiNodes, Binding.Kind.Api, input.environment, context)
//                is TypeDefNode -> typeCheck(node, typeDefNodes, Binding.Kind.Type, input.environment, context)
//                is TraitDefNode -> typeCheck(node, traitDefNodes, Binding.Kind.Trait, input.environment, context)
//                is TypeAliasNode -> typeCheck(node, typeAliasNodes, Binding.Kind.TypeAlias, input.environment, context)
//                is TypeConstructorNode -> typeCheck(node, typeConstructorNodes, Binding.Kind.TypeConstructor, input.environment, context)
//                is TraitConstructorNode -> typeCheck(node, traitConstructorNodes, Binding.Kind.TraitConstructor, input.environment, context)
//                is MethodDefNode -> typeCheck(node, methodDefNodes, Binding.Kind.Method, input.environment, context)
//                is TypeProjectionNode -> typeCheck(node, typeProjectionNodes, Binding.Kind.TypeProjection, input.environment, context)
//                else -> TODO("!!!")
//            }
//
//            if (result is Result.Failure) {
//                // This node gets demoted so that we have time to type check its dependencies
//                println("TYPE CHECKING FAILED: $node")
//                nodeStack.insert(node, 0)
//            }
//
//            iterations += 1
//        }
//
//        // NOTE - After several failed attempts, it looks like Kotlin's type system
//        //  might be too limited to allow us to encapsulate the following, duplicated logic
//        //  Specifically, it would be nice to pass non-default constructors around.
//        //  I would like to be able to do this in Orbit.
//        // Extra credit to anyone who can crack it in Kotlin (its not important, just cool)!
//
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Module))
////            .filterNodes(moduleNodes)
////            .map(::ModuleTypeResolver)
////            .map(partial(ModuleTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
////
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Api))
////            .filterNodes(apiNodes)
////            .map(::ApiTypeResolver)
////            .map(partial(ApiTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
////
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Type))
////            .filterNodes(typeDefNodes)
////            .map(::TypeDefTypeResolver)
////            .map(partial(TypeDefTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
////
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Trait))
////            .filterNodes(traitDefNodes)
////            .map(::TraitDefTypeResolver)
////            .map(partial(TraitDefTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
////
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.TypeAlias))
////            .filterNodes(typeAliasNodes)
////            .map(::TypeAliasTypeResolver)
////            .map(partial(TypeAliasTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
////
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.TypeConstructor))
////            .filterNodes(typeConstructorNodes)
////            .map(::TypeConstructorTypeResolver)
////            .map(partial(TypeConstructorTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
////
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.TraitConstructor))
////            .filterNodes(traitConstructorNodes)
////            .map(::TraitConstructorTypeResolver)
////            .map(partial(TraitConstructorTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
////
////        val m = input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Method))
////            .filterNodes(methodDefNodes) { n, b ->
////                n.signature.getPathOrNull() == b.path
////            }
////
////        m.map { Pair(it.first.signature, it.second) }
////            .map(::MethodSignatureTypeResolver)
////            .map(partial(MethodSignatureTypeResolver::resolve, input.environment, context))
////            .forEach {
////                context.bind(it.toString(OrbitMangler), it)
////            }
////
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Trait))
////            .filterNodes(traitDefNodes)
////            .map(::TraitSignaturesTypeResolver)
////            .map(partial(TraitSignaturesTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
////
////        // Now that Type, Trait & Method definitions are type resolved, we need to enforce explicit Trait Conformance
////        input.environment.scopes
////            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Type))
////            .filterNodes(typeDefNodes)
////            .map(::TraitConformanceTypeResolver)
////            .map(partial(TraitConformanceTypeResolver::resolve, input.environment, context))
////            .forEach(context::add)
//
//        // Ensure all modules that declare Api conformance(s) fulfill their contracts
//        // TODO - This should probably be an separate phase
//        input.environment.scopes
//            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Module))
//            .filterNodes(moduleNodes)
//            .map(::ApiConformanceTypeResolver)
//            .forEach(dispose(partial(ApiConformanceTypeResolver::resolve, input.environment, context)))
//
//        val allTypes = input.environment.scopes
//            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Type))
//            .filterNodes(typeDefNodes)
//            .map { it.first.getType() }
//
//        val allTraits = input.environment.scopes
//            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Trait))
//            .filterNodes(traitDefNodes)
//            .mapNotNull { it.first.getType() as? Trait }
//
//        val traitMethods = input.environment.scopes
//            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Method))
//            .filterNodes(methodDefNodes)
//            .filter {
//                val signature = it.first.signature.getType() as? SignatureProtocol<*>
//                    ?: return@filter false
//
//                when (signature) {
//                    is InstanceSignature -> signature.receiver.type in allTraits
//                    else -> signature.receiver in allTraits
//                }
//            }
//
//        // We need to generate specialised copies of any method declared with a Trait receiver
//        for (method in traitMethods) {
//            val signature = method.first.signature.getType() as? SignatureProtocol<*>
//                ?: continue
//
//            when (signature) {
//                is InstanceSignature -> {
//                    val receiverType = signature.receiver.type
//
//                    val conformingTypes = allTypes.filter {
//                        (receiverType.equalitySemantics as AnyEquality).isSatisfied(context, receiverType, it)
//                    }
//
//                    for (ct in conformingTypes) {
//                        val specialisedSignature = InstanceSignature(signature.name, Parameter(signature.receiver.name, ct), listOf(Parameter("self", ct)) + signature.parameters.subList(1, signature.parameters.size), signature.returnType)
//
//                        context.bind(specialisedSignature.toString(OrbitMangler), specialisedSignature)
//                    }
//                }
//
//                else -> {
//
//                }
//            }
//        }
//
//        val m = input.environment.scopes
//            .flatMap(partial(Scope::getBindingsByKind, Binding.Kind.Method))
//            .filterNodes(methodDefNodes) { n, b ->
//                (n as MethodDefNode).signature.getPathOrNull() == b.path
//            }
//
//        m.map(::MethodTypeResolver)
//            .forEach(dispose(partial(MethodTypeResolver::resolve, input.environment, context)))
//
//        invocation.storeResult(this::class.java.simpleName, context)
//
//        return context
//    }
//
//    private fun <N: Node> typeCheck(node: N, allNodes: List<N>, kind: Binding.Kind, environment: Environment, context: Context) : Result<TypeProtocol, N> {
//        try {
//            when (node) {
//                is TypeProjectionNode -> {
//                    val resolver = TypeProjectionTypeResolver(node, Binding(Binding.Kind.TypeProjection, node.getPath().relativeNames.last(), node.getPath()))
//                    val typeProjection = resolver.resolve(environment, context)
//
//                    context.add(typeProjection)
//
//                    return Result.Success(typeProjection)
//                }
//
//                is ModuleNode -> {
//                    val binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.kind == kind && it.path == node.getPath() }
//
//                    val typeResolver = ModuleTypeResolver(node, binding)
//                    val type = typeResolver.resolve(environment, context)
//
//                    context.add(type)
//
//                    return Result.Success(type)
//                }
//
//                is ApiDefNode -> {
//                    val binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.kind == kind && it.path == node.getPath() }
//
//                    val typeResolver = ApiTypeResolver(node, binding)
//                    val type = typeResolver.resolve(environment, context)
//
//                    context.add(type)
//
//                    return Result.Success(type)
//                }
//
//                is TypeDefNode -> {
//                    var binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.path == node.getPath() }
//
//                    val typeResolver = TypeDefTypeResolver(node, binding)
//                    val type = typeResolver.resolve(environment, context)
//
//                    context.add(type)
//
//                    val conformanceResolver = TraitConformanceTypeResolver(node, binding)
//                    val conf = conformanceResolver.resolve(environment, context)
//
//                    context.add(conf)
//
//                    return Result.Success(conf)
//                }
//
//                is TraitDefNode -> {
//                    var binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.kind == kind && it.path == node.getPath() }
//
//                    val typeResolver = TraitDefTypeResolver(node, binding)
//                    var type = typeResolver.resolve(environment, context)
//
//                    context.add(type)
//
//                    binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.kind == kind && it.path == node.getPath() }
//
//                    val sigResolver = TraitSignaturesTypeResolver(node, binding)
//                    type = sigResolver.resolve(environment, context)
//
//                    context.add(type)
//
//                    return Result.Success(type)
//                }
//
//                is TypeAliasNode -> {
//                    val binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.path == node.getPath() }
//
//                    val typeResolver = TypeAliasTypeResolver(node, binding)
//                    val type = typeResolver.resolve(environment, context)
//
//                    context.add(type)
//
//                    return Result.Success(type)
//                }
//
//                is TypeConstructorNode -> {
//                    val binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.kind == kind && it.path == node.getPath() }
//
//                    val typeResolver = TypeConstructorTypeResolver(node, binding)
//                    val type = typeResolver.resolve(environment, context)
//
//                    context.add(type)
//
//                    return Result.Success(type)
//                }
//
//                is TraitConstructorNode -> {
//                    val binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.kind == kind && it.path == node.getPath() }
//
//                    val typeResolver = TraitConstructorTypeResolver(node, binding)
//                    val type = typeResolver.resolve(environment, context)
//
//                    context.add(type)
//
//                    return Result.Success(type)
//                }
//
//                is MethodDefNode -> {
//                    val binding = environment.scopes.flatMap { it.bindings }
//                        .first { it.kind == kind && it.path == node.getPath() }
//
//                    val typeResolver = MethodSignatureTypeResolver(node.signature, binding)
//                    val type = typeResolver.resolve(environment, context)
//
//                    context.bind(binding.path.toString(OrbitMangler), type)
//
//                    return Result.Success(type)
//                }
//
//                else -> TODO("???")
//            }
//        } catch (_: MissingTypeException) {
//            return Result.Failure(node)
//        } catch (_: NoSuchElementException) {
//            println("HERE: $node")
//        }
//
//        TODO("???")
//    }
//}
