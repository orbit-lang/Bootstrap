package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeEffectInvocationNode
import org.orbit.util.Invocation

object TypeEffectInvocationInference : ITypeInference<TypeEffectInvocationNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeEffectInvocationNode, env: ITypeEnvironment): AnyType {
        val args = TypeInferenceUtils.inferAll(node.arguments, env)
        val effect = env.getTypeAs<IType.TypeEffect>(node.effectIdentifier.getPath())
            ?: throw invocation.make<TypeSystem>("Unknown Type Effect `${node.effectIdentifier.getTypeName()}`", node.effectIdentifier)

        if (args.count() != effect.arguments.count()) {
            throw invocation.make<TypeSystem>("Type Effect expects ${effect.arguments.count()} arguments, found ${args.count()}", node.effectIdentifier)
        }

        return effect.arguments.zip(args)
            .fold(effect) { acc, next -> acc.substitute(Substitution(next.first, next.second)) as IType.TypeEffect }
    }
}