package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.ContextNode
import org.orbit.core.nodes.EntityDefNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode

object ContextInference : ITypeInference<ContextNode, IMutableTypeEnvironment> {
    override fun infer(node: ContextNode, env: IMutableTypeEnvironment): AnyType {
        val abstracts = node.typeVariables.map { Specialisation(it.getPath()) }
        // TODO - Abstract Value Parameters
        val ctx = Context(node.getPath(), abstracts)
        val nEnv = ContextualTypeEnvironment(env, ctx)

        abstracts.forEach { nEnv.add(it.abstract) }

        GlobalEnvironment.add(ctx)

        val entityDefs = node.body.filterIsInstance<EntityDefNode>()

        TypeInferenceUtils.inferAll(entityDefs, nEnv)

        val signatureNodes = node.body.filterIsInstance<MethodDefNode>().map { it.signature }
        val signatures = TypeInferenceUtils.inferAllAs<MethodSignatureNode, IType.Signature>(signatureNodes, nEnv, parametersOf(false))

        signatures.forEach { nEnv.add(it) }

        TypeInferenceUtils.inferAll(node.body, nEnv)

        return ctx
    }
}