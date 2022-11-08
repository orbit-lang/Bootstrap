package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.SelfTypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.ParameterNode
import org.orbit.core.nodes.TraitDefNode

object TraitDefInference : ITypeInference<TraitDefNode, IMutableTypeEnvironment> {
    override fun infer(node: TraitDefNode, env: IMutableTypeEnvironment): AnyType {
        val path = node.getPath()
        val protoTrait = IType.Trait(path.toString(OrbitMangler), emptyList(), emptyList())
        val nEnv = SelfTypeEnvironment(env, protoTrait)
        val properties = TypeInferenceUtils.inferAllAs<ParameterNode, IType.Property>(node.properties, nEnv)
        val signatures = TypeInferenceUtils.inferAllAs<MethodSignatureNode, IType.Signature>(node.signatures, nEnv, parametersOf(true))
        val trait = IType.Trait(path.toString(OrbitMangler), properties, signatures)

        env.add(trait)

        return trait
    }
}