package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CauseNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.util.Invocation

object CauseInference : ITypeInference<CauseNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: CauseNode, env: ITypeEnvironment): AnyType {
        val effect = TypeInferenceUtils.inferAs<TypeIdentifierNode, IType.Effect>(node.invocationNode.effectIdentifier, env)
        val args = TypeInferenceUtils.inferAll(node.invocationNode.args, env)

        if (args.count() != effect.takes.count()) {
            throw invocation.make<TypeSystem>("Attempt to cause Effect $effect with ${args.count()} arguments, expected ${effect.takes.count()}", node.invocationNode)
        }

        effect.takes.zip(args).forEachIndexed { idx, it ->
            if (!TypeUtils.checkEq(env, it.first, it.second)) {
                throw invocation.make<TypeSystem>("Effect $effect expected argument of Type ${it.first} at index $idx, found ${it.second}", node.invocationNode.args[idx])
            }
        }

        return IType.Always
    }
}