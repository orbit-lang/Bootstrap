package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
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

        val projectedTrait = generalisedTrait.substitute(generalisedTrait, projectedType) as Trait
        val projection = Projection(projectedType, projectedTrait)

        val mEnv = ProjectionEnvironment(nEnv, projection)

        mEnv.annotate(SignatureInference.Option.Persistent)

        node.body.forEach { TypeInferenceUtils.infer(it, mEnv) }

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
}