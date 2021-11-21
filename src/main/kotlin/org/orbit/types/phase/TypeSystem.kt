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
        return target.name == input.name
    }
}

data class StructuralEqualityConstraint(override val target: Trait) : EqualityConstraint<TypeProtocol> {
    private fun checkConformance(universe: ContextProtocol, input: Entity) : Boolean {
        val traitPropertyConstraints = target.buildPropertyConstraints()

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

data class SignatureConstraint(override val target: SignatureProtocol<*>) : Constraint<SignatureProtocol<*>, Entity> {
    override fun checkConformance(universe: ContextProtocol, input: Entity): Boolean = universe.universe
        .asSequence()
        .filterIsInstance<TypeSignature>()
        .filter { it.isReceiverSatisfied(target.receiver as Entity, universe) }
        .filter { it.isParameterListSatisfied(target.parameters, universe) }
        .filter { it.isReturnTypeSatisfied(target.returnType as Entity, universe) }
        .count() == 1
}

class TypeSystem(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<NameResolverResult, Context>() {
    override val inputType: Class<NameResolverResult> = NameResolverResult::class.java
    override val outputType: Class<Context> = Context::class.java

    private val typeAssistant = TypeAssistant(context)

    private fun <N: Node, T: TypeAction> performTypeAction(nodes: List<N>, typeActionGenerator: (N) -> T) {
        nodes.map(typeActionGenerator)
            .forEach(typeAssistant::perform)
    }

    private fun <N: EntityConstructorNode, C: EntityConstructor> resolveEntityConstructorParameters(nodes: List<N>, generator: (String, List<TypeParameter>) -> C)
        = performTypeAction(nodes) { ResolveEntityConstructorTypeParameters<N, C>(it) { s, tps, _ -> generator(s, tps) } }

    private fun <N: EntityConstructorNode, C: EntityConstructor> resolveEntityConstructorProperties(nodes: List<N>, generator: (String, List<TypeParameter>, List<Property>, List<PartiallyResolvedTraitConstructor>) -> C)
        = performTypeAction(nodes) { ResolveEntityConstructorProperties(it, generator) }

    private fun refineEntityConstructorTypeParameters(nodes: List<EntityConstructorNode>)
        = performTypeAction(nodes, ::RefineEntityConstructorTypeParameters)

    private fun createMethodSignatures(nodes: List<ModuleNode>) {
        for (node in nodes) {
            val signatures = node.search(MethodDefNode::class.java)
                .map { it.signature }

            for (sig in signatures) {
                val typeAction = CreateMethodSignature(sig, node)
                typeAssistant.perform(typeAction)
            }
        }
    }

    private fun checkMethodReturnTypes(nodes: List<ModuleNode>) {
        for (node in nodes) {
            val methodNodes = node.search(MethodDefNode::class.java)

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

            resolveEntityConstructorParameters(typeConstructors) { s, tps -> TypeConstructor(s, tps) }
            resolveEntityConstructorParameters(traitConstructors) { s, tps -> TraitConstructor(s, tps) }

            resolveEntityConstructorProperties(typeConstructors, ::TypeConstructor)
            resolveEntityConstructorProperties(traitConstructors, ::TraitConstructor)

            performTypeAction(typeConstructors, ::ResolveTypeConstructorTraitConformance)

            // We now have enough information to resolve the types of properties for each type & trait
            performTypeAction(typeDefs) { ResolveEntityProperties<TypeDefNode, Type>(it) }
            performTypeAction(traitDefs) { ResolveEntityProperties<TraitDefNode, Trait>(it) }

            performTypeAction(traitDefs, ::ResolveTraitSignatures)

            performTypeAction(typeAliases, ::CreateTypeAlias)
            assembleTypeProjections(typeProjections)
            refineEntityConstructorTypeParameters(typeConstructors)

            performTypeAction(typeDefs, ::ResolveTraitConformance)

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

