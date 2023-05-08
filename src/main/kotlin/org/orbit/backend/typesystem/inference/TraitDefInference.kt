package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.ParameterNode
import org.orbit.core.nodes.TraitDefNode

object TraitDefInference : ITypeInference<TraitDefNode, IMutableTypeEnvironment> {
    override fun infer(node: TraitDefNode, env: IMutableTypeEnvironment): AnyType {
        val path = node.getPath()
        val protoTrait = Trait(path.toString(OrbitMangler), emptyList(), emptyList())
        val nEnv = SelfTypeEnvironment(env, protoTrait)
        val properties = TypeInferenceUtils.inferAllAs<ParameterNode, Property>(node.properties, nEnv)

        val options = SignatureInference.Option.Persistent + SignatureInference.Option.Virtual

        nEnv.annotate(options)

        val signatures = TypeInferenceUtils.inferAllAs<MethodSignatureNode, Signature>(node.signatures, nEnv)
        val trait = Trait(path.toString(OrbitMangler), properties, signatures)

        env.add(trait)
        signatures.forEach { env.add(it) }

        return trait
    }
}