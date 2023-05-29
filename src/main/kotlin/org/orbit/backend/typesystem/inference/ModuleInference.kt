package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.util.Invocation

object ModuleInference : ITypeInference<ModuleNode, GlobalEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ModuleNode, env: GlobalEnvironment): AnyType {
        if (node.isEmpty) {
            throw invocation.make<TypeSystem>("Empty Modules are meaningless and therefore not allowed: `module ${node.identifier.value}`", node)
        }

        TypeInferenceUtils.inferAll(node.entityDefs.filterIsInstance<TypeDefNode>(), env)
        TypeInferenceUtils.inferAll(node.entityDefs.filterIsInstance<TraitDefNode>(), env)
        TypeInferenceUtils.inferAll(node.typeEffects, env)
        TypeInferenceUtils.inferAll(node.attributeDefs, env)
        TypeInferenceUtils.inferAll(node.effects, env)
        TypeInferenceUtils.inferAll(node.contexts, env)
        TypeInferenceUtils.inferAll(node.typeAliasNodes, env)
        TypeInferenceUtils.inferAll(node.projections, env)

        val nEnv = env.fork()

        nEnv.annotate(SignatureInference.Option.Persistent)

        TypeInferenceUtils.inferAll(node.methodDefs.map { it.signature }, nEnv)
        TypeInferenceUtils.inferAll(node.operatorDefs, env)
        TypeInferenceUtils.inferAll(node.extensions, env)
        TypeInferenceUtils.inferAll(node.methodDefs, env)

        return Always
    }
}