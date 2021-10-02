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

class TypeSystem(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<NameResolverResult, Context>() {
    override val inputType: Class<NameResolverResult> = NameResolverResult::class.java
    override val outputType: Class<Context> = Context::class.java

    private val typeAssistant = TypeAssistant(context)

    private fun <N: Node, T: TypeProtocol> createStubs(nodes: List<N>, stubConstructor: (N) -> CreateStub<N, T>) {
        nodes.map(stubConstructor)
            .forEach(typeAssistant::perform)
    }

    private fun createTypeAliases(nodes: List<TypeAliasNode>) {
        nodes.map(::CreateTypeAlias)
            .forEach(typeAssistant::perform)
    }

    private fun <N: EntityDefNode, E: Entity> resolveEntityProperties(nodes: List<N>) {
        nodes.map { ResolveEntityProperties<N, E>(it) }
            .forEach(typeAssistant::perform)
    }

    private fun resolveTraitSignatures(nodes: List<TraitDefNode>) {
        nodes.map(::ResolveTraitSignatures)
            .forEach(typeAssistant::perform)
    }

    private fun resolveTraitConformance(nodes: List<TypeDefNode>) {
        nodes.map(::ResolveTraitConformance)
            .forEach(typeAssistant::perform)
    }

    private fun <N: EntityConstructorNode, C: EntityConstructor> resolveEntityConstructorParameters(nodes: List<N>, generator: (String, List<TypeParameter>) -> C) {
        nodes.map { ResolveEntityConstructorTypeParameters<N, C>(it, generator) }
            .forEach(typeAssistant::perform)
    }

    private fun <N: EntityConstructorNode, C: EntityConstructor> resolveEntityConstructorProperties(nodes: List<N>, generator: (String, List<TypeParameter>, List<Property>) -> C) {
        nodes.map { ResolveEntityConstructorProperties(it, generator) }
            .forEach(typeAssistant::perform)
    }

    private fun refineEntityConstructorTypeParameters(nodes: List<EntityConstructorNode>) {
        nodes.map(::RefineEntityConstructorTypeParameters)
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

            createStubs(moduleDefs, ::CreateModuleStub)

            // Next, create "stubs" for all types & traits
            val typeDefs = ast.search(TypeDefNode::class.java)
            val traitDefs = ast.search(TraitDefNode::class.java)
            val typeProjections = ast.search(TypeProjectionNode::class.java)
            val typeAliases = ast.search(TypeAliasNode::class.java)
            val typeConstructors = ast.search(TypeConstructorNode::class.java)
            val traitConstructors = ast.search(TraitConstructorNode::class.java)

            createStubs(typeDefs, ::CreateTypeStub)
            createStubs(traitDefs, ::CreateTraitStub)
            createStubs(typeConstructors, ::CreateTypeConstructorStub)
            createStubs(traitConstructors, ::CreateTraitConstructorStub)

            // We now have enough information to resolve the types of properties for each type & trait
            resolveEntityProperties<TypeDefNode, Type>(typeDefs)
            resolveEntityProperties<TraitDefNode, Trait>(traitDefs)
            resolveTraitSignatures(traitDefs)

            resolveEntityConstructorParameters(typeConstructors, ::TypeConstructor)
            resolveEntityConstructorProperties(typeConstructors, ::TypeConstructor)

            resolveEntityConstructorParameters(traitConstructors, ::TraitConstructor)
            resolveEntityConstructorProperties(traitConstructors, ::TraitConstructor)

            createTypeAliases(typeAliases)
            assembleTypeProjections(typeProjections)
            refineEntityConstructorTypeParameters(typeConstructors)

            resolveTraitConformance(typeDefs)

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

