package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.ITrait
import org.orbit.types.next.components.IType
import org.orbit.types.next.components.Type
import org.orbit.types.next.components.toString
import org.orbit.util.Invocation
import org.orbit.util.Printer

object TraitConformancePhase : TypePhase<TypeDefNode, IType>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<TypeDefNode>): IType {
        val type = input.inferenceUtil.inferAs<TypeDefNode, Type>(input.node)
        val traits = input.node.traitConformances.map {
            val trait = input.inferenceUtil.infer(it)

            when (trait) {
                is ITrait -> trait
                else -> throw invocation.make<TypeSystem>("Type ${type.toString(printer)} cannot conform to non-Trait type ${trait.toString(printer)}", input.node)
            }
        }

        traits.forEach { input.inferenceUtil.addConformance(type, it) }

        return type
    }
}