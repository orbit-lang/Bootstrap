package org.orbit.backend.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.ProgramNode
import org.orbit.core.phase.executeMeasured
import org.orbit.frontend.rules.ProgramRule
import org.orbit.frontend.utils.FrontendUtils
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.Invocation
import org.orbit.util.getKoinInstance

object BackendUtils : KoinComponent {
    private val invocation: Invocation by inject()

    fun check(sourceProvider: SourceProvider) : AnyType {
        val program = FrontendUtils.parse(sourceProvider, ProgramRule)

        CanonicalNameResolver.executeMeasured(invocation, program)

        return TypeSystem.executeMeasured(invocation, program.ast as ProgramNode)
    }

    fun codeGen(sourceProvider: SourceProvider): String {
        val program = FrontendUtils.parse(sourceProvider, ProgramRule)

        CanonicalNameResolver.executeMeasured(invocation, program)

        val typeCheckResult = TypeSystem.executeMeasured(invocation, program.ast as ProgramNode)

        if (!typeCheckResult.toBoolean()) {
            println(typeCheckResult)

            return ""
        }

        val codeGenerator = getKoinInstance<CodeGenUtil>()

        return codeGenerator.generate(program.ast)
    }
}