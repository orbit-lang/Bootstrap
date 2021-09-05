package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.injectResult
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.types.components.Context
import org.orbit.util.partial

class MetaTypeUnit(override val node: MetaTypeNode, override val depth: Int, private val inFuncNamePosition: Boolean = false) : CodeUnit<MetaTypeNode>,
    KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)

    override fun generate(mangler: Mangler) : String {
        val path = node.getPath()

        val separator = when (inFuncNamePosition) {
            true -> "_"
            else -> ", "
        }

        val typeParameters = node.typeParameters
            .map(partial(::TypeExpressionUnit, depth))
            .joinToString(separator, transform = partial(TypeExpressionUnit::generate, mangler))

        val typeName = path.toString(mangler)

        return when (inFuncNamePosition) {
            true -> "${typeName}_$typeParameters"
            else -> "$typeName<$typeParameters>"
        }
    }
}