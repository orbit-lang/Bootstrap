package org.orbit.backend.typesystem.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.orbit.backend.typesystem.components.AnyMetaType
import org.orbit.backend.typesystem.components.GlobalEnvironment
import org.orbit.backend.typesystem.intrinsics.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ProgramNode
import org.orbit.core.phase.Phase
import org.orbit.util.Invocation

object TypeSystem : Phase<ProgramNode, AnyMetaType>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun execute(input: ProgramNode): AnyMetaType {
        val env = GlobalEnvironment
            .import(OrbCoreNumbers)
            .import(OrbCoreTypes)
            .import(OrbCoreErrors)
            .import(OrbMoreFx)
//            .import(OrbCoreCollections)

        loadKoinModules(module {
            single(named("globalContext")) { env }
        })

        return TypeInferenceUtils.inferAs<ProgramNode, AnyMetaType>(input, env)
    }
}