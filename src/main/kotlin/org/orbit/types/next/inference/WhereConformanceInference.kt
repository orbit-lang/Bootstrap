package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.TraitConformanceTypeConstraintNode
import org.orbit.core.nodes.TypeConstraintNode
import org.orbit.core.nodes.TypeConstraintWhereClauseNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Substitution(val typeVariable: TypeVariable, val type: TypeComponent) {
    fun apply(inferenceUtil: InferenceUtil) : InferenceUtil {
        val nInferenceUtil = inferenceUtil.derive()

        nInferenceUtil.declare(Alias(typeVariable.fullyQualifiedName, type))

        return nInferenceUtil
    }
}

interface ITypeConstraint<A: TypeComponent> : TypeComponent {
    override val kind: Kind get() = IntrinsicKinds.Type
    override val isSynthetic: Boolean get() = false

    fun check(ctx: Ctx) : Boolean
    fun substitute(typeVariable: TypeVariable, type: TypeComponent) : ITypeConstraint<A>

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other.fullyQualifiedName) {
        fullyQualifiedName -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}

data class SameConstraint(val source: TypeComponent, val target: TypeComponent) : ITypeConstraint<TypeComponent>, KoinComponent {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} = ${target.fullyQualifiedName}"

    override fun check(ctx: Ctx): Boolean = when (source) {
        is TypeVariable -> {
            val invocation = getKoinInstance<Invocation>()
            val printer = getKoinInstance<Printer>()

            throw invocation.make<TypeSystem>("Constraint ${source.toString(printer)} = ${target.toString(printer)}, unrefined for Type Variable ${source.toString(printer)}", SourcePosition.unknown)
        }

        else -> NominalEq.eq(ctx, source, target)
    }

    override fun substitute(typeVariable: TypeVariable, type: TypeComponent): ITypeConstraint<TypeComponent> = when (source) {
        is TypeVariable -> when (source.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            true -> SameConstraint(type, target)
            else -> this
        }

        else -> this
    }
}

data class LikeConstraint(val source: TypeComponent, val trait: ITrait, ) : ITypeConstraint<ITrait> {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} : ${trait.fullyQualifiedName}"

    override fun check(ctx: Ctx): Boolean = when (source) {
        is TypeVariable -> {
            val invocation = getKoinInstance<Invocation>()
            val printer = getKoinInstance<Printer>()

            throw invocation.make<TypeSystem>("Constraint ${source.toString(printer)} : ${trait.toString(printer)}, unrefined for Type Variable ${source.toString(printer)}", SourcePosition.unknown)
        }

        else -> StructuralEq.eq(ctx, trait, source)
    }

    override fun substitute(typeVariable: TypeVariable, type: TypeComponent): ITypeConstraint<ITrait> = when (source) {
        is TypeVariable -> when (source.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            true -> LikeConstraint(type, trait)
            else -> this
        }

        else -> this
    }
}

//object MemberConstraint : ITypeConstraint<TypeFamily<*>> {
//    override fun check(ctx: Ctx, a: TypeFamily<*>, b: TypeComponent): Boolean = when (a.compare(ctx, b)) {
//        is TypeRelation.Member<*> -> true
//        else -> false
//    }
//}

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