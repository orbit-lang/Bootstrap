package org.orbit.backend.utils

import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.SourceProvider
import org.orbit.frontend.rules.ProgramRule
import org.orbit.frontend.utils.FrontendUtils
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.frontend.components.nodes.ProgramNode

object BackendUtils {
    fun generatePrecessProgram(sourceProvider: SourceProvider) : ProgramNode {
        val frontendResult = FrontendUtils.parse(sourceProvider, ProgramRule)
        val ast = frontendResult.ast as org.orbit.core.nodes.ProgramNode

        return TypeGenUtil.walk(ast)
    }

    fun generatePrecessSource(sourceProvider: SourceProvider) : String {
        val program = generatePrecessProgram(sourceProvider)

        return program.toString()
    }

    fun check(sourceProvider: SourceProvider) : AnyType {
        val program = FrontendUtils.parse(sourceProvider, ProgramRule)

        CanonicalNameResolver.execute(program)

        return TypeSystem.execute(program.ast as org.orbit.core.nodes.ProgramNode)
    }
}