package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.ProjectionNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.types.next.inference.ProjectedPropertyInferenceContext
import org.orbit.types.next.inference.ProjectionWhereClauseInference
import org.orbit.types.next.inference.TypeAnnotatedInferenceContext
import org.orbit.util.Invocation
import org.orbit.util.Printer

object ProjectionPhase : TypePhase<ProjectionNode, TypeComponent>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<ProjectionNode>): TypeComponent {
        // TODO - Extend other things: Traits, PolymorphicTypes, etc
        val source = input.inferenceUtil.infer(input.node.typeIdentifier)

        if (source !is IType)
            throw invocation.make<TypeSystem>("Projections on non-Types is not currently supported, found ${source.toString(printer)} (Kind: ${source.kind.toString(printer)})", SourcePosition.unknown)

        val nInferenceUtil = input.inferenceUtil.derive(retainsTypeMap = true, retainsBindingScope = true, self = source)

        if (input.node.instanceBinding != null) {
            nInferenceUtil.bind(input.node.instanceBinding.identifier, source)
        }

        val target = when (val t = nInferenceUtil.infer(input.node.traitIdentifier)) {
            is MonomorphicType<*> -> when (t.specialisedType) {
                is ITrait -> t.specialisedType
                else -> throw invocation.make<TypeSystem>("Only Trait-like components may appear on the right-hand side of a Projection, found ${t.toString(printer)} (Kind: ${t.kind.toString(printer)})", input.node.traitIdentifier)
            }
            is ITrait -> t
            else -> throw invocation.make<TypeSystem>("Only Trait-like components may appear on the right-hand side of a Projection, found ${t.toString(printer)} (Kind: ${t.kind.toString(printer)})", input.node.traitIdentifier)
        }

        // NOTE - By declaring conformance here, projected properties can refer to each other
        nInferenceUtil.addConformance(source, target)
        input.inferenceUtil.addConformance(source, target)

        val projection = Projection(source, target, emptyList())

        val projectedProperties = input.node.whereNodes
            .map { ProjectionWhereClauseInference.infer(nInferenceUtil, ProjectedPropertyInferenceContext(projection, it::class.java), it) }
            .map { it.typeValue() as ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member> }

        val nProjection = Projection(source, target, projectedProperties)

        return when (nProjection.implementsProjectedTrait(nInferenceUtil.toCtx())) {
            is ContractResult.Failure -> throw invocation.make<TypeSystem>("Type Projection ${nProjection.toString(printer)} is not fully implemented", input.node)

            else -> {
                nProjection.project(input.inferenceUtil)
            }
        }
    }
}