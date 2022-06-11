package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.ConstructorNode
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.Result

object ConstructorInference : Inference<ConstructorNode, Type>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun checkContexts(inferenceUtil: InferenceUtil, node: ConstructorNode, poly: PolymorphicType<*>, given: List<Pair<Int, TypeComponent>>) {
        val contexts = inferenceUtil.getContexts(poly)

        if (contexts.isNotEmpty()) {
            val context = contexts.map { it.context }.reduce { acc, next -> when (val r = acc.merge(next)) {
                is Result.Success -> r.value
                is Result.Failure -> throw invocation.make<TypeSystem>(r.reason.message, node.typeExpressionNode)
            }}

            val nContext = poly.parameters.zip(given).fold(context) { acc, next -> acc.sub(next.first, next.second.second) }
            val solution = nContext.solve(inferenceUtil.toCtx())

            if (solution is Never) throw invocation.make<TypeSystem>(solution.message, node.typeExpressionNode)
        }
    }

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ConstructorNode): InferenceResult {
        val args = node.parameterNodes.map { inferenceUtil.infer(it) }
            .toMutableList()

        val source: IType = when (val t = inferenceUtil.infer(node.typeExpressionNode)) {
            is MonomorphicType<*> -> {
                val slice = t.concreteParameters.map { Pair(it.index, it.concreteType) }

                checkContexts(inferenceUtil, node, t.polymorphicType, slice)

                t
            }
            is IType -> t
            is PolymorphicType<*> -> {
                if (args.count() < t.parameters.count())
                    return Never("Attempting to instantiate non-Type (${t.kind.toString(printer)}) ${t.toString(printer)}").inferenceResult()

                val slice = args.subList(0, t.parameters.count()).mapIndexed { idx, item ->
                    Pair(idx, item)
                }

                checkContexts(inferenceUtil, node, t, slice)

                MonoUtil.monomorphise(inferenceUtil.toCtx(), t, slice, null)
                    .toType(printer) as IType
            }

            else -> return Never("Attempting to instantiate non-Type (${t.kind.toString(printer)}) ${t.toString(printer)}").inferenceResult()
        }

        val argsClone = args.toList()
        for (arg in argsClone.withIndex()) {
            if (arg.value === Type.hole) {
                val member = source.getMembers()[arg.index]

                if (member !is Field) throw invocation.make<TypeSystem>("Type ${source.toString(printer)} does not declare a default value for field ${member.toString(printer)}", SourcePosition.unknown)
                if (member.defaultValue == null) throw invocation.make<TypeSystem>("Type ${source.toString(printer)} does not declare a default value for field ${member.toString(printer)}", SourcePosition.unknown)

                args[arg.index] = member.defaultValue
            }
        }

        // NOTE - We only need as many args as there are declared Fields on the Type under construction
        val fields = source.getMembers().filterIsInstance<Field>()
        if (args.count() != fields.count()) {
            var recovered = false
            for (pair in fields.withIndex()) {
                val member = pair.value
                if (member.defaultValue == null) continue

                val idx = source.getMembers().indexOfFirst { it.memberName == member.memberName }
                val constructorMembers = source.getMembers().filter { it is Field || it is Property }
                if (idx != pair.index) {
                    val delta = idx - pair.index
                    for (i in IntRange(0, delta - 1)) {
                        args.add(i, constructorMembers[i].type)
                    }
                }

                args.add(idx, member.type as IType)

                if (args.count() == constructorMembers.count()) {
                    recovered = true
                    break
                }
            }

            if (!recovered) {
                // TODO - Multiple constructors
                val pretty = args.joinToString(", ") { it.toString(printer) }
                return Never("Type ${source.toString(printer)} cannot be instantiated with arguments ($pretty)", node.firstToken.position)
                    .inferenceResult()
            }
        }

        val nFields = source.getMembers().zip(args).map {
            Field(it.first.memberName, it.second)
        }

        val ctx = inferenceUtil.toCtx()
        val nType = Type(source.fullyQualifiedName, nFields)
        val nTrait = source.deriveTrait(ctx)

        val result = nTrait.isImplemented(ctx, nType)

        return when (result) {
            ContractResult.None -> source
            is ContractResult.Success -> source
            is ContractResult.Failure -> Never("Type Projection error:\n\t${result.getErrorMessage(printer, source)}")
            is ContractResult.Group -> Never("Type Projection errors:\n\t${result.getErrorMessage(printer, source)}")
        }.inferenceResult()
    }
}