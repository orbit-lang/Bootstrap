package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CauseNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.util.Invocation

object CauseInference : ITypeInference<CauseNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: CauseNode, env: ITypeEnvironment): AnyType {
        val effect = TypeInferenceUtils.inferAs<TypeIdentifierNode, Effect>(node.invocationNode.effectIdentifier, env)

        val matches = mutableListOf<Effect>()
        for (e in env.getTrackedEffects()) {
            if (TypeUtils.checkEq(env, e, effect)) {
                matches.add(e)
            }
        }

        if (matches.isEmpty()) {
            throw invocation.make<TypeSystem>("Attempting to cause Effect $effect in a Method that does not declare it. Add `with $effect` to the return Type of your Method Signature", node.invocationNode)
        }

        val args = TypeInferenceUtils.inferAll(node.invocationNode.args, env)

        if (args.count() != effect.takes.count()) {
            throw invocation.make<TypeSystem>("Attempt to cause Effect $effect with ${args.count()} arguments, expected ${effect.takes.count()}", node.invocationNode)
        }

        effect.takes.zip(args).forEachIndexed { idx, it ->
            if (!TypeUtils.checkEq(env, it.first, it.second)) {
                throw invocation.make<TypeSystem>("Effect $effect expected argument of Type ${it.first} at index $idx, found ${it.second}", node.invocationNode.args[idx])
            }
        }

        return Always
    }
}