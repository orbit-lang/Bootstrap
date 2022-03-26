package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.ReferenceCallNode
import org.orbit.types.next.components.*
import org.orbit.util.Printer

object ReferenceCallInference : Inference<ReferenceCallNode, TypeComponent>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ReferenceCallNode): InferenceResult {
        val lambda = inferenceUtil.inferAs<ExpressionNode, Func>(node.referenceNode)
        val arguments = inferenceUtil.inferAllAs<ExpressionNode, TypeComponent>(node.parameterNodes, AnyExpressionContext)
        val pretty = arguments.joinToString(", ") { it.toString(printer) }

        // NOTE - For now, a zero-parameter Lambda is represented by `(_) -> R` where _ is Never
        // TODO - zero-parameter Lambdas should encode their `takes` as `Unit`
        if (lambda.takes is NeverType && arguments.isEmpty()) return lambda.returns.inferenceResult()
        if (lambda.takes is NeverType) return Never("Cannot invoke Lambda ${lambda.toString(printer)} with arguments $pretty").inferenceResult()

        // Use the same trick as MethodCallInference
        val callableInterface = lambda.derive()
        val calleeFields = arguments.mapIndexed { idx, type -> Field("$idx", type) }
        // TODO - Pass the Type Annotation in via the inference context
        val calleeReturns = Field("__returns", lambda.returns)
        val calleeType = Type(lambda.getPath(OrbitMangler) + "SyntheticCallee", calleeFields + calleeReturns)

        val onFailure = {
            val lambdaPretty = lambda.takes.elements.joinToString(", ") { it.toString(printer) }

            Never("Cannot invoke Lambda ${lambda.toString(printer)} with $pretty, expected $lambdaPretty", node.referenceNode.firstToken.position)
        }

        return when (val r = callableInterface.isImplemented(inferenceUtil.toCtx(), calleeType)) {
            is ContractResult.Success -> lambda.returns
            is ContractResult.Group -> when (r.isSuccessGroup) {
                true -> calleeReturns.type
                else -> onFailure()
            }

            else -> onFailure()
        }.inferenceResult()
    }
}