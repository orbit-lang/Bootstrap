package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.common.AbstractTypeAliasUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.TypeAliasNode
import org.orbit.types.components.Context
import org.orbit.types.components.TypeExpression
import kotlin.math.abs

class TypeAliasUnit(override val node: TypeAliasNode, override val depth: Int) : AbstractTypeAliasUnit {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    override fun generate(mangler: Mangler): String {
        val sourcePath = node.sourceTypeIdentifier.getPath()
        val relativeTargetPath = node.targetTypeIdentifier.getPath()
        val absoluteTargetPath = node.targetTypeIdentifier.getType().getFullyQualifiedPath()
        val targetType = (node.targetTypeIdentifier.getType() as TypeExpression).evaluate(context)
        val targetTypeNameSwift = mangler.mangle(absoluteTargetPath)

        val targetPath = OrbitMangler.unmangle(node.targetTypeIdentifier.getType().name)
        val nSourcePath = sourcePath + targetPath.drop(relativeTargetPath)

        val header = "/* type ${nSourcePath.toString(OrbitMangler)} = ${targetType.name} */"

        return """
            |$header
            |typealias ${nSourcePath.toString(mangler)} = $targetTypeNameSwift
        """.trimMargin()
            .prependIndent(indent())
    }
}