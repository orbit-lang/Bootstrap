package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.phase.globalContext
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.util.Invocation

object ContextInstantiationInference : ITypeInferenceOLD<ContextInstantiationNode>, KoinComponent {
    private val globalContext: Env by globalContext()
    private val invocation: Invocation by inject()

    override fun infer(node: ContextInstantiationNode, env: Env): AnyType {
        val nEnv = env + TypeSystemUtilsOLD.inferAs<TypeExpressionNode, Env>(node.contextIdentifierNode, globalContext)
        val abstractTypeParameters = nEnv.getUnsolvedTypeParameters()
        val concreteTypeParameters = TypeSystemUtilsOLD.inferAll(node.typeParameters, nEnv)

        if (concreteTypeParameters.count() != abstractTypeParameters.count()) {
            val prettyParameters = abstractTypeParameters.joinToString("\n") { it.prettyPrint(1) }
            throw invocation.make<TypeSystem>("Context `${nEnv.name}` declares ${abstractTypeParameters.count()} Type Parameters, found ${concreteTypeParameters.count()}:\n\t$prettyParameters", node)
        }

        // TODO - Enforce Type Variable Constraints (if any)
        val mEnv = nEnv.solvingAll(abstractTypeParameters.zip(concreteTypeParameters))

        val abstractValueParameters = mEnv.context.bindings.map { it.abstract }
        val concreteValueParameters = TypeSystemUtilsOLD.inferAll(node.valueParameters, mEnv)

        if (concreteValueParameters.count() != abstractValueParameters.count()) {
            val prettyParameters = abstractValueParameters.joinToString("\n") { it.prettyPrint(1) }
            throw invocation.make<TypeSystem>("Context `${nEnv.name}` declares ${abstractValueParameters.count()} Value Parameters, found ${concreteValueParameters.count()}:\n\t$prettyParameters", node)
        }

        for ((idx, abstract) in abstractValueParameters.withIndex()) {
            val concrete = concreteValueParameters[idx]

            if (!TypeUtils.checkEq(mEnv, concrete, abstract)) {
                throw invocation.make<TypeSystem>("Value Parameter at index $idx cannot be satisfied by Type `$concrete`, expected `${abstract}`", node)
            }
        }

        return mEnv
    }
}