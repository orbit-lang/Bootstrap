package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.ParameterNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object TraitDefInference : ITypeInference<TraitDefNode> {
    override fun infer(node: TraitDefNode, env: Env): AnyType {
        val path = node.getPath()
        val protoTrait = IType.Trait(path.toString(OrbitMangler), emptyList(), emptyList())

        val trait = env.withSelf(protoTrait) { nEnv ->
            val members = TypeSystemUtils.inferAllAs<ParameterNode, IType.Member>(node.properties, nEnv)
            val signatures = TypeSystemUtils.inferAllAs<MethodSignatureNode, IType.Signature>(node.signatures, nEnv, parametersOf(true))
            val trait = IType.Trait(path.toString(OrbitMangler), members, signatures)

            env.extendInPlace(Decl.Trait(trait))
            trait
        }

        return trait
    }
}