package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.util.Invocation

object ContextInstantiationInference : ITypeInference<ContextInstantiationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ContextInstantiationNode, env: IMutableTypeEnvironment): AnyType {
        val ctx = TypeInferenceUtils.inferAs<TypeExpressionNode, Context>(node.contextIdentifierNode, GlobalEnvironment)
        val abstractTypeParameters = ctx.getUnsolved()
        val concreteTypeParameters = TypeInferenceUtils.inferAll(node.typeParameters, env)

        if (concreteTypeParameters.count() != abstractTypeParameters.count()) {
            val prettyParameters = abstractTypeParameters.joinToString("\n") { it.prettyPrint(1) }
            throw invocation.make<TypeSystem>("Context `${ctx.name}` declares ${abstractTypeParameters.count()} Type Parameters, found ${concreteTypeParameters.count()}:\n\t$prettyParameters", node)
        }

        // TODO - Enforce Type Variable Constraints (if any)
        val nCtx = ctx.solvingAll(abstractTypeParameters.zip(concreteTypeParameters).map(::Specialisation))

        // TODO - Value Parameters

        GlobalEnvironment.registerSpecialisation(nCtx)

        return nCtx
    }
}