package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeAliasNode
import org.orbit.types.next.components.Never
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.next.Alias
import org.orbit.util.next.IAlias

object TypeAliasPhase : TypePhase<TypeAliasNode, IAlias>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeAliasNode>): IAlias {
        val target = input.inferenceUtil.infer(input.node.targetTypeIdentifier)

        return Alias(input.node.getPath(), target)
    }
}