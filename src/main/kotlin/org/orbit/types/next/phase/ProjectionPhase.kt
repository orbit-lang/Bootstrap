package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.ProjectionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnyInferenceContext
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

        val wheres = nInferenceUtil.inferAllAs<WhereClauseNode, Field>(input.node.whereNodes, AnyInferenceContext(WhereClauseNode::class.java))

        val nType: IType = Type(source.fullyQualifiedName, source.getFields() + wheres)
        val mType: TypeComponent = when (source) {
            is MonomorphicType<*> -> (source as MonomorphicType<IType>).with(nType)
            else -> nType
        }

        input.inferenceUtil.addConformance(mType, target)

        val ctx = input.inferenceUtil.toCtx()

        return when (val result = target.isImplemented(ctx, mType)) {
            is ContractResult.Success, ContractResult.None -> mType
            is ContractResult.Failure -> throw invocation.make<TypeSystem>("Type Projection error:\n\t${result.getErrorMessage(printer, source)}", input.node)
            is ContractResult.Group -> when (result.isSuccessGroup) {
                true -> mType
                else -> throw invocation.make<TypeSystem>("Type Projection errors:\n\t${result.getErrorMessage(printer, source)}", input.node)
            }
        }
    }
}