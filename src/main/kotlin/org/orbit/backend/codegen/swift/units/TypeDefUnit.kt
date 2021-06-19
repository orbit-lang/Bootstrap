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

class TypeDefUnit(override val node: TypeDefNode, override val depth: Int) : CodeUnit<TypeDefNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeChecker)

    override fun generate(mangler: Mangler): String {
        val typePath = node.getPath()

        // TODO - Lookup value semantics for this type (i.e. class or struct)
        val associatedTypes = mutableListOf<String>()
        val header = "/* type ${typePath.toString(OrbitMangler)} */"
        var adoptedProtocols = node.traitConformances.joinToString(", ") {
            val type = (it.getType() as TypeExpression)

            if (type is MetaType && it is MetaTypeNode) {
                //val traitConstructor = context.getTypeByPath()
                val concreteTypes = type.concreteTypeParameters

                type.entityConstructor.typeParameters.zip(concreteTypes).forEach { p ->
                    val typeParameter = p.first
                    val concreteType = (p.second as TypeExpression).evaluate(context)

                    val tpName = (OrbitMangler + mangler).invoke(typeParameter.name)
                    val ctName = (OrbitMangler + mangler).invoke(concreteType.name)

                    associatedTypes.add("typealias $tpName = $ctName".prependIndent(indent(depth + 1)))
                }
            }

            (OrbitMangler + mangler).invoke((OrbitMangler + mangler).invoke(type.name))
        }

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
            |${associatedTypes.joinToString(newline())}
            |$propertyDefs
            |}
        """.trimMargin().prependIndent(indent())
    }
}

class TypeParameterUnit(override val node: TypeIdentifierNode, override val depth: Int) : CodeUnit<TypeIdentifierNode> {
    override fun generate(mangler: Mangler): String {
        return "associatedtype ${node.value}".prependIndent(indent(depth))
    }
}

class TraitConstructorUnit(override val node: TraitConstructorNode, override val depth: Int) : CodeUnit<TraitConstructorNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeChecker)

    override fun generate(mangler: Mangler): String {
        val traitConstructor = node.getType() as TraitConstructor
        val traitPath = node.getPath()
        val typeParametersOrbit = traitConstructor.typeParameters.joinToString(", ", transform = TypeParameter::name)
        val typeParametersSwift = node.typeParameterNodes
            .map(partial(::TypeParameterUnit, depth + 1))
            .map(partial(TypeParameterUnit::generate, mangler))
            .joinToString(newline())

        val header = "/* trait ${traitPath.toString(OrbitMangler)}(${typeParametersOrbit}) */"

        return """
            |$header
            |protocol ${traitPath.toString(mangler)} {
            |$typeParametersSwift
            |}
        """.trimMargin().prependIndent(indent())
    }
}