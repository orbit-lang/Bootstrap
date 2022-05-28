package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.ProjectionNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.types.next.inference.ProjectionWhereClauseInference
import org.orbit.types.next.inference.TypeAnnotatedInferenceContext
import org.orbit.util.Invocation
import org.orbit.util.Printer

object ProjectionPhase : TypePhase<ProjectionNode, TypeComponent>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<ProjectionNode>): TypeComponent {
        // TODO - Extend other things: Traits, PolymorphicTypes, etc
        val source = input.inferenceUtil.inferAs<TypeExpressionNode, IType>(input.node.typeIdentifier)
        val nInferenceUtil = input.inferenceUtil.derive(retainsTypeMap = true, retainsBindingScope = true, self = source)

        if (input.node.instanceBinding != null) {
            nInferenceUtil.bind(input.node.instanceBinding.identifier, source)
        }

        val target = nInferenceUtil.infer(input.node.traitIdentifier)

        if (target !is ITrait) throw invocation.make<TypeSystem>("Only Trait-like components may appear on the right-hand side of a Type Projection, found ${target.toString(printer)} (Kind: ${target.kind.toString(printer)})", input.node.traitIdentifier)

        val projectedProperties = input.node.whereNodes
            .map { ProjectionWhereClauseInference.infer(nInferenceUtil, TypeAnnotatedInferenceContext(target, it::class.java), it) }
            .map { it.typeValue() as ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member> }

        val projection = Projection(source, target, projectedProperties)

        return when (projection.implementsProjectedTrait(nInferenceUtil.toCtx())) {
            is ContractResult.Failure -> throw invocation.make<TypeSystem>("Type Projection ${projection.toString(printer)} is not fully implemented", input.node)

            else -> {
                projection.project(input.inferenceUtil)
            }
        }
    }
}