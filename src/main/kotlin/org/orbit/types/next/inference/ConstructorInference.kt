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
        val args = node.parameterNodes.map { inferenceUtil.infer(it) }
            .toMutableList()

        val source: IType = when (val t = inferenceUtil.infer(node.typeExpressionNode)) {
            is IType -> t
            is PolymorphicType<*> -> {
                // We have enough information here to attempt type inference for constructor calls that do not
                //  explicitly pass Type Parameters at the call-site
                if (args.count() < t.parameters.count())
                    return Never("Attempting to instantiate non-Type (${t.kind.toString(printer)}) ${t.toString(printer)}").inferenceResult()

                val slice = args.subList(0, t.parameters.count()).mapIndexed { idx, item ->
                    Pair(idx, item)
                }

                MonoUtil.monomorphise(inferenceUtil.toCtx(), t, slice, null)
                    .toType(printer) as IType
            }

            else -> return Never("Attempting to instantiate non-Type (${t.kind.toString(printer)}) ${t.toString(printer)}").inferenceResult()
        }

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