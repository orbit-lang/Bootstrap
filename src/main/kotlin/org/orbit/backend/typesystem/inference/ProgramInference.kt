package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ProgramNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object ProgramInference : ITypeInference<ProgramNode> {
    override fun infer(node: ProgramNode, env: Env): IType<*> {
        TypeSystemUtils.inferAll(node.getModuleDefs(), env)

        return IType.Always
    }
}