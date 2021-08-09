package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.types.components.*
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

class TypeAliasUnit(override val node: TypeAliasNode, override val depth: Int) : CodeUnit<TypeAliasNode> {
    override fun generate(mangler: Mangler): String {
        val sourcePath = node.sourceTypeIdentifier.getPath()
        val targetPath = node.targetTypeIdentifier.getPath()

        val header = "/* type ${sourcePath.toString(OrbitMangler)} = ${targetPath.toString(OrbitMangler)} */"

        return """
            |$header
            |typealias ${sourcePath.toString(mangler)} = ${targetPath.toString(mangler)}
        """.trimMargin()
            .prependIndent(indent())
    }
}

class TypeProjectionUnit(override val node: TypeProjectionNode, override val depth: Int) : CodeUnit<TypeProjectionNode> {
    override fun generate(mangler: Mangler): String {
        val typePath = node.typeIdentifier.getPath()
        val traitPath = node.traitIdentifier.getPath()

        val header = "/* type projection ${typePath.toString(OrbitMangler)} : ${traitPath.toString(OrbitMangler)} */"

        return """
            |$header
            |extension ${typePath.toString(mangler)} : ${traitPath.toString(mangler)} {}
        """.trimMargin()
            .prependIndent(indent())
    }
}

class TraitDefUnit(override val node: TraitDefNode, override val depth: Int) : CodeUnit<TraitDefNode> {
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
        """.trimMargin()
            .prependIndent(indent())
    }
}

class MetaTypeUnit(override val node: MetaTypeNode, override val depth: Int, private val inFuncNamePosition: Boolean = false) : CodeUnit<MetaTypeNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeChecker)

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

class TypeExpressionUnit(override val node: TypeExpressionNode, override val depth: Int, private val inFuncNamePosition: Boolean = false) : CodeUnit<TypeExpressionNode>, KoinComponent {
    override fun generate(mangler: Mangler) : String {
        val path = node.getPath()

        return when (node) {
            is TypeIdentifierNode -> path.toString(mangler)
            is MetaTypeNode -> MetaTypeUnit(node, depth, inFuncNamePosition).generate(mangler)
            else -> TODO("???")
        }
    }
}

class TypeDefUnit(override val node: TypeDefNode, override val depth: Int) : CodeUnit<TypeDefNode>, KoinComponent {
    override fun generate(mangler: Mangler) : String {
        val typePath = node.getPath()

        // TODO - Lookup value semantics for this type (i.e. class or struct)
        val header = "/* type ${typePath.toString(OrbitMangler)} */"
        var adoptedProtocols = node.traitConformances
            .map(partial(::TypeExpressionUnit, depth))
            .joinToString(", ", transform = partial(TypeExpressionUnit::generate, mangler))

        if (node.traitConformances.isNotEmpty()) {
            adoptedProtocols = " : $adoptedProtocols"
        }

        val typeDef = "class ${typePath.toString(mangler)}"

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

class TypeParameterUnit(override val node: TypeIdentifierNode, override val depth: Int) : CodeUnit<TypeIdentifierNode> {
    override fun generate(mangler: Mangler): String {
        return node.value
    }
}

// NOTE - Because generic protocols are dogshit in Swift, its easier to just export as a base class
class TraitConstructorUnit(override val node: TraitConstructorNode, override val depth: Int) : CodeUnit<TraitConstructorNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeChecker)

    override fun generate(mangler: Mangler): String {
        val traitConstructor = node.getType() as TraitConstructor
        val traitPath = node.getPath()
        val typeParametersOrbit = traitConstructor.typeParameters.joinToString(", ", transform = TypeParameter::name)
        val typeParametersSwift = node.typeParameterNodes
            .map(partial(::TypeParameterUnit, depth + 1))
            .map(partial(TypeParameterUnit::generate, mangler))
            .joinToString(", ")

        val header = "/* trait ${traitPath.toString(OrbitMangler)}(${typeParametersOrbit}) */"

        return """
            |$header
            |class ${traitPath.toString(mangler)} <${typeParametersSwift}> {}
        """.trimMargin().prependIndent(indent())
    }
}