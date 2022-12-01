package org.orbit.backend.utils

import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.ProgramNode
import org.orbit.frontend.rules.ProgramRule
import org.orbit.frontend.utils.FrontendUtils
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.getKoinInstance

object BackendUtils {
    fun check(sourceProvider: SourceProvider) : AnyType {
        val program = FrontendUtils.parse(sourceProvider, ProgramRule)

        CanonicalNameResolver.execute(program)

        return TypeSystem.execute(program.ast as ProgramNode)
    }

    fun codeGen(sourceProvider: SourceProvider): String {
        val program = FrontendUtils.parse(sourceProvider, ProgramRule)

        CanonicalNameResolver.execute(program)

        val typeCheckResult = TypeSystem.execute(program.ast as ProgramNode)

        if (!typeCheckResult.toBoolean()) {
            println(typeCheckResult)

            return ""
        }

        val codeGenerator = getKoinInstance<CodeGenUtil>()

        return codeGenerator.generate(program.ast)
    }
}