package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.components.Context
import org.orbit.types.components.Entity
import org.orbit.types.components.IntrinsicTypes
import org.orbit.util.Invocation
import org.orbit.util.partial

private class PropertyDefUnit(override val node: PairNode, override val depth: Int, private val isProtocol: Boolean) : CodeUnit<PairNode> {
    override fun generate(mangler: Mangler): String {
        val type = node.getPath().toString(mangler)
        val header = "/* ${node.identifierNode.identifier} $type */"

        return when (isProtocol) {
            true -> """
            |var ${node.identifierNode.identifier} : $type { get }
            """.trimMargin().prependIndent(indent(depth - 1))
            else -> """
            |$header
            |let ${node.identifierNode.identifier} : $type
        """.trimMargin().prependIndent(indent(depth - 1))
        }
    }
}

class TraitDefUnit(override val node: TraitDefNode, override val depth: Int) : CodeUnit<TraitDefNode>, KoinComponent {
    override fun generate(mangler: Mangler): String {
        val traitPath = node.getPath()

        val header = "/* trait ${traitPath.toString(OrbitMangler)} */"
        val traitDef = "protocol ${traitPath.toString(mangler)}"

        val propertyDefs = node.propertyPairs
            .map(partial(::PropertyDefUnit, depth + 2, true))
            .joinToString(newline(), transform = partial(PropertyDefUnit::generate, mangler))

        return """
            |$header
            |$traitDef {
            |$propertyDefs
            |}
        """.trimMargin().prependIndent(indent())
    }
}

class TypeDefUnit(override val node: TypeDefNode, override val depth: Int) : CodeUnit<TypeDefNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeChecker)

    override fun generate(mangler: Mangler): String {
        val typePath = node.getPath()
        //val typeType = context.getType(typePath) as Entity

//        if (IntrinsicTypes.isIntrinsicType(typePath)) {
//            // Intrinsics are auto-defined as typealiases
//            return ""
//        }

        // TODO - Lookup value semantics for this type (i.e. class or struct)
        val header = "/* type ${typePath.toString(OrbitMangler)} */"
        var adoptedProtocols = node.traitConformances.joinToString(", ") { it.getPath().toString(mangler) }

        if (node.traitConformances.isNotEmpty()) {
            adoptedProtocols = " : $adoptedProtocols"
        }

        val typeDef = "struct ${typePath.toString(mangler)}"

        val propertyDefs = node.getAllPropertyPairs()
            .map(partial(::PropertyDefUnit, depth + 2, false))
            .joinToString(newline(), transform = partial(PropertyDefUnit::generate, mangler))

        return """
            |$header
            |$typeDef$adoptedProtocols {
            |$propertyDefs
            |}
        """.trimMargin().prependIndent(indent())
    }
}