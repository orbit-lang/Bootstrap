package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object ModuleInference : ITypeInference<ModuleNode> {
    override fun infer(node: ModuleNode, env: Env): IType<*> {
        TypeSystemUtils.inferAll(node.contexts, env)
        TypeSystemUtils.inferAll(node.entityDefs.filterIsInstance<TypeDefNode>(), env)
        TypeSystemUtils.inferAll(node.methodDefs.map { it.signature }, env, parametersOf(true))
        TypeSystemUtils.inferAll(node.methodDefs, env)

        return env
    }
}