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
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Substitution(val typeVariable: TypeVariable, val type: TypeComponent)

interface ContextualRefinement {
    fun refine(inferenceUtil: InferenceUtil)
}

private object RefinementErrorWriter : KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    @Throws
    fun missing(typeVariable: TypeComponent) {
        throw invocation.make<TypeSystem>("Unknown Type Variable ${typeVariable.toString(printer)} in Equality Constraint", SourcePosition.unknown)
    }

    @Throws
    fun doubleRefinement(typeVariable: TypeComponent) {
        throw invocation.make<TypeSystem>("Equality Constraints may only be applied to Type Variables. ${typeVariable.toString(printer)} is already a concrete type, possibly as the result of a previous constraint", SourcePosition.unknown)
    }

    @Throws
    fun cyclicConstraint(source: TypeComponent, target: TypeComponent) {
        throw invocation.make<TypeSystem>("Cyclic Equality Constraint detected between ${source.toString(printer)} and ${target.toString(printer)}", SourcePosition.unknown)
    }
}

data class EqualityRefinement(val typeVariable: TypeComponent, val concreteType: TypeComponent) : ContextualRefinement {
    override fun refine(inferenceUtil: InferenceUtil) = when (inferenceUtil.find(typeVariable.fullyQualifiedName)) {
        null -> RefinementErrorWriter.missing(typeVariable)
        is TypeVariable -> {
            val a = inferenceUtil.find(typeVariable.fullyQualifiedName)!!
            val b = inferenceUtil.find(concreteType.fullyQualifiedName)!!

            if (a.fullyQualifiedName == b.fullyQualifiedName) {
                RefinementErrorWriter.cyclicConstraint(typeVariable, concreteType)
            }

            inferenceUtil.declare(Alias(typeVariable.fullyQualifiedName, concreteType))
        }
        else -> when (typeVariable.fullyQualifiedName == concreteType.fullyQualifiedName) {
            true -> {}
            else -> RefinementErrorWriter.doubleRefinement(typeVariable)
        }
    }
}

data class ConformanceRefinement(val type: TypeComponent, val trait: ITrait) : ContextualRefinement {
    override fun refine(inferenceUtil: InferenceUtil)
        = inferenceUtil.addConformance(type, trait)
}

interface ITypeConstraint<A: TypeComponent> : TypeComponent {
    override val kind: Kind get() = IntrinsicKinds.Type
    override val isSynthetic: Boolean get() = false

    fun check(ctx: Ctx)
    fun substitute(typeVariable: TypeVariable, type: TypeComponent) : ITypeConstraint<A>
    fun getRefinement() : ContextualRefinement

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other.fullyQualifiedName) {
        fullyQualifiedName -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}

private object ConstraintErrorWriter : KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    @Throws
    fun brokenConstraint(constraint: ITypeConstraint<*>) {
        throw invocation.make<TypeSystem>("Contextual Constraint broken: ${printer.apply(constraint.fullyQualifiedName, PrintableKey.Bold, PrintableKey.Italics)}", SourcePosition.unknown)
    }
}

data class SameConstraint(val source: TypeComponent, val target: TypeComponent) : ITypeConstraint<TypeComponent>, KoinComponent {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} = ${target.fullyQualifiedName}"

    override fun check(ctx: Ctx) = when (source) {
        is TypeVariable -> {
            val invocation = getKoinInstance<Invocation>()
            val printer = getKoinInstance<Printer>()

            throw invocation.make<TypeSystem>("Constraint ${source.toString(printer)} = ${target.toString(printer)}, unrefined for Type Variable ${source.toString(printer)}", SourcePosition.unknown)
        }

        else -> when (NominalEq.eq(ctx, source, target)) {
            true -> {}
            else -> ConstraintErrorWriter.brokenConstraint(this)
        }
    }

    override fun substitute(typeVariable: TypeVariable, type: TypeComponent): ITypeConstraint<TypeComponent> {
        return if (source is TypeVariable && source.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            SameConstraint(type, target)
        } else if (target is TypeVariable && target.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            SameConstraint(source, type)
        } else this
    }

    override fun getRefinement(): ContextualRefinement
        = EqualityRefinement(source, target)
}

data class KindEqConstraint(val source: TypeComponent, val target: TypeComponent) : ITypeConstraint<TypeComponent> {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} ^ ${target.fullyQualifiedName}"

    override fun check(ctx: Ctx) {
        val a = when (source) {
            is Kind -> source
            else -> source.kind
        }

        val b = when (target) {
            is Kind -> target
            else -> target.kind
        }

        if (!AnyEq.eq(ctx, a, b)) {
            ConstraintErrorWriter.brokenConstraint(KindEqConstraint(a, b))
        }
    }

    override fun getRefinement(): ContextualRefinement {
        TODO("Not yet implemented")
    }

    override fun substitute(typeVariable: TypeVariable, type: TypeComponent): ITypeConstraint<TypeComponent> {
        return if (source is TypeVariable && source.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            KindEqConstraint(type, target)
        } else if (target is TypeVariable && target.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            KindEqConstraint(source, type)
        } else this
    }
}

data class LikeConstraint(val source: TypeComponent, val trait: TypeComponent) : ITypeConstraint<ITrait> {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} : ${trait.fullyQualifiedName}"

    override fun check(ctx: Ctx) = when (source) {
        is TypeVariable -> {
            val invocation = getKoinInstance<Invocation>()
            val printer = getKoinInstance<Printer>()

            throw invocation.make<TypeSystem>("Constraint ${source.toString(printer)} : ${trait.toString(printer)}, unrefined for Type Variable ${source.toString(printer)}", SourcePosition.unknown)
        }

        else -> when (trait) {
            is ITrait -> when (StructuralEq.eq(ctx, TypeReference(trait.fullyQualifiedName), source)) {
                true -> {}
                else -> ConstraintErrorWriter.brokenConstraint(this)
            }
            else -> {
                val invocation = getKoinInstance<Invocation>()
                val printer = getKoinInstance<Printer>()
                throw invocation.make<TypeSystem>("Cannot check for conformance against non-Trait ${trait.toString(printer)}", SourcePosition.unknown)
            }
        }
    }

    override fun substitute(typeVariable: TypeVariable, type: TypeComponent): ITypeConstraint<ITrait> {
        return if (source is TypeVariable && source.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            LikeConstraint(type, trait)
        } else if (trait is TypeVariable && trait.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            LikeConstraint(source, type)
        } else this
    }

    override fun getRefinement(): ContextualRefinement
        = ConformanceRefinement(source, trait as ITrait)
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