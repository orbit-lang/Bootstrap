package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation
import kotlin.math.exp

private object LambdaLiteralBodyRule : ParseRule<BlockNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val node = context.attemptAny(listOf(BlockRule.lambda, ExpressionRule.singleExpressionBodyRule))
            ?: return ParseRule.Result.Failure.Abort

        val body = when (node) {
            is BlockNode -> node
            is IExpressionNode -> node.toBlockNode()
            else -> throw invocation.make<Parser>("Expected either a block or a single expression as Lambda body", node)
        }

        return +body
    }
}

private object ParameterlessLambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()

        if (start.type != TokenTypes.LBrace) return ParseRule.Result.Failure.Abort

        val body = context.attempt(BlockRule.lambda)
            ?: return ParseRule.Result.Failure.Abort

        return +LambdaLiteralNode(start, body.lastToken, emptyList(), body)
    }
}

private object SingleParameterLambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val start = context.peek()

        // TODO - We need to be able to explicitly state the Receiver of a Lambda

        val bindings = when (start.type) {
            TokenTypes.Identifier -> when (start.text) {
                "_" -> emptyList()
                else -> {
                    val name = context.attempt(IdentifierRule)!!
                    val type = TypeIdentifierNode.any()
                    val next = context.peek()

                    if (next.type != TokenTypes.In) return ParseRule.Result.Failure.Rewind(listOf(name.firstToken))

                    listOf(ParameterNode(start, start, name, type, null))
                }
            }

            else -> return ParseRule.Result.Failure.Abort
        }

        val recorded = context.end()

        if (context.peek().type != TokenTypes.In) return ParseRule.Result.Failure.Rewind(recorded)

        context.expect(TokenTypes.In)

        val body = context.attempt(LambdaLiteralBodyRule)
            ?: return ParseRule.Result.Failure.Abort

        return +LambdaLiteralNode(start, body.lastToken, bindings, body)
    }
}

private object LambdaBindingsRule : ParseRule<LambdaBindingsNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val next = context.peek()

        if (next.text == "->") return +LambdaBindingsNode(next, next, emptyList())

        val expectClose = when (next.type) {
            TokenTypes.LParen -> {
                context.expect(TokenTypes.LParen)
                true
            }
            else -> false
        }

        val separator = SeparatedRule(innerRule = ParameterRule(true))
        val separated = context.attempt(separator)
            ?: return ParseRule.Result.Failure.Rewind(collector)
        val bindings = separated.nodes

        if (expectClose) {
            context.expect(TokenTypes.RParen)
        }

        val op = context.expect(TokenTypes.OperatorSymbol)

        if (op.text != "->") return ParseRule.Result.Failure.Throw("Expected `->` between lambda bindings and body", op)

        return +LambdaBindingsNode(separated.firstToken, separated.lastToken, bindings)
    }
}

private object UntypedParametersLambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val start = context.expect(TokenTypes.LBrace)
        val bindings = context.attempt(LambdaBindingsRule)

        if (bindings == null) {
            context.rewind(collector)
        }

        val body = mutableListOf<INode>()
        var next = context.peek()
        while (next.type != TokenTypes.RBrace) {
            val node = context.attemptAny(
                listOf(
                    CauseRule,
                    CheckRule,
                    MirrorRule,
                    TypeOfRule,
                    RefOfRule,
                    ContextOfRule,
                    DeferRule,
                    PrintRule,
                    AssignmentRule,
                    InvocationRule,
                    ExpressionRule.defaultValue
                )
            ) ?: return ParseRule.Result.Failure.Abort

            body.add(node)

            next = context.peek()
        }

        val end = context.expect(TokenTypes.RBrace)

        return +LambdaLiteralNode(start, end, bindings?.bindings ?: emptyList(), BlockNode(start, end, body))
    }
}

private object TypedParametersLambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ParameterRule())
        val delimResult = context.attempt(delim)
        val recorded = context.end()

        if (delimResult == null) return ParseRule.Result.Failure.Rewind(recorded)
        if (context.peek().type != TokenTypes.In) return ParseRule.Result.Failure.Rewind(recorded)

        context.expect(TokenTypes.In)

        val bindings = delimResult.nodes
        val body = context.attempt(LambdaLiteralBodyRule)
            ?: return ParseRule.Result.Failure.Abort

        return +LambdaLiteralNode(delimResult.firstToken, body.lastToken, bindings, body)
    }
}

object LambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val node = context.attemptAny(listOf(UntypedParametersLambdaLiteralRule)) //listOf(ParameterlessLambdaLiteralRule, SingleParameterLambdaLiteralRule, TypedParametersLambdaLiteralRule, UntypedParametersLambdaLiteralRule))
            as? LambdaLiteralNode
            ?: return ParseRule.Result.Failure.Abort

        return +node
    }
}