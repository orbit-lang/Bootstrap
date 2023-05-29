package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.*

private sealed interface IContextVariableSpecialiser<N: IContextVariableNode> {
    fun specialise(node: N, env: ITypeEnvironment) : Specialisation
}

private object TypeIdentifierSpecialiser : IContextVariableSpecialiser<TypeIdentifierNode> {
    override fun specialise(node: TypeIdentifierNode, env: ITypeEnvironment): Specialisation
        = Specialisation(node.getPath())
}

private object ConstrainedTypeSpecialiser : IContextVariableSpecialiser<ConstrainedTypeVarNode> {
    override fun specialise(node: ConstrainedTypeVarNode, env: ITypeEnvironment): Specialisation {
        // TODO - Arbitrary Constraints
        val constrainedType = TypeVar(node.getPath().toString(OrbitMangler))
        val constraintType = TypeInferenceUtils.inferAs<INode, Trait>(node.constraint, env)
        val constraint = ConformanceConstraint(constrainedType, constraintType)

        return Specialisation(node.getPath(), listOf(constraint))
    }
}

private object AnyContextVariableSpecialiser : IContextVariableSpecialiser<IContextVariableNode> {
    override fun specialise(node: IContextVariableNode, env: ITypeEnvironment): Specialisation = when (node) {
        is TypeIdentifierNode -> TypeIdentifierSpecialiser.specialise(node, env)
        is ConstrainedTypeVarNode -> ConstrainedTypeSpecialiser.specialise(node, env)
        else -> TODO("Specialise Context Variable: $node")
    }
}

object ContextInference : ITypeInference<ContextNode, IMutableTypeEnvironment> {
    override fun infer(node: ContextNode, env: IMutableTypeEnvironment): AnyType {
        val abstracts = node.typeVariables.map { AnyContextVariableSpecialiser.specialise(it, env) }
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
        val signatures = TypeInferenceUtils.inferAllAs<MethodSignatureNode, Signature>(signatureNodes, mEnv)

        signatures.forEach { mEnv.add(it) }

        val body = node.body.filterNot { it is EntityDefNode }

        // TODO - There is a bug in here where method signatures are getting bound twice
        TypeInferenceUtils.inferAll(body, mEnv)

        return nCtx
    }
}