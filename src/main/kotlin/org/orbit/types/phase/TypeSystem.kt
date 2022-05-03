package org.orbit.types.phase

import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getResult
import org.orbit.core.nodes.*
import org.orbit.core.phase.AdaptablePhase
import org.orbit.core.storeResult
import org.orbit.frontend.phase.Parser
import org.orbit.graph.phase.NameResolverResult
import org.orbit.graph.phase.measureTimeWithResult
import org.orbit.types.components.*
import org.orbit.types.typeactions.*
import org.orbit.types.util.TypeAssistant
import org.orbit.util.Invocation
import kotlin.contracts.ExperimentalContracts
import kotlin.time.ExperimentalTime

interface Kind {
    operator fun invoke(x: Kind) : Kind
}

sealed class IntrinsicKinds<Kx: Kind, Ky: Kind> : Kind {
    object Anything : IntrinsicKinds<Nothing, Anything>() {
        override fun invoke(x: Kind): Kind = Anything
    }

    object Empty : IntrinsicKinds<Nothing, Nothing>() {
        override fun invoke(x: Kind): Kind = Empty
    }

    data class TypeInstance(val name: String) : IntrinsicKinds<Empty, Nothing>() {
        override fun invoke(x: Kind): Kind = Empty
    }

    data class Type(val name: String) : IntrinsicKinds<Empty, TypeInstance>() {
        override fun invoke(x: Kind): Kind = TypeInstance(name)
    }

    data class TypeConstructor(val name: String) : IntrinsicKinds<TypeInstance, Type>() {
        override fun invoke(x: Kind): Kind = Type(name + "::" + (x as TypeInstance).name)
    }
}

interface Constraint<Domain: TypeProtocol, Codomain: TypeProtocol> {
    val target: Domain

    fun checkConformance(universe: ContextProtocol, input: Codomain) : Boolean
}

interface EqualityConstraint<Domain: TypeProtocol> : Constraint<Domain, Domain>

data class NominalEqualityConstraint(override val target: TypeProtocol) : EqualityConstraint<TypeProtocol> {
    override fun checkConformance(universe: ContextProtocol, input: TypeProtocol): Boolean {
        val lType = universe.refresh(target, true)
        val rType = universe.refresh(input, true)

        return lType.name == rType.name
    }
}

data class StructuralEqualityConstraint(override val target: Trait) : EqualityConstraint<TypeProtocol> {
    private fun checkConformance(universe: ContextProtocol, input: Entity) : Boolean {
        val traitPropertyConstraints = target.buildPropertyConstraints()

        // TODO - move typeParameters into Entity
        if (target.typeParameters.isNotEmpty() && input is Type) {
            if (input.typeParameters.count() < target.typeParameters.count()) {
                throw Exception("TODO - NOT ENOUGH TYPE PARAMETERS")
            }

            // The input type must satisfy all of the target's type parameters
            var count = 0
            for (typeParameter1 in target.typeParameters) {
                for (typeParameter2 in input.typeParameters) {
                    val equalityConstraint = AnyEqualityConstraint(typeParameter1)

                    if (equalityConstraint.checkConformance(universe, typeParameter2)) {
                        count += 1
                    } else {
                        println("HERE")
                    }
                }
            }

            if (count != target.typeParameters.count()) {
                throw Exception("TODO - Type ${input.name} does not satisfy Trait ${target.name}'s required type parameters")
            }
        }

        var count = 0
        for (constraint in traitPropertyConstraints) {
            if (constraint.checkConformance(universe, input)) {
                count += 1
            }
        }

        return count == target.properties.count()
    }

    override fun checkConformance(universe: ContextProtocol, input: TypeProtocol): Boolean = when (input) {
        is Entity -> checkConformance(universe, input)
        else -> false
    }
}

data class AnyEqualityConstraint(override val target: TypeProtocol) : EqualityConstraint<TypeProtocol> {
    override fun checkConformance(universe: ContextProtocol, input: TypeProtocol): Boolean = when (target) {
        is Trait -> StructuralEqualityConstraint(target).checkConformance(universe, input)
        else -> NominalEqualityConstraint(target).checkConformance(universe, input)
    }
}

data class PropertyConstraint(override val target: Property) : Constraint<Property, Entity> {
    override fun checkConformance(universe: ContextProtocol, input: Entity): Boolean {
        val eq = when (target.type) {
            is Trait -> StructuralEqualityConstraint(target.type)
            else -> NominalEqualityConstraint(target.type)
        }

        return input.properties.any {
            it.name == target.name && eq.checkConformance(universe, it.type)
        }
    }
}

data class SignatureConstraint(private val trait: Trait, override val target: SignatureProtocol<*>) : Constraint<SignatureProtocol<*>, Entity> {
    override fun checkConformance(universe: ContextProtocol, input: Entity): Boolean = (universe as Context).withSubContext { ctx ->
        if (trait.traitConstructor != null) {
            trait.typeParameters.zip(trait.traitConstructor.typeParameters).forEach {
                ctx.add(TypeAlias(it.second.name, it.first as Type))
            }
        }

        return@withSubContext ctx.types.asSequence()
            .filterIsInstance<Module>()
            .flatMap { it.signatures }
            .filter { it.name == target.name }
            .filter { it.isReceiverSatisfied(target.receiver as Entity, ctx) }
            .filter { it.isParameterListSatisfied(target.parameters, ctx) }
            .filter { it.isReturnTypeSatisfied(target.returnType as Entity, ctx) }
            .count() == 1
    }
}

class TypeSystem(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<NameResolverResult, Context>() {
    override val inputType: Class<NameResolverResult> = NameResolverResult::class.java
    override val outputType: Class<Context> = Context::class.java

    private val typeAssistant = TypeAssistant(context)

    private fun <N: Node, T: TypeAction> performTypeAction(assistant: TypeAssistant = typeAssistant, nodes: List<N>, typeActionGenerator: (N) -> T) {
        nodes.map(typeActionGenerator)
            .forEach(assistant::perform)
    }

    private fun <N: EntityConstructorNode, C: EntityConstructor> resolveEntityConstructorParameters(assistant: TypeAssistant = typeAssistant, nodes: List<N>, generator: (String, List<TypeParameter>, List<TypeSignature>) -> C)
        = performTypeAction(assistant, nodes) { ResolveEntityConstructorTypeParameters<N, C>(it) { s, tps, _, sigs -> generator(s, tps, sigs) } }

    private fun <N: EntityConstructorNode, C: EntityConstructor> resolveEntityConstructorProperties(assistant: TypeAssistant = typeAssistant, nodes: List<N>, generator: (String, List<TypeParameter>, List<Property>, List<PartiallyResolvedTraitConstructor>, List<TypeSignature>) -> C)
        = performTypeAction(assistant, nodes) { ResolveEntityConstructorProperties(it, generator) }

    private fun refineEntityConstructorTypeParameters(assistant: TypeAssistant = typeAssistant, nodes: List<EntityConstructorNode>)
        = performTypeAction(assistant, nodes, ::RefineEntityConstructorTypeParameters)

    private fun createMethodSignatures(assistant: TypeAssistant = typeAssistant, nodes: List<ModuleNode>) {
        for (node in nodes) {
            // NOTE - We search this way to avoid capturing nested method defs inside extensions
            val signatures = node.methodDefs
                .map { it.signature }

            for (sig in signatures) {
                val typeAction = CreateMethodSignature(sig, node)
                assistant.perform(typeAction)
            }
        }
    }

    private fun checkMethodReturnTypes(assistant: TypeAssistant = typeAssistant, nodes: List<ModuleNode>) {
        for (node in nodes) {
            val methodNodes = node.methodDefs

            methodNodes.map(::MethodReturnTypeCheck)
                .forEach(assistant::perform)
        }
    }

    private fun assembleTypeProjections(assistant: TypeAssistant = typeAssistant, nodes: List<TypeProjectionNode>) {
        nodes.map(::TypeProjectionAssembler)
            .forEach(assistant::perform)
    }

    private fun finaliseModules(assistant: TypeAssistant = typeAssistant, nodes: List<ModuleNode>) {
        nodes.map(::FinaliseModule)
            .forEach(assistant::perform)
    }

    private fun processModule(moduleNode: ModuleNode) {
        val nContext = Context(context)
        val assistant = TypeAssistant(nContext)

        performTypeAction(assistant, listOf(moduleNode), ::CreateModuleStub)

        val typeDefs = moduleNode.entityDefs.filterIsInstance<TypeDefNode>()
        val traitDefs = moduleNode.entityDefs.filterIsInstance<TraitDefNode>()
        val typeProjections = moduleNode.typeProjections
        val typeAliases = moduleNode.typeAliasNodes
        val typeConstructors = moduleNode.entityConstructors.filterIsInstance<TypeConstructorNode>()
        val traitConstructors = moduleNode.entityConstructors.filterIsInstance<TraitConstructorNode>()
        val typeFamilies = moduleNode.entityDefs.filterIsInstance<FamilyNode>()

        performTypeAction(assistant, typeDefs, ::CreateTypeStub)
        performTypeAction(assistant, traitDefs, ::CreateTraitStub)
        performTypeAction(assistant, typeConstructors, ::CreateTypeConstructorStub)
        performTypeAction(assistant, traitConstructors, ::CreateTraitConstructorStub)
        performTypeAction(assistant, traitConstructors, ::ResolveTraitConstructorSignatures)

        resolveEntityConstructorParameters(assistant, typeConstructors) { s, tps, _ -> TypeConstructor(s, tps, emptyList()) }
        resolveEntityConstructorParameters(assistant, traitConstructors) { s, tps, sigs -> TraitConstructor(s, tps, signatures = sigs) }

        resolveEntityConstructorProperties(assistant, typeConstructors, ::TypeConstructor)
        resolveEntityConstructorProperties(assistant, traitConstructors, ::TraitConstructor)

        performTypeAction(assistant, typeConstructors, ::ResolveTypeConstructorTraitConformance)

        // We now have enough information to resolve the types of properties for each type & trait
        performTypeAction(assistant, typeDefs) { ResolveEntityProperties<TypeDefNode, Type>(it) }
        performTypeAction(assistant, traitDefs) { ResolveEntityProperties<TraitDefNode, Trait>(it) }

        performTypeAction(assistant, traitDefs, ::ResolveTraitSignatures)
        performTypeAction(assistant, typeAliases, ::CreateTypeAlias)

        val extensions = moduleNode.extensions

        for (ext in extensions) {
            performTypeAction(assistant, extensions) { ExtendEntity(ext, moduleNode) }
        }

        assembleTypeProjections(assistant, typeProjections)
        refineEntityConstructorTypeParameters(assistant, typeConstructors)

        performTypeAction(assistant, typeDefs, ::ResolveTraitConformance)

        for (mono in nContext.monomorphisedTypes.values) {
            val assembler = AssembleMonoExtensions(mono, moduleNode)
            assembler.execute(context)
        }

        for (td in typeDefs) {
            val assembler = AssembleExtensions(td, moduleNode)

            assembler.execute(context)
        }

        createMethodSignatures(assistant, listOf(moduleNode))
        checkMethodReturnTypes(assistant, listOf(moduleNode))

        finaliseModules(assistant, listOf(moduleNode))
    }

    @ExperimentalContracts
    @ExperimentalTime
    override fun execute(input: NameResolverResult) : Context {
        invocation.storeResult(CompilationSchemeEntry.typeSystem, context)

        val timed = measureTimeWithResult {
            val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast

            // Start by creating type "stubs" for all modules
            val moduleDefs = ast.search(ModuleNode::class.java)

            performTypeAction(typeAssistant, moduleDefs, ::CreateModuleStub)

            // Next, create "stubs" for all types & traits
            val typeDefs = ast.search(TypeDefNode::class.java)
            val traitDefs = ast.search(TraitDefNode::class.java)
            val typeProjections = ast.search(TypeProjectionNode::class.java)
            val typeAliases = ast.search(TypeAliasNode::class.java)
            val typeConstructors = ast.search(TypeConstructorNode::class.java)
            val traitConstructors = ast.search(TraitConstructorNode::class.java)

            performTypeAction(typeAssistant, typeDefs, ::CreateTypeStub)
            performTypeAction(typeAssistant, traitDefs, ::CreateTraitStub)
            performTypeAction(typeAssistant, typeConstructors, ::CreateTypeConstructorStub)
            performTypeAction(typeAssistant, traitConstructors, ::CreateTraitConstructorStub)

            performTypeAction(typeAssistant, traitConstructors, ::ResolveTraitConstructorSignatures)

            resolveEntityConstructorParameters(typeAssistant, typeConstructors) { s, tps, _ -> TypeConstructor(s, tps, emptyList()) }
            resolveEntityConstructorParameters(typeAssistant, traitConstructors) { s, tps, sigs -> TraitConstructor(s, tps, signatures = sigs) }

            resolveEntityConstructorProperties(typeAssistant, typeConstructors, ::TypeConstructor)
            resolveEntityConstructorProperties(typeAssistant, traitConstructors, ::TraitConstructor)

            performTypeAction(typeAssistant, typeConstructors, ::ResolveTypeConstructorTraitConformance)

            // We now have enough information to resolve the types of properties for each type & trait
            performTypeAction(typeAssistant, typeDefs) { ResolveEntityProperties<TypeDefNode, Type>(it) }
            performTypeAction(typeAssistant, traitDefs) { ResolveEntityProperties<TraitDefNode, Trait>(it) }

            performTypeAction(typeAssistant, traitDefs, ::ResolveTraitSignatures)
            performTypeAction(typeAssistant, typeAliases, ::CreateTypeAlias)

            for (module in moduleDefs) {
                val extensions = module.search<ExtensionNode>()

                for (ext in extensions) {
                    performTypeAction(typeAssistant, extensions) { ExtendEntity(ext, module) }
                }
            }

            assembleTypeProjections(typeAssistant, typeProjections)
            refineEntityConstructorTypeParameters(typeAssistant, typeConstructors)

            performTypeAction(typeAssistant, typeDefs, ::ResolveTraitConformance)

            for (module in moduleDefs) {
                for (mono in context.monomorphisedTypes.values) {
                    val assembler = AssembleMonoExtensions(mono, module)
                    assembler.execute(context)
                }

                val tds = module.search<TypeDefNode>()

                for (td in tds) {
                    val assembler = AssembleExtensions(td, module)

                    assembler.execute(context)
                }
            }

            createMethodSignatures(typeAssistant, moduleDefs)
            checkMethodReturnTypes(typeAssistant, moduleDefs)

            finaliseModules(typeAssistant, moduleDefs)

//            moduleDefs.forEach(::processModule)

            invocation.storeResult(CompilationSchemeEntry.typeSystem, context)
            invocation.storeResult("__type_assistant__", typeAssistant)

            return@measureTimeWithResult context
        }

        println("Completed Type checking in ${timed.first}")

        return timed.second
    }
}

