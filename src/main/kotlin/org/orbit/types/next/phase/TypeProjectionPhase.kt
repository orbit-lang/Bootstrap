package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.util.Invocation
import org.orbit.util.Printer

object TypeProjectionPhase : TypePhase<TypeProjectionNode, TypeComponent>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<TypeProjectionNode>): TypeComponent {
        // TODO - Extend other things: Traits, PolymorphicTypes, etc
        val source = input.inferenceUtil.inferAs<TypeExpressionNode, IType>(input.node.typeIdentifier)
        val nInferenceUtil = input.inferenceUtil.derive(retainsTypeMap = true, retainsBindingScope = true, self = source)

        val target = nInferenceUtil.infer(input.node.traitIdentifier)

        if (target !is ITrait) return Never("Only Trait-like components may appear on the right-hand side of a Type Projection, found ${target.toString(printer)} (Kind: ${target.kind.toString(printer)})")

//        if (target.references(source)) return Never("Source Type ${source.toString(printer)} must not reference itself in the projected Trait ${target.toString(printer)}", input.node.traitIdentifier.firstToken.position)

        val wheres = input.inferenceUtil.inferAllAs<WhereClauseNode, Field>(input.node.whereNodes,
            AnyInferenceContext(WhereClauseNode::class.java)
        )

        val ctx = input.inferenceUtil.toCtx()
        val nType = Type(source.fullyQualifiedName, source.getFields() + wheres)

        return when (val result = target.isImplemented(ctx, nType)) {
            is ContractResult.Success, ContractResult.None -> nType
            is ContractResult.Failure -> throw invocation.make<TypeSystem>("Type Projection error:\n\t${result.getErrorMessage(printer, source)}", input.node)
            is ContractResult.Group -> when (result.isSuccessGroup) {
                true -> nType
                else -> throw invocation.make<TypeSystem>("Type Projection errors:\n\t${result.getErrorMessage(printer, source)}", input.node)
            }
        }
    }
}