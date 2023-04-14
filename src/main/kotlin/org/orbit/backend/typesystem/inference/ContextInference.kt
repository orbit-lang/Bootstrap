package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.*

object ContextInference : ITypeInference<ContextNode, IMutableTypeEnvironment> {
    override fun infer(node: ContextNode, env: IMutableTypeEnvironment): AnyType {
        val abstracts = node.typeVariables.map { Specialisation(it.getPath()) }
        // TODO - Abstract Value Parameters
        val ctx = Context(node.getPath(), abstracts)
        val nEnv = ContextualTypeEnvironment(env, ctx)

        abstracts.forEach { nEnv.add(it.abstract) }

        val constraints = TypeInferenceUtils.inferAllAs<IContextClauseExpressionNode, AttributeInvocationExpression>(node.clauses, nEnv)

//        val groupedConstraints = mutableMapOf<AnyType, List<ITypeConstraint>>()
//        for (constraint in constraints) {
//            val pConstraints = groupedConstraints[constraint.type] ?: emptyList()
//
//            if (pConstraints.contains(constraint)) continue
//
//            groupedConstraints[constraint.type] = pConstraints + constraints.filter { it.type === constraint.type }
//        }

        constraints.forEach { it.evaluate(nEnv) }

        val nAbstracts = when (constraints.isEmpty()) {
            true -> abstracts
            else -> abstracts
//            else -> groupedConstraints.mapNotNull {
//                val abstract = it.key as? IType.TypeVar ?: return@mapNotNull null
//
//                Specialisation(IType.TypeVar(abstract.name, it.value))
//            }
        }

        val nCtx = Context(node.getPath(), nAbstracts)
        val mEnv = ContextualTypeEnvironment(env, nCtx)

        GlobalEnvironment.add(nCtx)

        val entityDefs = node.body.filterIsInstance<EntityDefNode>()

        TypeInferenceUtils.inferAll(entityDefs, mEnv)

        val signatureNodes = node.body.filterIsInstance<MethodDefNode>().map { it.signature }
        val signatures = TypeInferenceUtils.inferAllAs<MethodSignatureNode, Signature>(signatureNodes, mEnv, parametersOf(false))

        signatures.forEach { mEnv.add(it) }

        val body = node.body.filterNot { it is EntityDefNode }

        // TODO - There is a bug in here where method signatures are getting bound twice
        TypeInferenceUtils.inferAll(body, mEnv)

        return nCtx
    }
}