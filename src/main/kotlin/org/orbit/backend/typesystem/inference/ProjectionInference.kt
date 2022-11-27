package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.backend.typesystem.utils.toSignature
import org.orbit.core.nodes.MethodDelegateNode
import org.orbit.core.nodes.ProjectedPropertyAssignmentNode
import org.orbit.core.nodes.ProjectionNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import kotlin.math.exp

private sealed interface TraitMemberVerificationResult<M: IType.Trait.Member> {
    data class Implemented<M: IType.Trait.Member>(val members: List<M>) : TraitMemberVerificationResult<M>
    data class NotImplemented<M: IType.Trait.Member>(val reasons: List<String>) : TraitMemberVerificationResult<M> {
        constructor(reason: String) : this(listOf(reason))
    }

    operator fun plus(other: TraitMemberVerificationResult<M>) : TraitMemberVerificationResult<M> = when (this) {
        is Implemented -> when (other) {
            is Implemented -> Implemented(members + other.members)
            is NotImplemented -> other
        }

        is NotImplemented -> when (other) {
            is Implemented -> this
            is NotImplemented -> NotImplemented(reasons + other.reasons)
        }
    }
}

object ProjectionInference : ITypeInference<ProjectionNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun verifyProperty(env: ITypeEnvironment, expected: IType.Property, provided: List<IType.Property>) : TraitMemberVerificationResult<IType.Property> {
        val implementations = provided.filter { TypeUtils.checkProperties(env, it, expected) }

        if (implementations.isEmpty()) {
            val tag = printer.apply("Missing required Property:", PrintableKey.Error)
            return TraitMemberVerificationResult.NotImplemented("$tag `$expected`")
        }

        if (implementations.count() > 1) {
            val tag = printer.apply("Multiple implementations found for Property:", PrintableKey.Error)
            val prettyImpls = implementations.joinToString("\n\t")

            return TraitMemberVerificationResult.NotImplemented("$tag `$expected\n\t$prettyImpls`")
        }

        return TraitMemberVerificationResult.Implemented(implementations)
    }

    private fun verifySignature(env: IMutableTypeEnvironment, expected: IType.Signature, provided: List<IType.Signature>) : TraitMemberVerificationResult<IType.Signature> {
        val implementations = provided.filter { TypeUtils.checkSignatures(env, it, expected) }

        if (implementations.isEmpty()) {
            val tag = printer.apply("Missing required Method:", PrintableKey.Error)
            return TraitMemberVerificationResult.NotImplemented("$tag `$expected`")
        }

        if (implementations.count() > 1) {
            val tag = printer.apply("Multiple implementations found for Method:", PrintableKey.Error)
            val prettyImpls = implementations.joinToString("\n\t")

            return TraitMemberVerificationResult.NotImplemented("$tag `$expected`\n\t$prettyImpls")
        }

        return TraitMemberVerificationResult.Implemented(implementations)
    }

    @Suppress("NAME_SHADOWING")
    override fun infer(node: ProjectionNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = when (val n = node.context) {
            null -> env
            else -> ContextualTypeEnvironment(env, TypeInferenceUtils.inferAs(n, env))
        }

        val projectedType = TypeInferenceUtils.infer(node.typeIdentifier, nEnv)
        val projectedTrait = TypeInferenceUtils.inferAs<TypeExpressionNode, IType.Trait>(node.traitIdentifier, nEnv)
        val projection = Projection(projectedType, projectedTrait)
        val mEnv = ProjectionEnvironment(nEnv, projection)

        env.add(projection, projectedType)

        val flat = projectedType.flatten(projectedType, mEnv)

        if (flat is IType.Union) {
            for (constructor in flat.unionConstructors) {
                env.add(Projection(constructor, projection.target), constructor)
            }
        } else if (flat is IType.Struct) {
            env.add(Projection(flat, projection.target), flat)
        }

        val signatureNodes = node.body.filterIsInstance<MethodDelegateNode>()
        val propertyNodes = node.body.filterIsInstance<ProjectedPropertyAssignmentNode>()

        val properties = propertyNodes.map {
            TypeInferenceUtils.inferAs<ProjectedPropertyAssignmentNode, IType.Property>(it, mEnv)
        } + when (flat) {
            is IType.Struct -> flat.getProperties()
            else -> emptyList()
        }

        val propertyResults = projectedTrait.properties.fold(TraitMemberVerificationResult.Implemented<IType.Property>(emptyList()) as TraitMemberVerificationResult<IType.Property>) { acc, next ->
            acc + verifyProperty(mEnv, next, properties)
        }

        if (propertyResults is TraitMemberVerificationResult.NotImplemented) {
            val projection = Projection(projectedType, projectedTrait)
            val header = "Projection `$projection` is incomplete for the following reasons:"
            val errors = propertyResults.reasons.joinToString("\n\t")

            throw invocation.make<TypeSystem>("$header\n\t$errors", node)
        }

        val signatures = signatureNodes.mapNotNull {
            val node = it
            when (val type = TypeInferenceUtils.infer(node, mEnv)) {
                is AnyArrow -> type.toSignature(projectedType, node.methodName.identifier)
                else -> null
            }
        }

        val signatureResults = projectedTrait.signatures.fold(TraitMemberVerificationResult.Implemented<IType.Signature>(emptyList()) as TraitMemberVerificationResult<IType.Signature>) { acc, next ->
            acc + verifySignature(mEnv, next, signatures)
        }

        if (signatureResults is TraitMemberVerificationResult.NotImplemented) {
            val projection = Projection(projectedType, projectedTrait)
            val header = "Projection `$projection` is incomplete for the following reasons:"
            val errors = signatureResults.reasons.joinToString("\n\t")

            throw invocation.make<TypeSystem>("$header\n\t$errors", node)
        }

        for (signature in (signatureResults as TraitMemberVerificationResult.Implemented).members) {
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