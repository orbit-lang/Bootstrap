package org.orbit.backend.typegen.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.phase.Phase
import org.orbit.backend.typesystem.components.IType
import org.orbit.precess.backend.utils.PrecessUtils
import org.orbit.precess.frontend.components.nodes.ProgramNode
import org.orbit.util.Invocation

object TypeGen : Phase<org.orbit.core.nodes.ProgramNode, IType.IMetaType<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun execute(input: org.orbit.core.nodes.ProgramNode): IType.IMetaType<*> {
        val program = TypeGenUtil.walk<org.orbit.core.nodes.ProgramNode, ProgramNode>(input)

        return PrecessUtils.run(program)
    }
}