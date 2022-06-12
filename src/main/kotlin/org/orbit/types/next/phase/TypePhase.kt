package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.nodes.*
import org.orbit.core.phase.AdaptablePhase
import org.orbit.core.phase.Phase
import org.orbit.core.phase.PhaseAdapter
import org.orbit.core.phase.getInputType
import org.orbit.core.storeResult
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.next.ITypeMapRead

interface TypePhase<N: Node, T: TypeComponent> : Phase<TypePhaseData<N>, T> {
    fun run(input: TypePhaseData<N>) : T

    override fun execute(input: TypePhaseData<N>): T = run(input).apply {
        input.inferenceUtil.set(input.node, this, true)
    }
}

fun <N: Node, T: TypeComponent> TypePhase<N, T>.executeAll(inferenceUtil: InferenceUtil, nodes: List<N>) : List<T>
    = nodes.map { execute(TypePhaseData(inferenceUtil, it)) }

object OperatorDeclarationPhase : TypePhase<OperatorDefNode, Operator>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun runInfix(input: TypePhaseData<OperatorDefNode>) : InfixOperator {
        val methodRef = input.inferenceUtil.inferAs<MethodReferenceNode, Func>(input.node.methodReferenceNode)

        if (methodRef.takes.count() != 2) {
            throw invocation.make<TypeSystem>("Infix Operators accept exactly 2 parameters, found ${methodRef.takes.count()} via reference to method ${methodRef.toString(printer)}", input.node.methodReferenceNode)
        }

        val lhs = methodRef.takes.nth(0)
        val rhs = methodRef.takes.nth(1)

        return InfixOperator(input.node.identifierNode.identifier, input.node.symbol, lhs, rhs, methodRef.returns)
    }

    override fun run(input: TypePhaseData<OperatorDefNode>): Operator = when (input.node.fixity) {
        OperatorFixity.Infix -> runInfix(input)
        else -> TODO("Unsupported Operator Fixity: ${input.node.fixity}")
    }
}

object ModulePhase : TypePhase<ModuleNode, Module>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<ModuleNode>): Module {
        val typeDefs = input.node.entityDefs.filterIsInstance<TypeDefNode>()
        val traitDefs = input.node.entityDefs.filterIsInstance<TraitDefNode>()
        val typeAliasDefs = input.node.typeAliasNodes
        val typeConstructorDefs = input.node.entityConstructors.filterIsInstance<TypeConstructorNode>()
        val traitConstructorDefs = input.node.entityConstructors.filterIsInstance<TraitConstructorNode>()
        val typeProjections = input.node.projections
        val extensions = input.node.extensions
        val methodDefs = input.node.methodDefs
        val familyDefs = input.node.entityDefs.filterIsInstance<FamilyNode>()
        val familyConstructorDefs = input.node.entityConstructors.filterIsInstance<FamilyConstructorNode>()
        val contexts = input.node.contexts
        val opDefs = input.node.operatorDefs

        var types: List<IType> = TypeStubPhase.executeAll(input.inferenceUtil, typeDefs)
        var traits: List<ITrait> = TraitStubPhase.executeAll(input.inferenceUtil, traitDefs)
        var families = FamilyPhase.executeAll(input.inferenceUtil, familyDefs)

        TypeConstructorStubPhase.executeAll(input.inferenceUtil, typeConstructorDefs)
        TraitConstructorStubPhase.executeAll(input.inferenceUtil, traitConstructorDefs)
        FamilyConstructorStubPhase.executeAll(input.inferenceUtil, familyConstructorDefs)

        ContextPhase.executeAll(input.inferenceUtil, contexts)

        TypeConstructorFieldsPhase.executeAll(input.inferenceUtil, typeConstructorDefs)
        TraitConstructorSignaturesPhase.executeAll(input.inferenceUtil, traitConstructorDefs)

        FamilyConstructorExpansionPhase.executeAll(input.inferenceUtil, familyConstructorDefs)

        types = TraitConformancePhase.executeAll(input.inferenceUtil, typeDefs)
        types = TypeFieldsPhase.executeAll(input.inferenceUtil, typeDefs)
        traits = TraitContractsPhase.executeAll(input.inferenceUtil, traitDefs)

        TypeConstructorConformancePhase.executeAll(input.inferenceUtil, typeConstructorDefs)

        TypeAliasPhase.executeAll(input.inferenceUtil, typeAliasDefs)

        ExtensionStubPhase.executeAll(input.inferenceUtil, extensions)

        ContextAwarePhase.executeAll(input.inferenceUtil, typeConstructorDefs)
        ContextAwarePhase.executeAll(input.inferenceUtil, traitConstructorDefs)
        ContextAwarePhase.executeAll(input.inferenceUtil, extensions)

        families = FamilyExpansionPhase.executeAll(input.inferenceUtil, familyDefs)

        TypeConstructorConstraintsPhase.executeAll(input.inferenceUtil, typeConstructorDefs)
        TraitConstructorConstraintsPhase.executeAll(input.inferenceUtil, traitConstructorDefs)

        var methods = MethodStubPhase.executeAll(input.inferenceUtil, methodDefs)

        ProjectionPhase.executeAll(input.inferenceUtil, typeProjections)
        ExtensionPhase.executeAll(input.inferenceUtil, extensions)

        types = TraitConformanceVerification.executeAll(input.inferenceUtil, typeDefs)

        OperatorDeclarationPhase.executeAll(input.inferenceUtil, opDefs)
        MethodBodyPhase.executeAll(input.inferenceUtil, methodDefs)

        return Module(input.node.getPath()).apply {
            extendAll(types + traits + families)
        }
    }
}

object TypeSystem : AdaptablePhase<TypePhaseData<ProgramNode>, TypeSystem.Result>(), KoinComponent {
    private object NameResolverAdapter : PhaseAdapter<NameResolverResult, TypePhaseData<ProgramNode>>, KoinComponent {
        private val inferenceUtil: InferenceUtil by inject()

        override fun bridge(output: NameResolverResult): TypePhaseData<ProgramNode> {
            return TypePhaseData(inferenceUtil, output.environment.ast as ProgramNode)
        }
    }

    data class Result(val modules: List<Module>, val typeMap: ITypeMapRead)

    override val inputType: Class<TypePhaseData<ProgramNode>> = getInputType()
    override val outputType: Class<Result> = Result::class.java

    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    init {
        registerAdapter(NameResolverAdapter)
    }

    override fun execute(input: TypePhaseData<ProgramNode>): Result {
        val moduleNodes = input.node.search(ModuleNode::class.java)
        val modules = ModulePhase.executeAll(input.inferenceUtil, moduleNodes)
        val result = Result(modules, input.inferenceUtil.getTypeMap())
        val typeErrors = input.inferenceUtil.getTypeErrors()

        if (typeErrors.isNotEmpty()) {
            val fullMessage = typeErrors.joinToString("\n") { it.message }

            throw invocation.make<TypeSystem>(fullMessage, input.node)
        }

        invocation.storeResult(CompilationSchemeEntry.typeSystem, result)
//          invocation.storeResult("__type_assistant__", typeAssistant)

        return result
    }
}
