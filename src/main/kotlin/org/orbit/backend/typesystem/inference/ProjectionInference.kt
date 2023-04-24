package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.kinds.KindUtil
import org.orbit.backend.typesystem.components.utils.TraitImplementationUtil
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ProjectionNode
import org.orbit.util.Invocation
import org.orbit.util.Printer

object ProjectionInference : ITypeInference<ProjectionNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

//    private fun verifyProperty(env: ITypeEnvironment, expected: Property, provided: List<Property>) : TraitMemberVerificationResult<Property> {
//        val implementations = provided.filter { TypeUtils.checkProperties(env, it, expected) }
//
//        if (implementations.isEmpty()) {
//            val tag = printer.apply("Missing required Property:", PrintableKey.Error)
//            return TraitMemberVerificationResult.NotImplemented("$tag `$expected`")
//        }
//
//        if (implementations.count() > 1) {
//            val tag = printer.apply("Multiple implementations found for Property:", PrintableKey.Error)
//            val prettyImpls = implementations.joinToString("\n\t")
//
//            return TraitMemberVerificationResult.NotImplemented("$tag `$expected\n\t$prettyImpls`")
//        }
//
//        return TraitMemberVerificationResult.Implemented(implementations)
//    }
//
//    private fun verifySignature(env: IMutableTypeEnvironment, expected: Signature, provided: List<Signature>) : TraitMemberVerificationResult<Signature> {
//        val implementations = provided.filter { TypeUtils.checkSignatures(env, it, expected) }
//
//        if (implementations.isEmpty()) {
//            val tag = printer.apply("Missing required Method:", PrintableKey.Error)
//            return TraitMemberVerificationResult.NotImplemented("$tag `$expected`")
//        }
//
//        if (implementations.count() > 1) {
//            val tag = printer.apply("Multiple implementations found for Method:", PrintableKey.Error)
//            val prettyImpls = implementations.joinToString("\n\t")
//
//            return TraitMemberVerificationResult.NotImplemented("$tag `$expected`\n\t$prettyImpls")
//        }
//
//        return TraitMemberVerificationResult.Implemented(implementations)
//    }
//
//    private fun check(results: TraitMemberVerificationResult<out TraitMember>, node: ProjectionNode, projectedType: AnyType, projectedTrait: Trait) {
//        if (results is TraitMemberVerificationResult.NotImplemented) {
//            val projection = Projection(projectedType, projectedTrait)
//            val header = "Projection `$projection` is incomplete for the following reasons:"
//            val errors = results.reasons.joinToString("\n\t")
//
//            throw invocation.make<TypeSystem>("$header\n\t$errors", node)
//        }
//    }

    override fun infer(node: ProjectionNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = when (val n = node.context) {
            null -> env
            else -> ContextualTypeEnvironment(env, TypeInferenceUtils.inferAs(n, env))
        }

        val projectedType = TypeInferenceUtils.infer(node.typeIdentifier, nEnv)
        val pTrait = TypeInferenceUtils.infer(node.traitIdentifier, nEnv)
            .flatten(Always, nEnv)
        val generalisedTrait = pTrait as? Trait
            ?: throw invocation.make<TypeSystem>("Projecting conformance to non-Trait type $pTrait (Kind: ${KindUtil.getKind(pTrait, pTrait::class.java.simpleName,node)}) is currently unsupported", node.traitIdentifier)

        val selfAlias = TypeAlias("Self", generalisedTrait)
        val projectedTrait = generalisedTrait.substitute(selfAlias, projectedType) as Trait

        val projection = Projection(projectedType, projectedTrait)
        val mEnv = ProjectionEnvironment(nEnv, projection)

        env.add(projection, projectedType)

        when (val flat = projectedType.flatten(projectedType, mEnv)) {
            is Union -> {
                for (constructor in flat.unionConstructors) {
                    env.add(Projection(constructor, projection.target), constructor)
                }
            }

            is Struct -> {
                env.add(Projection(flat, projection.target), flat)
            }
        }

        val implUtil = TraitImplementationUtil(projectedTrait)

        return when (val result = implUtil.isImplemented(projectedType, mEnv)) {
            is TraitMemberVerificationResult.NotImplemented -> {
                val pretty = "Incomplete Projection $projectedType : $projectedTrait:\n\t$result"
                throw invocation.make<TypeSystem>(pretty, node)
            }

            else -> projectedType
        }
    }

//    @Suppress("NAME_SHADOWING")
//    override fun infer(node: ProjectionNode, env: IMutableTypeEnvironment): AnyType {
//        val nEnv = when (val n = node.context) {
//            null -> env
//            else -> ContextualTypeEnvironment(env, TypeInferenceUtils.inferAs(n, env))
//        }
//
//        val projectedType = TypeInferenceUtils.infer(node.typeIdentifier, nEnv)
//        val pTrait = TypeInferenceUtils.infer(node.traitIdentifier, nEnv)
//            .flatten(Always, nEnv)
//        val projectedTrait = pTrait as? Trait
//            ?: throw invocation.make<TypeSystem>("Projecting conformance to non-Trait type $pTrait (Kind: ${KindUtil.getKind(pTrait, pTrait::class.java.simpleName,node)}) is currently unsupported", node.traitIdentifier)
//
//        val projection = Projection(projectedType, projectedTrait)
//        val mEnv = ProjectionEnvironment(nEnv, projection)
//
//        env.add(projection, projectedType)
//
//        val flat = projectedType.flatten(projectedType, mEnv)
//
//        if (flat is Union) {
//            for (constructor in flat.unionConstructors) {
//                env.add(Projection(constructor, projection.target), constructor)
//            }
//        } else if (flat is Struct) {
//            env.add(Projection(flat, projection.target), flat)
//        }
//
//        val signatureNodes = node.body.filterIsInstance<MethodDelegateNode>()
//        val propertyNodes = node.body.filterIsInstance<ProjectedPropertyAssignmentNode>()
//
//        val properties = propertyNodes.map {
//            TypeInferenceUtils.inferAs<ProjectedPropertyAssignmentNode, Property>(it, mEnv)
//        } + when (flat) {
//            is Struct -> flat.getProperties()
//            else -> emptyList()
//        }
//
//        val propertyResults = projectedTrait.properties.fold(TraitMemberVerificationResult.Implemented<Property>(emptyList()) as TraitMemberVerificationResult<Property>) { acc, next ->
//            acc + verifyProperty(mEnv, next, properties)
//        }
//
//        check(propertyResults, node, projectedType, projectedTrait)
//
//        val signatures = signatureNodes.mapNotNull {
//            val node = it
//            when (val type = TypeInferenceUtils.infer(node, mEnv)) {
//                is AnyArrow -> type.toSignature(projectedType, node.methodName.identifier)
//                else -> null
//            }
//        }
//
//        val signatureResults = projectedTrait.signatures.fold(TraitMemberVerificationResult.Implemented<Signature>(emptyList()) as TraitMemberVerificationResult<Signature>) { acc, next ->
//            acc + verifySignature(mEnv, next, signatures)
//        }
//
//        check(signatureResults, node, projectedType, projectedTrait)
//
//        for (signature in (signatureResults as TraitMemberVerificationResult.Implemented).members) {
//            env.add(signature)
//        }
//
//        val signatureNames = signatures.map { it.name }
//        val missing = projectedTrait.signatures.filterNot { it.name in signatureNames }
//
//        if (missing.isNotEmpty()) {
//            val pretty = missing.joinToString("\n") { it.prettyPrint(1) }
//
//            throw invocation.make<TypeSystem>("Incomplete Projection `${projectedType.prettyPrint()}` : `${projectedTrait.prettyPrint()}`. Missing the following expected Methods:\n$pretty", node)
//        }
//
//        return projectedType
//    }
}