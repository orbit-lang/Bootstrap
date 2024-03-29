package org.orbit.frontend.rules

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ModuleRule : ParseRule<ModuleNode> {
    sealed class Errors {
        data class MissingName(override val sourcePosition: SourcePosition)
            : ParseError("Module definition requires a name", sourcePosition)
    }

    override fun parse(context: Parser) : ParseRule.Result {
        val start = context.expect(TokenTypes.Module)
        val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
            ?: throw context.invocation.make(Errors.MissingName(start.position))

        if (!context.hasMore) {
            return +ModuleNode(start, typeIdentifierNode.lastToken, emptyList(), typeIdentifierNode, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), operatorDefs = emptyList(), attributeDefs = emptyList(), typeEffects = emptyList(), effects = emptyList())
        }

        var next = context.peek()
        val implements = mutableListOf<TypeIdentifierNode>()

        if (next.type == TokenTypes.Colon) {
            context.consume()

            while (true) {
                val expr = LiteralRule(TypeIdentifierRule.RValue).execute(context)
                    .asSuccessOrNull<RValueNode>()
                    ?: return ParseRule.Result.Failure.Abort

                val impl = expr.node.expressionNode as? TypeIdentifierNode
                    ?: TODO("@ModuleRule:50")

                implements.add(impl)

                next = context.peek()

                if (next.type == TokenTypes.Comma) {
                    context.consume()
                    next = context.peek()
                } else if (next.type != TokenTypes.TypeIdentifier) {
                    break
                }
            }
        }

        next = context.peek()

        var withinNode: TypeIdentifierNode? = null
        if (next.type == TokenTypes.Within) {
            withinNode = context.attempt(WithinRule)
        }

        next = context.peek()

        val withNodes = mutableListOf<TypeIdentifierNode>()
        if (next.type == TokenTypes.With) {
            var with = context.attempt(ImportRule)

            while (with != null) {
                withNodes.add(with)
                with = context.attempt(ImportRule)
            }
        }

        if (!context.hasMore) {
            return +ModuleNode(start, withNodes.lastOrNull()?.lastToken ?: next, implements, typeIdentifierNode, withinNode, withNodes, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), operatorDefs = emptyList(), attributeDefs = emptyList(), typeEffects = emptyList(), effects = emptyList())
        }

        if (context.peek().type != TokenTypes.LBrace) {
            return +ModuleNode(start, withNodes.lastOrNull()?.lastToken ?: next, implements, typeIdentifierNode, withinNode, withNodes, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), operatorDefs = emptyList(), attributeDefs = emptyList(), typeEffects = emptyList(), effects = emptyList())
        }

        context.expect(TokenTypes.LBrace)

        val entityDefNodes = mutableListOf<EntityDefNode>()
        val typeAliasNodes = mutableListOf<TypeAliasNode>()
        val methodDefNodes = mutableListOf<MethodDefNode>()
        val typeProjectionNodes = mutableListOf<ProjectionNode>()
        val extensionNodes = mutableListOf<ExtensionNode>()
        val contextNodes = mutableListOf<ContextNode>()
        val operatorDefNodes = mutableListOf<OperatorDefNode>()
        val attributeDefNodes = mutableListOf<AttributeDefNode>()
        val typeEffectDefNodes = mutableListOf<TypeEffectNode>()
        val effectNodes = mutableListOf<EffectNode>()

        next = context.peek()

        while (next.type != TokenTypes.RBrace) {
            when (next.type) {
                TokenTypes.Effect -> {
                    val effect = context.attempt(EffectRule)
                        ?: TODO("ModuleRule:EffectRule")

                    effectNodes.add(effect)
                }

                TokenTypes.TypeEffect -> {
                    val typeEffect = context.attempt(TypeEffectRule)
                        ?: TODO("ModuleRule:TypeEffectRule")

                    typeEffectDefNodes.add(typeEffect)
                }

                TokenTypes.Attribute -> {
                    val attribute = context.attempt(AttributeDefRule)
                        ?: TODO("ModuleRule:AttributeDefRule")

                    attributeDefNodes.add(attribute)
                }

                TokenTypes.Prefix, TokenTypes.Infix, TokenTypes.Postfix -> {
                    val op = context.attempt(OperatorDefRule)
                        ?: TODO("ModuleRule:OperatorDef")

                    operatorDefNodes.add(op)
                }

                TokenTypes.Context -> {
                    val contextNode = context.attempt(ContextRule)
                        ?: TODO("ModuleRule:Context: ${context.peek()}")

                    contextNodes.add(contextNode)
                }

                TokenTypes.Fun -> {
                    val methodDefNode = context.attempt(MethodDefRule, true)
                        ?: throw Exception("Expected method signature following '(' at container level")

                    methodDefNodes.add(methodDefNode)
                }

                TokenTypes.Type, TokenTypes.Trait, TokenTypes.Family -> {
                    val entity = context.attemptAny(EntityDefParseRule.moduleTopLevelRules, true)

                    when (entity) {
                        is TypeAliasNode -> typeAliasNodes.add(entity)
                        else -> entityDefNodes.add(entity!! as EntityDefNode)
                    }
                }

                TokenTypes.Projection -> {
                    val projection = context.attempt(ProjectionRule)
                        ?: TODO("@ModuleRule:105")

                    typeProjectionNodes.add(projection)
                }

                TokenTypes.Extension -> {
                    val extension = context.attempt(ExtensionRule, true)
                        ?: TODO("@ModuleRule:112")

                    extensionNodes.add(extension)
                }

                TokenTypes.Alias -> {
                    val typeAlias = context.attempt(TypeAliasRule)
                        ?: TODO("@ModuleRule:119")

                    typeAliasNodes.add(typeAlias)
                }

                else -> throw context.invocation.make<Parser>("Unexpected top-level lexeme: `${next.text}`", next)
            }

            next = context.peek()
        }

        val end = context.expect(TokenTypes.RBrace)

        return +ModuleNode(start, end, implements, typeIdentifierNode, withinNode, withNodes, entityDefNodes, methodDefNodes, typeAliasNodes, typeProjectionNodes, extensionNodes, contextNodes, operatorDefs = operatorDefNodes, attributeDefs = attributeDefNodes, typeEffects = typeEffectDefNodes, effects = effectNodes)
    }
}