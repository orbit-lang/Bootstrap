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
import org.orbit.util.partial
import org.orbit.util.partialReverse
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

    private fun <N: Node, T: TypeAction> performTypeAction(nodes: List<N>, typeActionGenerator: (N) -> T) {
        nodes.map(typeActionGenerator)
            .forEach(typeAssistant::perform)
    }

    private fun <N: EntityConstructorNode, C: EntityConstructor> resolveEntityConstructorParameters(nodes: List<N>, generator: (String, List<TypeParameter>, List<TypeSignature>) -> C)
        = performTypeAction(nodes) { ResolveEntityConstructorTypeParameters<N, C>(it) { s, tps, _, sigs -> generator(s, tps, sigs) } }

    private fun <N: EntityConstructorNode, C: EntityConstructor> resolveEntityConstructorProperties(nodes: List<N>, generator: (String, List<TypeParameter>, List<Property>, List<PartiallyResolvedTraitConstructor>, List<TypeSignature>) -> C)
        = performTypeAction(nodes) { ResolveEntityConstructorProperties(it, generator) }

    private fun refineEntityConstructorTypeParameters(nodes: List<EntityConstructorNode>)
        = performTypeAction(nodes, ::RefineEntityConstructorTypeParameters)

    private fun createMethodSignatures(nodes: List<ModuleNode>) {
        for (node in nodes) {
            // NOTE - We search this way to avoid capturing nested method defs inside extensions
            val signatures = node.methodDefs
                .map { it.signature }

            for (sig in signatures) {
                val typeAction = CreateMethodSignature(sig, node)
                typeAssistant.perform(typeAction)
            }
        }
    }

    private fun checkMethodReturnTypes(nodes: List<ModuleNode>) {
        for (node in nodes) {
            val methodNodes = node.methodDefs

            methodNodes.map(::MethodReturnTypeCheck)
                .forEach(typeAssistant::perform)
        }
    }

    private fun assembleTypeProjections(nodes: List<TypeProjectionNode>) {
        nodes.map(::TypeProjectionAssembler)
            .forEach(typeAssistant::perform)
    }

    private fun finaliseModules(nodes: List<ModuleNode>) {
        nodes.map(::FinaliseModule)
            .forEach(typeAssistant::perform)
    }

    @ExperimentalContracts
    @ExperimentalTime
    override fun execute(input: NameResolverResult) : Context {
        val timed = measureTimeWithResult {
            val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast

            // Start by creating type "stubs" for all modules
            val moduleDefs = ast.search(ModuleNode::class.java)

            performTypeAction(moduleDefs, ::CreateModuleStub)

            // Next, create "stubs" for all types & traits
            val typeDefs = ast.search(TypeDefNode::class.java)
            val traitDefs = ast.search(TraitDefNode::class.java)
            val typeProjections = ast.search(TypeProjectionNode::class.java)
            val typeAliases = ast.search(TypeAliasNode::class.java)
            val typeConstructors = ast.search(TypeConstructorNode::class.java)
            val traitConstructors = ast.search(TraitConstructorNode::class.java)

            performTypeAction(typeDefs, ::CreateTypeStub)
            performTypeAction(traitDefs, ::CreateTraitStub)
            performTypeAction(typeConstructors, ::CreateTypeConstructorStub)
            performTypeAction(traitConstructors, ::CreateTraitConstructorStub)

            performTypeAction(traitConstructors, ::ResolveTraitConstructorSignatures)

            resolveEntityConstructorParameters(typeConstructors) { s, tps, _ -> TypeConstructor(s, tps, emptyList()) }
            resolveEntityConstructorParameters(traitConstructors) { s, tps, sigs -> TraitConstructor(s, tps, signatures = sigs) }

            resolveEntityConstructorProperties(typeConstructors, ::TypeConstructor)
            resolveEntityConstructorProperties(traitConstructors, ::TraitConstructor)

            performTypeAction(typeConstructors, ::ResolveTypeConstructorTraitConformance)

            // We now have enough information to resolve the types of properties for each type & trait
            performTypeAction(typeDefs) { ResolveEntityProperties<TypeDefNode, Type>(it) }
            performTypeAction(traitDefs) { ResolveEntityProperties<TraitDefNode, Trait>(it) }

            performTypeAction(traitDefs, ::ResolveTraitSignatures)
            performTypeAction(typeAliases, ::CreateTypeAlias)

            for (module in moduleDefs) {
                val extensions = module.search<ExtensionNode>()

                for (ext in extensions) {
                    performTypeAction(extensions) { ExtendEntity(ext, module) }
                }
            }

            assembleTypeProjections(typeProjections)
            refineEntityConstructorTypeParameters(typeConstructors)

            performTypeAction(typeDefs, ::ResolveTraitConformance)

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

            createMethodSignatures(moduleDefs)
            checkMethodReturnTypes(moduleDefs)

            finaliseModules(moduleDefs)

            invocation.storeResult(CompilationSchemeEntry.typeSystem, context)
            invocation.storeResult("__type_assistant__", typeAssistant)

            return@measureTimeWithResult context
        }

        println("Completed Type checking in ${timed.first}")

        return timed.second
    }
}

