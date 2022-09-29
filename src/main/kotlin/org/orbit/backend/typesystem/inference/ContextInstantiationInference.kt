package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.phase.globalContext
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.util.Invocation

object ContextInstantiationInference : ITypeInference<ContextInstantiationNode>, KoinComponent {
    private val globalContext: Env by globalContext()
    private val invocation: Invocation by inject()

    override fun infer(node: ContextInstantiationNode, env: Env): AnyType {
        val nEnv = env + TypeSystemUtils.inferAs<TypeExpressionNode, Env>(node.contextIdentifierNode, globalContext)
        val abstractTypeParameters = nEnv.getUnsolvedTypeParameters()
        val concreteTypeParameters = TypeSystemUtils.inferAll(node.typeParameters, nEnv)

        if (concreteTypeParameters.count() != abstractTypeParameters.count()) {
            val prettyParameters = abstractTypeParameters.joinToString("\n") { it.prettyPrint(1) }
            throw invocation.make<TypeSystem>("Context `${nEnv.name}` declares ${abstractTypeParameters.count()} Type Parameters, found ${concreteTypeParameters.count()}:\n\t$prettyParameters", node)
        }

        // TODO - Enforce Type Variable Constraints (if any)
        val mEnv = nEnv.solvingAll(abstractTypeParameters.zip(concreteTypeParameters))

        val abstractValueParameters = mEnv.context.values
        val concreteValueParameters = TypeSystemUtils.inferAll(node.valueParameters, mEnv)

        if (concreteValueParameters.count() != abstractValueParameters.count()) {
            val prettyParameters = abstractValueParameters.joinToString("\n") { it.prettyPrint(1) }
            throw invocation.make<TypeSystem>("Context `${nEnv.name}` declares ${abstractValueParameters.count()} Value Parameters, found ${concreteValueParameters.count()}:\n\t$prettyParameters", node)
        }

        for ((idx, abstract) in abstractValueParameters.withIndex()) {
            val concrete = concreteValueParameters[idx]

            if (!TypeUtils.checkEq(mEnv, concrete, abstract.type)) {
                throw invocation.make<TypeSystem>("Value Parameter at index $idx cannot be satisfied by Type `$concrete`, expected `${abstract.type}`", node)
            }
        }

        return mEnv
    }
}