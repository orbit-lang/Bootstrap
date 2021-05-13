package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.frontend.Parser
import org.orbit.types.IntrinsicTypes
import org.orbit.util.Invocation
import org.orbit.util.partial

private class PropertyDefUnit(override val node: PairNode, override val depth: Int) : CodeUnit<PairNode> {
    override fun generate(mangler: Mangler): String {
        val type = node.getPath().toString(mangler)
        val header = "/* ${node.identifierNode.identifier} $type */"

        return """
            |$header
            |let ${node.identifierNode.identifier} : $type
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}

class TypeDefUnit(override val node: TypeDefNode, override val depth: Int) : CodeUnit<TypeDefNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun generate(mangler: Mangler): String {
        val typePath = node.getPath()

        if (IntrinsicTypes.isIntrinsicType(typePath)) {
            // Intrinsics are auto-defined as typealiases
            return ""
        }

        // TODO - Lookup value semantics for this type (i.e. class or struct)
        val header = "/* type ${typePath.toString(OrbitMangler)} */"
        val typeDef = "struct ${typePath.toString(mangler)}"

        val propertyDefs = node.propertyPairs
            .map(partial(::PropertyDefUnit, depth + 2))
            .map(partial(PropertyDefUnit::generate, mangler))
            .joinToString(newline())

        val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast
        val methodNodes = ast.search<MethodDefNode>()
            .filter { it.signature.receiverTypeNode.getPath().toString(mangler) == typePath.toString(mangler) }

        val funcs = methodNodes
            .map(partial(::MethodDefUnit, depth + 1))
            .map(partial(MethodDefUnit::generate, mangler))
            .joinToString(newline())

        return """
            |$header
            |$typeDef {
            |$propertyDefs
            |}
            |$funcs
        """.trimMargin().prependIndent(indent())
    }
}