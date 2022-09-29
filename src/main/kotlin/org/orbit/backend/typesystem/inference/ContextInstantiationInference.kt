package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.phase.globalContext
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.util.Invocation

object ContextInstantiationInference : ITypeInference<ContextInstantiationNode>, KoinComponent {
    private val globalContext: Env by globalContext()
    private val invocation: Invocation by inject()

    override fun infer(node: ContextInstantiationNode, env: Env): AnyType {
        val nEnv = env + TypeSystemUtils.inferAs<TypeExpressionNode, Env>(node.contextIdentifierNode, globalContext)
        val abstractTypeParameters = nEnv.getUnsolvedTypeParameters()
        val concreteTypeParameters = TypeSystemUtils.inferAll(node.typeVariables, env)

        if (concreteTypeParameters.count() != abstractTypeParameters.count()) {
            throw invocation.make<TypeSystem>("Context `${nEnv.name}` declares ${abstractTypeParameters.count()} Type Variables, found ${concreteTypeParameters.count()}",
                node)
        }

        // TODO - Enforce Type Variable Constraints (if any)

        return nEnv.solvingAll(abstractTypeParameters.zip(concreteTypeParameters))
    }
}