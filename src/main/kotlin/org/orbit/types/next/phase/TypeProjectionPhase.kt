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
        val target = input.inferenceUtil.inferAs<TypeExpressionNode, ITrait>(input.node.traitIdentifier)
        val wheres = input.inferenceUtil.inferAllAs<WhereClauseNode, Field>(input.node.whereNodes,
            AnyInferenceContext(WhereClauseNode::class.java)
        )

        val ctx = input.inferenceUtil.toCtx()
        val nType = Type(source.fullyQualifiedName, source.getFields() + wheres)
        val result = target.isImplemented(ctx, nType)

        return when (result) {
            is ContractResult.Success, ContractResult.None -> nType
            is ContractResult.Failure -> Never("Type Projection error:\n\t${result.getErrorMessage(printer, source)}")
            is ContractResult.Group -> Never("Type Projection errors:\n\t${result.getErrorMessage(printer, source)}")
        }
    }
}