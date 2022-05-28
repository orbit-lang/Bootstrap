package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.types.next.utils.mapOnly
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

interface ProjectionPropertyInference<W: WhereClauseExpressionNode> : Inference<W, ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member>>

object StoredPropertyInference : ProjectionPropertyInference<AssignmentStatementNode> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: AssignmentStatementNode): InferenceResult {
        val type = inferenceUtil.infer(node.value)

        return StoredProjectedProperty(Field(node.identifier.identifier, type, node.value))
            .inferenceResult()
    }
}

object ComputedPropertyInference : ProjectionPropertyInference<WhereClauseByExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseByExpressionNode): InferenceResult {
        val trait = (context as? TypeAnnotatedInferenceContext<*>)?.typeAnnotation as? Trait
            ?: TODO("@ComputedPropertyInference:21")
        val signature = trait.getTypedContracts<SignatureContract>()
            .map { it.input }
            .firstOrNull { it.getName() == node.identifierNode.identifier }

        val inf = when (signature) {
            null -> inferenceUtil
            else -> {
                val expectedBindings = signature.getParameterTypes()
                val declaredBindings = node.lambdaExpression.bindings

                when (declaredBindings.count()) {
                    0 -> inferenceUtil
                    expectedBindings.count() -> {
                        val declaredTypes = declaredBindings.mapIndexed { idx, node ->
                            val type = expectedBindings[idx]
                            Pair(node.identifierNode.identifier, type)
                        }

                        val nInferenceUtil = inferenceUtil.derive()

                        declaredTypes.forEach { nInferenceUtil.bind(it.first, it.second) }

                        nInferenceUtil
                    }
                    else -> {
                        val declaredTypes = declaredBindings.map { inferenceUtil.infer(it.typeExpressionNode) }
                        val prettyExpected = expectedBindings.joinToString(", ") { Field("_", it).toString(printer) }
                        val prettyDeclared = declaredBindings.mapIndexed { index, pair -> "(${pair.identifierNode.identifier}: ${declaredTypes[index].toString(printer)})" }
                            .joinToString(", ")

                        throw invocation.make<TypeSystem>("Projected Signatures must declare Lambda bindings for all parameters, or none at all.\n\tExpected $prettyExpected\n\tFound $prettyDeclared", node.lambdaExpression.firstToken)
                    }
                }
            }
        }

        val lambda = inf.inferAs<LambdaLiteralNode, Func>(node.lambdaExpression)

        if (signature != null) {
            if (!AnyEq.weakEq(inf.toCtx(), signature.getReturnType(), lambda.returns))
                throw invocation.make<TypeSystem>("Projected Signature ${printer.apply(node.identifierNode.identifier, PrintableKey.Bold, PrintableKey.Italics)} must return expected Type ${signature.getReturnType().toString(printer)}, found ${lambda.returns.toString(printer)}", node.lambdaExpression)
        }

        return trait.contracts.mapOnly({ when (val contract = it) {
            is FieldContract -> contract.input.memberName == node.identifierNode.identifier
            is SignatureContract -> contract.input.getName() == node.identifierNode.identifier
            else -> TODO("!!!")
        }}) { when (it) {
            is FieldContract -> ComputedProjectedProperty(it.input, lambda)
            is SignatureContract -> ProjectedSignatureProperty(it.input.getName(), trait, lambda)
            else -> TODO("!!!")
        }}.inferenceResult()
    }
}

object ProjectionWhereClauseInference : Inference<WhereClauseNode, ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member>> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseNode): InferenceResult = when (node.whereExpression) {
        is AssignmentStatementNode -> StoredPropertyInference.infer(inferenceUtil, context, node.whereExpression)
        is WhereClauseByExpressionNode -> ComputedPropertyInference.infer(inferenceUtil, context, node.whereExpression)
        else -> TODO("Unsupported Where Clause: $node")
    }
}