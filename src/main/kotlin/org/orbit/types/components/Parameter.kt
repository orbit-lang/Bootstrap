package org.orbit.types.components

import jdk.nashorn.internal.parser.TokenType
import org.orbit.core.OrbitMangler
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.nodes.IdentifierNode
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.util.Printer

data class Parameter(override val name: String, val type: TypeProtocol) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = type.equalitySemantics
    override val kind: TypeKind = NullaryType

    fun toNode(context: Context) : PairNode {
        val nameToken = Token(TokenTypes.Identifier, name, SourcePosition.unknown)
        val typeToken = Token(TokenTypes.TypeIdentifier, type.name, SourcePosition.unknown)
        val typePath = OrbitMangler.unmangle(type.name)
        val type = context.getTypeByPath(typePath)

        val nameNode = IdentifierNode(nameToken, nameToken, name)
        val typeNode = TypeIdentifierNode(typeToken, typeToken, type.name)

        typeNode.annotate(typePath, Annotations.Path)
        typeNode.annotate(type, Annotations.Type)

        val node = PairNode(nameToken, typeToken, nameNode, typeNode)

        node.annotate(typePath, Annotations.Path)
        node.annotate(type, Annotations.Type)

        return node
    }

    override fun toString(printer: Printer): String {
        return "${name}: ${type.toString(printer)}"
    }
}