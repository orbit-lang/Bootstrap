package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.next.components.ISignature
import org.orbit.util.Invocation

object MethodStubPhase : TypePhase<MethodDefNode, ISignature>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<MethodDefNode>): ISignature {
        return input.inferenceUtil.inferAs(input.node.signature)
    }
}