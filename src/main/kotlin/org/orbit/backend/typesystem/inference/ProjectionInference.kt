package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.ProjectionNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

private sealed interface SignatureVerificationResult {
    data class Implemented(val signatures: List<IType.Signature>) : SignatureVerificationResult
    data class NotImplemented(val reasons: List<String>) : SignatureVerificationResult {
        constructor(reason: String) : this(listOf(reason))
    }

    operator fun plus(other: SignatureVerificationResult) : SignatureVerificationResult = when (this) {
        is Implemented -> when (other) {
            is Implemented -> Implemented(signatures + other.signatures)
            is NotImplemented -> other
        }

        is NotImplemented -> when (other) {
            is Implemented -> this
            is NotImplemented -> NotImplemented(reasons + other.reasons)
        }
    }
}

object ProjectionInference : ITypeInference<ProjectionNode, GlobalEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun verifySignature(env: IMutableTypeEnvironment, expected: IType.Signature, provided: List<IType.Signature>) : SignatureVerificationResult {
        val implementations = provided.filter { TypeUtils.checkSignatures(env, it, expected) }

        if (implementations.isEmpty()) {
            val tag = printer.apply("Missing required Method:", PrintableKey.Error)
            return SignatureVerificationResult.NotImplemented("$tag `$expected`")
        }

        if (implementations.count() > 1) {
            val tag = printer.apply("Multiple implementations found for Method:", PrintableKey.Error)
            val prettyImpls = implementations.joinToString("\n\t")

            return SignatureVerificationResult.NotImplemented("$tag `$expected`\n\t$prettyImpls")
        }

        return SignatureVerificationResult.Implemented(implementations)
    }

    @Suppress("NAME_SHADOWING")
    override fun infer(node: ProjectionNode, env: GlobalEnvironment): AnyType {
        val nEnv = when (val n = node.context) {
            null -> env
            else -> ContextualTypeEnvironment(env, TypeInferenceUtils.inferAs(n, env))
        }

        val projectedType = TypeInferenceUtils.infer(node.typeIdentifier, nEnv)
        val projectedTrait = TypeInferenceUtils.inferAs<TypeExpressionNode, IType.Trait>(node.traitIdentifier, nEnv)
        val projection = Projection(projectedType, projectedTrait)
        val mEnv = ProjectionEnvironment(nEnv, projection)

        env.add(projection, projectedType)

        val oEnv = SelfTypeEnvironment(mEnv, projectedType)

        val bodyTypes = TypeInferenceUtils.inferAll(node.body, oEnv)
        val signatures = bodyTypes.filterIsInstance<IType.Signature>()

        val signatureResults = projectedTrait.signatures.fold(SignatureVerificationResult.Implemented(emptyList()) as SignatureVerificationResult) { acc, next ->
            acc + verifySignature(nEnv, next, signatures)
        }

        if (signatureResults is SignatureVerificationResult.NotImplemented) {
            val projection = Projection(projectedType, projectedTrait)
            val header = "Projection `$projection` is incomplete for the following reasons:"
            val errors = signatureResults.reasons.joinToString("\n\t")

            throw invocation.make<TypeSystem>("$header\n\t$errors", node)
        }

        for (signature in (signatureResults as SignatureVerificationResult.Implemented).signatures) {
            env.add(signature)
        }

        val signatureNames = signatures.map { it.name }
        val missing = projectedTrait.signatures.filterNot { it.name in signatureNames }

        if (missing.isNotEmpty()) {
            val pretty = missing.joinToString("\n") { it.prettyPrint(1) }

            throw invocation.make<TypeSystem>("Incomplete Projection `${projectedType.prettyPrint()}` : `${projectedTrait.prettyPrint()}`. Missing the following expected Methods:\n$pretty", node)
        }

        return projectedType
    }
}