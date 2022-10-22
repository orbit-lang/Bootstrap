package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.ParameterNode
import org.orbit.core.nodes.TraitDefNode

object TraitDefInference : ITypeInferenceOLD<TraitDefNode> {
    override fun infer(node: TraitDefNode, env: Env): AnyType {
        val path = node.getPath()
        val protoTrait = IType.Trait(path.toString(OrbitMangler), emptyList(), emptyList())
        val nEnv = env.withSelf(protoTrait)
        val members = TypeSystemUtilsOLD.inferAllAs<ParameterNode, IType.Member>(node.properties, nEnv)
        val signatures = TypeSystemUtilsOLD.inferAllAs<MethodSignatureNode, IType.Signature>(node.signatures, nEnv, parametersOf(true))
        val trait = IType.Trait(path.toString(OrbitMangler), members, signatures)

        env.extendInPlace(Decl.Trait(trait))

        return trait
    }
}