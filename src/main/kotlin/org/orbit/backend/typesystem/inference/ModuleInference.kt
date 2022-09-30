package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode

object ModuleInference : ITypeInference<ModuleNode> {
    override fun infer(node: ModuleNode, env: Env): AnyType {
        TypeSystemUtils.inferAll(node.entityDefs.filterIsInstance<TypeDefNode>(), env)
        TypeSystemUtils.inferAll(node.entityDefs.filterIsInstance<TraitDefNode>(), env)
        TypeSystemUtils.inferAll(node.contexts, env)
        TypeSystemUtils.inferAll(node.typeAliasNodes, env)
        TypeSystemUtils.inferAll(node.methodDefs.map { it.signature }, env, parametersOf(true))
        TypeSystemUtils.inferAll(node.operatorDefs, env)
        TypeSystemUtils.inferAll(node.extensions, env)
        TypeSystemUtils.inferAll(node.projections, env)
        TypeSystemUtils.inferAll(node.methodDefs, env)

        return env
    }
}