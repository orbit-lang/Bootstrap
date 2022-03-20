package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.ContractResult
import org.orbit.types.next.components.Type
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.containsInstances

object TraitConformanceVerification : TypePhase<TypeDefNode, Type>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<TypeDefNode>): Type {
        val type = input.inferenceUtil.inferAs<TypeDefNode, Type>(input.node)
        val traits = input.inferenceUtil.getConformance(type)

        if (traits.isEmpty()) return type

        val ctx = input.inferenceUtil.toCtx()
        val start: ContractResult = ContractResult.None
        val result = traits.fold(start) { acc, next ->
            acc + (next.isImplemented(ctx, type))
        }

        return when (result) {
            is ContractResult.None, is ContractResult.Success -> type
            is ContractResult.Failure -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, type), input.node)
            is ContractResult.Group -> when (result.results.containsInstances<ContractResult.Failure>()) {
                true -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, type), input.node)
                else -> type
            }
        }
    }
}