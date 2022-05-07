package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.types.next.components.Ctx
import org.orbit.types.next.components.PolymorphicType
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.components.toString
import org.orbit.types.next.constraints.*
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.util.Invocation
import org.orbit.util.Printer

object ExtensionPhase : TypePhase<ExtensionNode, TypeComponent>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun refine(acc: Pair<InferenceUtil, ConstraintApplication<TypeComponent>>, next: Constraint<TypeComponent, ConstraintApplication<TypeComponent>>) : Pair<InferenceUtil, ConstraintApplication<TypeComponent>> = when (acc.second) {
        is EqualityConstraintApplication.Partial<*> -> {
            val partial = acc.second as EqualityConstraintApplication.Partial<TypeComponent>
            val nInferenceUtil = acc.first.derive(self = partial.result)

            Pair(nInferenceUtil, next.refine(nInferenceUtil, partial.result)!!)
        }
        is EqualityConstraintApplication.Total<*> -> when (next) {
            is EqualityConstraint<*> -> throw invocation.make<TypeSystem>("Attempting to refine total Type ${(acc.second as EqualityConstraintApplication.Total<TypeComponent>).result.toString(printer)}", SourcePosition.unknown)
            else -> {
                val nInferenceUtil = acc.first.derive(self = acc.second.resultValue())
                Pair(nInferenceUtil, next.refine(nInferenceUtil, acc.second.initialValue)!!)
            }
        }
        is EqualityConstraintApplication.None<*> -> Pair(acc.first, next.refine(acc.first, acc.second.initialValue)!!)
        is ConformanceConstraintApplication<*> -> {
            Pair(acc.first, next.refine(acc.first, acc.second.initialValue)!!)
        }
        is IdentityConstraintApplication<*> -> Pair(acc.first, next.refine(acc.first, acc.second.initialValue)!!)
        else -> acc
    }

    override fun run(input: TypePhaseData<ExtensionNode>): TypeComponent {
        val targetType = input.inferenceUtil.inferAs<TypeExpressionNode, PolymorphicType<TypeComponent>>(input.node.targetTypeNode)
        var nInferenceUtil = input.inferenceUtil.derive(self = targetType)

        val constraints = input.node.whereClauses.map {
            nInferenceUtil.inferAs<WhereClauseNode, Constraint<TypeComponent, ConstraintApplication<TypeComponent>>>(it)
        }

        val result = constraints.fold(Pair(nInferenceUtil, IdentityConstraintApplication(targetType) as ConstraintApplication<TypeComponent>), ::refine)

        nInferenceUtil = result.first.derive(self = result.second.resultValue())

        MethodStubPhase.executeAll(nInferenceUtil, input.node.methodDefNodes)
        MethodBodyPhase.executeAll(nInferenceUtil, input.node.methodDefNodes)

        return result.second.resultValue()
    }
}