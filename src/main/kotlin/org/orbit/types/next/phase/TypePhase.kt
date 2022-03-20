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
import org.orbit.util.containsInstances
import org.orbit.util.next.ITypeMapRead

interface TypePhase<N: Node, T: IType> : Phase<TypePhaseData<N>, T> {
    fun run(input: TypePhaseData<N>) : T

    override fun execute(input: TypePhaseData<N>): T = run(input).apply {
        input.inferenceUtil.set(input.node, this, true)
    }
}

fun <N: Node, T: IType> TypePhase<N, T>.executeAll(inferenceUtil: InferenceUtil, nodes: List<N>) : List<T>
    = nodes.map { execute(TypePhaseData(inferenceUtil, it)) }

object TraitConformancePhase : TypePhase<TypeDefNode, Type>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeDefNode>): Type {
        val type = input.inferenceUtil.inferAs<TypeDefNode, Type>(input.node)
        // TODO - evaluate type expressions
        val traits = input.inferenceUtil.inferAllAs<TypeExpressionNode, Trait>(input.node.traitConformances)

        traits.forEach { input.inferenceUtil.addConformance(type, it) }

        return type
    }
}

object TraitConformanceVerification : TypePhase<TypeDefNode, Type>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<TypeDefNode>): Type {
        val type = input.inferenceUtil.inferAs<TypeDefNode, Type>(input.node)
        val traits = input.inferenceUtil.getConformance(type)

        if (traits.isEmpty()) return type

        val ctx = input.inferenceUtil.toCtx()
        val start: ContractResult = ContractResult.None
        val result = traits.fold(start) { acc, next ->
            acc + (next.isImplemented(ctx, type))
        }

        return when (result) {
            is ContractResult.None, is ContractResult.Success -> type
            is ContractResult.Failure -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, type), input.node)
            is ContractResult.Group -> when (result.results.containsInstances<ContractResult.Failure>()) {
                true -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, type), input.node)
                else -> type
            }
        }
    }
}

object ModulePhase : TypePhase<ModuleNode, Module>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<ModuleNode>): Module {
        val typeDefs = input.node.search<TypeDefNode>()
        val traitDefs = input.node.search<TraitDefNode>()

        var types = TypeStubPhase.executeAll(input.inferenceUtil, typeDefs)
        var traits = TraitStubPhase.executeAll(input.inferenceUtil, traitDefs)

        types = TypeFieldsPhase.executeAll(input.inferenceUtil, typeDefs)
        traits = TraitContractsPhase.executeAll(input.inferenceUtil, traitDefs)

        types = TraitConformancePhase.executeAll(input.inferenceUtil, typeDefs)
        types = TraitConformanceVerification.executeAll(input.inferenceUtil, typeDefs)

        return Module(input.node.getPath()).apply {
            extendAll(types + traits)
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

    init {
        registerAdapter(NameResolverAdapter)
    }

    override fun execute(input: TypePhaseData<ProgramNode>): Result {
        val moduleNodes = input.node.search(ModuleNode::class.java)
        val modules = ModulePhase.executeAll(input.inferenceUtil, moduleNodes)
        val result = Result(modules, input.inferenceUtil.getTypeMap())

        invocation.storeResult(CompilationSchemeEntry.typeSystem, result)
//        invocation.storeResult("__type_assistant__", typeAssistant)

        return result
    }
}
