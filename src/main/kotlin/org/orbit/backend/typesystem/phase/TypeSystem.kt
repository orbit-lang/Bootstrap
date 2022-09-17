package org.orbit.backend.typesystem.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.ProgramNode
import org.orbit.core.phase.Phase
import org.orbit.precess.backend.components.IType
import org.orbit.util.Invocation

object TypeSystem : Phase<ProgramNode, IType.IMetaType<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun execute(input: ProgramNode): IType.IMetaType<*> {
        TODO("Not yet implemented")
    }
}