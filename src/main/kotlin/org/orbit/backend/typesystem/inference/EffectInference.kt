package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.backend.typesystem.intrinsics.OrbMoreFx
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.EffectDeclarationNode
import org.orbit.core.nodes.EffectNode
import org.orbit.core.nodes.IInvokableDelegateNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.util.Invocation

object EffectInference : ITypeInference<EffectNode, IMutableTypeEnvironment> {
    override fun infer(node: EffectNode, env: IMutableTypeEnvironment): AnyType {
        val domain = TypeInferenceUtils.inferAll(node.lambda.domain, env)
        val codomain = TypeInferenceUtils.infer(node.lambda.codomain, env)
        val nEffect = Effect(node.identifier.getPath(), domain, codomain)

        env.add(nEffect)

        return nEffect
    }
}

object EffectDeclarationInference : ITypeInference<EffectDeclarationNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: EffectDeclarationNode, env: ITypeEnvironment): AnyType {
        return TypeInferenceUtils.inferAs<TypeIdentifierNode, Effect>(node.effect, env)

//        val effect = TypeInferenceUtils.inferAs<TypeIdentifierNode, Effect>(node.effect, env)
//        val handlerNode = node.handler ?: return effect
//        val handler = TypeInferenceUtils.inferAs<IInvokableDelegateNode, AnyArrow>(handlerNode, env)
//
//        if (handler.getDomain().count() != 1) {
//            throw invocation.make<TypeSystem>("Default Effect Handler has wrong Type. Expected , found $handler", handlerNode)
//        }
//
//        return effect.withHandler(handler)
    }
}