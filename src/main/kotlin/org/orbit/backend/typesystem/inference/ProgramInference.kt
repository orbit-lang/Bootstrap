package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.ProgramNode

object ProgramInference : ITypeInferenceOLD<ProgramNode> {
    override fun infer(node: ProgramNode, env: Env): AnyType {
        TypeSystemUtilsOLD.inferAll(node.getModuleDefs(), env)

        return IType.Always
    }
}