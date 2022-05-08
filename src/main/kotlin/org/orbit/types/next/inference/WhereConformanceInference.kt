package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.util.Printer

interface ITypeConstraint<T: TypeComponent, U: TypeComponent> {
    val source: T

    fun check(ctx: Ctx, type: U) : Boolean
}

data class SameConstraint<T: TypeComponent>(override val source: T) : ITypeConstraint<T, T> {
    override fun check(ctx: Ctx, type: T): Boolean = when (type) {
        is T -> NominalEq.eq(ctx, source, type)
        else -> false
    }
}

data class LikeConstraint<T: TypeComponent>(override val source: ITrait) : ITypeConstraint<ITrait, T> {
    override fun check(ctx: Ctx, type: T): Boolean
        = ctx.getConformance(type).contains(source)
}

data class MemberConstraint<T: TypeComponent>(override val source: TypeFamily<T>) : ITypeConstraint<TypeFamily<T>, T> {
    override fun check(ctx: Ctx, type: T): Boolean = when (source.compare(ctx, type)) {
        is TypeRelation.Member<*> -> true
        else -> false
    }
}

data class TypeConstraint(val source: AbstractTypeParameter, val target: ITrait) : TypeComponent {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} : ${target.fullyQualifiedName}"
    override val isSynthetic: Boolean = true
    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other.fullyQualifiedName) {
        fullyQualifiedName -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}

object TraitConformanceConstraintInference : Inference<TraitConformanceTypeConstraintNode, ITrait>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(
        inferenceUtil: InferenceUtil,
        context: InferenceContext,
        node: TraitConformanceTypeConstraintNode
    ): InferenceResult {
        val parameter = inferenceUtil.inferAs<TypeIdentifierNode, AbstractTypeParameter>(node.constrainedTypeNode)
        val trait = inferenceUtil.infer(node.constraintTraitNode)

        return when (trait) {
            is ITrait -> TypeConstraint(parameter, trait)
            else -> Never("Only Traits may appear on the right-hand side of a Conformance Constraint, found ${trait.toString(printer)} (Kind: ${trait.kind.toString(printer)})", node.firstToken.position)
        }.inferenceResult()
    }
}

object TypeConstraintInference : Inference<TypeConstraintNode, TypeConstraint> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeConstraintNode): InferenceResult = when (node) {
        is TraitConformanceTypeConstraintNode -> TraitConformanceConstraintInference.infer(inferenceUtil, context, node)
        else -> TODO("HERE")
    }
}

object WhereConformanceInference : Inference<TypeConstraintWhereClauseNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeConstraintWhereClauseNode): InferenceResult {
        val constraint = inferenceUtil.inferAs<TypeConstraintNode, TypeConstraint>(node.statementNode)

        return InferenceResult.Success(constraint)
    }
}