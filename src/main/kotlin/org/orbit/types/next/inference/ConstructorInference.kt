package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.ConstructorNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.types.next.components.*
import org.orbit.util.Invocation
import org.orbit.util.Printer

object ConstructorInference : Inference<ConstructorNode, Type>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ConstructorNode): InferenceResult {
        val source = inferenceUtil.infer(node.typeExpressionNode)

        if (source !is IType)
            return Never("Attempting to instantiate non-Type ${source.toString(printer)}").inferenceResult()

        val args = inferenceUtil.inferAllAs<ExpressionNode, IType>(node.parameterNodes, AnyExpressionContext)
            .toMutableList()

        if (args.count() != source.getFields().count()) {
            var recovered = false
            for (pair in source.getFields().withIndex()) {
                if (pair.value.defaultValue == null) continue
                args.add(pair.index, pair.value.type as IType)

                if (args.count() == source.getFields().count()) {
                    recovered = true
                    break
                }
            }

            if (!recovered) {
                // TODO - Multiple constructors
                val pretty = args.joinToString(", ") { it.toString(printer) }
                return Never("Type ${source.toString(printer)} cannot be instantiated with arguments ($pretty)")
                    .inferenceResult()
            }
        }

        val nFields = source.getFields().zip(args).map {
            Field(it.first.name, it.second)
        }

        val ctx = inferenceUtil.toCtx()
        val nType = Type(source.fullyQualifiedName, nFields)
        val nTrait = source.deriveTrait(ctx)

        val result = nTrait.isImplemented(ctx, nType)

        return when (result) {
            ContractResult.None -> source
            is ContractResult.Success -> result.type
            is ContractResult.Failure -> Never("Type Projection error:\n${result.getErrorMessage(printer, source)}")
            is ContractResult.Group -> Never("Type Projection errors:\n${result.getErrorMessage(printer, source)}")
        }.inferenceResult()
    }
}