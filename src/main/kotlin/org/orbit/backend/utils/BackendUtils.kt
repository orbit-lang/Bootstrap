package org.orbit.backend.utils

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.SourceProvider
import org.orbit.frontend.rules.ProgramRule
import org.orbit.frontend.utils.FrontendUtils
import org.orbit.graph.phase.CanonicalNameResolver

object BackendUtils {
    fun check(sourceProvider: SourceProvider) : AnyType {
        val program = FrontendUtils.parse(sourceProvider, ProgramRule)

        CanonicalNameResolver.execute(program)

        return TypeSystem.execute(program.ast as org.orbit.core.nodes.ProgramNode)
    }
}