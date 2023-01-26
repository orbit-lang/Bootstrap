package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.GlobalEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode

object ModuleInference : ITypeInference<ModuleNode, GlobalEnvironment> {
    override fun infer(node: ModuleNode, env: GlobalEnvironment): AnyType {
        TypeInferenceUtils.inferAll(node.entityDefs.filterIsInstance<TypeDefNode>(), env)
        TypeInferenceUtils.inferAll(node.entityDefs.filterIsInstance<TraitDefNode>(), env)
        TypeInferenceUtils.inferAll(node.typeEffects, env)
        TypeInferenceUtils.inferAll(node.attributeDefs, env)
        TypeInferenceUtils.inferAll(node.effects, env)
        TypeInferenceUtils.inferAll(node.typeAliasNodes, env)
        TypeInferenceUtils.inferAll(node.projections, env)
        TypeInferenceUtils.inferAll(node.contexts, env)
        TypeInferenceUtils.inferAll(node.methodDefs.map { it.signature }, env, parametersOf(true))
        TypeInferenceUtils.inferAll(node.operatorDefs, env)
        TypeInferenceUtils.inferAll(node.extensions, env)
        TypeInferenceUtils.inferAll(node.methodDefs, env)

        return IType.Always
    }
}