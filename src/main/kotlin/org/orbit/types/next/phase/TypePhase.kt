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
import org.orbit.graph.phase.measureTimeWithResult
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnnotatedBlockInferenceContext
import org.orbit.types.next.inference.BlockInference
import org.orbit.types.next.inference.InferenceResult
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.next.ITypeMapRead
import kotlin.contracts.ExperimentalContracts
import kotlin.math.sign
import kotlin.time.ExperimentalTime

interface TypePhase<N: Node, T: TypeComponent> : Phase<TypePhaseData<N>, T> {
    fun run(input: TypePhaseData<N>) : T

    override fun execute(input: TypePhaseData<N>): T = run(input).apply {
        input.inferenceUtil.set(input.node, this, true)
    }
}

fun <N: Node, T: TypeComponent> TypePhase<N, T>.executeAll(inferenceUtil: InferenceUtil, nodes: List<N>) : List<T>
    = nodes.map { execute(TypePhaseData(inferenceUtil, it)) }

object MethodBodyPhase : TypePhase<MethodDefNode, TypeComponent>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun typeCheckGenericMethodBody(input: TypePhaseData<MethodDefNode>, signature: PolymorphicType<ISignature>) : TypeComponent {
        TODO("Generic Methods")
    }

    private fun typeCheckMethodBody(input: TypePhaseData<MethodDefNode>, signature: Signature) : TypeComponent {
        val nInferenceUtil = input.inferenceUtil.derive(self = signature.receiver)

        return when (val result = BlockInference.infer(nInferenceUtil, AnnotatedBlockInferenceContext(signature.returns), input.node.body)) {
            is InferenceResult.Success<*> -> result.type
            is InferenceResult.Failure -> result.never
        }
    }

    override fun run(input: TypePhaseData<MethodDefNode>): TypeComponent {
        val signature = input.inferenceUtil.inferAs<MethodSignatureNode, ISignature>(input.node.signature)

        return when (signature) {
            is PolymorphicType<*> -> typeCheckGenericMethodBody(input, signature as PolymorphicType<ISignature>)
            is Signature -> typeCheckMethodBody(input, signature)
            else -> Never("${signature.toString(printer)} is not a Signature")
        }
    }
}

object ModulePhase : TypePhase<ModuleNode, Module>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<ModuleNode>): Module {
        val typeDefs = input.node.search<TypeDefNode>()
        val traitDefs = input.node.search<TraitDefNode>()
        val typeAliasDefs = input.node.search<TypeAliasNode>()
        val typeConstructorDefs = input.node.search<TypeConstructorNode>()
        val traitConstructorDefs = input.node.search<TraitConstructorNode>()
        val typeProjections = input.node.search<TypeProjectionNode>()
        val methodDefs = input.node.search<MethodDefNode>()

        var types: List<IType> = TypeStubPhase.executeAll(input.inferenceUtil, typeDefs)
        var traits: List<ITrait> = TraitStubPhase.executeAll(input.inferenceUtil, traitDefs)

        TypeConstructorStubPhase.executeAll(input.inferenceUtil, typeConstructorDefs)
        TraitConstructorStubPhase.executeAll(input.inferenceUtil, traitConstructorDefs)

        types = TraitConformancePhase.executeAll(input.inferenceUtil, typeDefs)
        types = TypeFieldsPhase.executeAll(input.inferenceUtil, typeDefs)
        traits = TraitContractsPhase.executeAll(input.inferenceUtil, traitDefs)

        TypeConstructorConstraintsPhase.executeAll(input.inferenceUtil, typeConstructorDefs)
        TraitConstructorConstraintsPhase.executeAll(input.inferenceUtil, traitConstructorDefs)
        TypeAliasPhase.executeAll(input.inferenceUtil, typeAliasDefs)

        var methods = MethodStubPhase.executeAll(input.inferenceUtil, methodDefs)

        TypeProjectionPhase.executeAll(input.inferenceUtil, typeProjections)

        types = TraitConformanceVerification.executeAll(input.inferenceUtil, typeDefs)

        MethodBodyPhase.executeAll(input.inferenceUtil, methodDefs)

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

    @ExperimentalTime
    @ExperimentalContracts
    override fun execute(input: TypePhaseData<ProgramNode>): Result {
        val timedResult = measureTimeWithResult {
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

            result
        }

        println("Completed type checking in ${timedResult.first}")

        return timedResult.second
    }
}
