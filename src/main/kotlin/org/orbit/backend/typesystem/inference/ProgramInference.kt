package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.GlobalEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ProgramNode

object ProgramInference : ITypeInference<ProgramNode, GlobalEnvironment> {
    override fun infer(node: ProgramNode, env: GlobalEnvironment): AnyType {
        TypeInferenceUtils.inferAll(node.getModuleDefs(), env)

        return IType.Always
    }
}