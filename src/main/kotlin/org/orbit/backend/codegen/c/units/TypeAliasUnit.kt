package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.common.AbstractTypeAliasUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.TypeAliasNode
import org.orbit.types.components.Context
import org.orbit.types.components.TypeExpression

class TypeAliasUnit(override val node: TypeAliasNode, override val depth: Int) : AbstractTypeAliasUnit {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    override fun generate(mangler: Mangler): String {
        val sourcePath = node.sourceTypeIdentifier.getPath()
        val targetType = (node.targetTypeIdentifier.getType() as TypeExpression).evaluate(context)
        val targetTypeNameSwift = (OrbitMangler + mangler).invoke(targetType.name)

        val header = "/* type ${sourcePath.toString(OrbitMangler)} = ${targetType.name} */"

        return """
            |$header
            |typedef $targetTypeNameSwift ${sourcePath.toString(mangler)};
        """.trimMargin()
            .prependIndent(indent())
    }
}