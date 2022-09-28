package org.orbit.backend.typesystem.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ProgramNode
import org.orbit.core.phase.Phase
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.util.Invocation

object TypeSystem : Phase<ProgramNode, IType.IMetaType<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun execute(input: ProgramNode): IType.IMetaType<*> {
        val env = Env()
            .import(OrbCoreNumbers)
            .import(OrbCoreTypes)

        loadKoinModules(module {
            single(named("globalContext")) { env }
        })

        val result = TypeSystemUtils.inferAs<ProgramNode, IType.IMetaType<*>>(input, env)

        println(env)

        return result
    }
}

fun KoinComponent.globalContext(): Lazy<Env> =
    lazy(KoinPlatformTools.defaultLazyMode()) { get(named("globalContext"), null) }