package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode

object ModuleInference : ITypeInferenceOLD<ModuleNode> {
    override fun infer(node: ModuleNode, env: Env): AnyType {
        TypeSystemUtilsOLD.inferAll(node.entityDefs.filterIsInstance<TypeDefNode>(), env)
        TypeSystemUtilsOLD.inferAll(node.entityDefs.filterIsInstance<TraitDefNode>(), env)
        TypeSystemUtilsOLD.inferAll(node.contexts, env)
        TypeSystemUtilsOLD.inferAll(node.typeAliasNodes, env)
        TypeSystemUtilsOLD.inferAll(node.methodDefs.map { it.signature }, env, parametersOf(true))
        TypeSystemUtilsOLD.inferAll(node.operatorDefs, env)
        TypeSystemUtilsOLD.inferAll(node.extensions, env)
        TypeSystemUtilsOLD.inferAll(node.projections, env)
        TypeSystemUtilsOLD.inferAll(node.methodDefs, env)

        return env
    }
}