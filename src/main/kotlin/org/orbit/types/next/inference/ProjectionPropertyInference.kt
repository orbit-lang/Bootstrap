package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.types.next.utils.mapOnly
import org.orbit.types.next.utils.onlyOrNull
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class ProjectedPropertyInferenceContext<N: Node>(val projection: Projection, val clazz: Class<N>) : InferenceContext {
    override val nodeType: Class<out Node> = clazz

    override fun <N : Node> clone(clazz: Class<N>): InferenceContext = this
}

interface ProjectionPropertyInference<W: WhereClauseExpressionNode> : Inference<W, ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member>>

object StoredPropertyInference : ProjectionPropertyInference<AssignmentStatementNode> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: AssignmentStatementNode): InferenceResult {
        val type = inferenceUtil.infer(node.value)

        return StoredProjectedProperty(Field(node.identifier.identifier, type, type))
            .inferenceResult()
    }
}

object ComputedPropertyInference : ProjectionPropertyInference<WhereClauseByExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseByExpressionNode): InferenceResult {
        val projection = (context as? ProjectedPropertyInferenceContext<*>)?.projection
            ?: TODO("@ComputedPropertyInference:38")
        val trait = projection.trait as? Trait
            ?: TODO("@ComputedPropertyInference:40")

        val signature = trait.getTypedContracts<SignatureContract>()
            .map { it.input }
            .firstOrNull { it.getName() == node.identifierNode.identifier }

        val inf = when (signature) {
            null -> inferenceUtil
            else -> {
                val expectedBindings = signature.getParameterTypes()
                val declaredBindings = node.lambdaExpression.bindings

                when (declaredBindings.count()) {
                    0 -> {
                        val nInferenceUtil = inferenceUtil.derive()
                        if (signature.isInstanceMethod) {
                            // TODO - This feels too much like "compiler magic"
                            nInferenceUtil.bind("self", projection.baseType)
                        }

                        nInferenceUtil
                    }
                    expectedBindings.count() -> {
                        val declaredTypes = declaredBindings.mapIndexed { idx, node ->
                            val type = expectedBindings[idx]
                            Pair(node.identifierNode.identifier, type)
                        }

                        val nInferenceUtil = inferenceUtil.derive()

                        declaredTypes.forEach { nInferenceUtil.bind(it.first, it.second) }

                        if (signature.isInstanceMethod) {
                            // TODO - This feels too much like "compiler magic"
                            nInferenceUtil.bind("self", signature.getReceiverType())
                        }

                        nInferenceUtil
                    }
                    else -> {
                        val declaredTypes = declaredBindings.map { inferenceUtil.infer(it.typeNode) }
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

        val contract = trait.contracts.onlyOrNull {
            when (it) {
                is FieldContract -> it.input.memberName == node.identifierNode.identifier
                is SignatureContract -> it.input.getName() == node.identifierNode.identifier
                else -> TODO("!!!")
            }
        }

        return when (contract) {
            null -> throw invocation.make<TypeSystem>("Projection target ${trait.toString(printer)} does not declare projectable member ${printer.apply(node.identifierNode.identifier, PrintableKey.Italics, PrintableKey.Bold)}", node.identifierNode)
            is FieldContract -> ComputedProjectedProperty(contract.input, lambda)
            is SignatureContract -> ProjectedSignatureProperty(contract.input.getName(), trait, lambda)
            else -> TODO("!!!")
        }.inferenceResult()
    }
}

object ProjectionWhereClauseInference : Inference<WhereClauseNode, ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member>> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseNode): InferenceResult = when (node.whereExpression) {
        is AssignmentStatementNode -> StoredPropertyInference.infer(inferenceUtil, context, node.whereExpression)
        is WhereClauseByExpressionNode -> ComputedPropertyInference.infer(inferenceUtil, context, node.whereExpression)
        else -> TODO("Unsupported Where Clause: $node")
    }
}