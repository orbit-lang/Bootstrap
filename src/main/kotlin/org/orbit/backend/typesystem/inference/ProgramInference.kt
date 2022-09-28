package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ProgramNode

object ProgramInference : ITypeInference<ProgramNode> {
    override fun infer(node: ProgramNode, env: Env): AnyType {
        TypeSystemUtils.inferAll(node.getModuleDefs(), env)

        return IType.Always
    }
}