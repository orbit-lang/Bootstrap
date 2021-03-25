package org.orbit.types

import org.json.JSONObject
import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.IdentifierNode
import org.orbit.core.nodes.RValueNode
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser

interface Expression {
    fun infer(context: Context) : Type
}

data class Variable(val name: String) : Expression {
    override fun infer(context: Context) : Type {
        return context.get(name) ?: throw Exception("Failed to infer type of variable '$name'")
    }
}

data class Binary(val op: String, val left: Type, val right: Type) : Expression {
    override fun infer(context: Context) : Type {
        val opFuncName = "${left.name}$op${right.name}"

        return (context.get(opFuncName) as? Lambda)?.outputType
            ?: throw Exception("Failed to infer type of binary expression: '$opFuncName'")
    }
}

data class Assignment(val lhs: String, val rhs: Expression) : Expression {
    override fun infer(context: Context): Type {
        val type = rhs.infer(context)

        context.bind(lhs, type)

        return type
    }
}

data class Call(val func: Lambda, val args: List<Expression>) : Expression {
    internal constructor(func: Lambda, arg: Expression) : this(func, listOf(arg))

    override fun infer(context: Context): Type {
        val argType = args[0].infer(context)
        val equality = StructuralEquality(func.inputType, argType)

        if (!equality.satisfied()) {
            throw Exception("Cannot invoke lambda '${func.name}' with argument type '${argType.name}'")
        }

        if (argType is Lambda) {
            return Call(argType, args.drop(1)).infer(context)
        }

        return func.outputType
    }
}

data class Block(val body: List<Expression>) : Expression {
    override fun infer(context: Context): Type {
        // NOTE - Empty blocks resolve to Unit type
        return body.lastOrNull()?.infer(context) ?: Entity("Unit")
    }
}

class Context(builtIns: Set<Type> = emptySet()) : Serial {
    constructor(builtIns: List<Type>) : this(builtIns.toSet())
    constructor(vararg builtIns: Type) : this(builtIns.toSet())
    internal constructor(vararg builtIns: String) : this(builtIns.map { Entity(it) })

    val types: MutableSet<Type> = builtIns.toMutableSet()
    val bindings = mutableMapOf<String, Type>()
    private var next = 0

    fun bind(name: String, type: Type) {
        bindings[name] = type
        next += 1
    }

    fun add(type: Type) = types.add(type)
    fun get(name: String) : Type? = bindings[name]

    fun remove(name: String) {
        bindings.remove(name)
    }

    fun removeAll(names: List<String>) {
        names.forEach { remove(it) }
    }

    init {
        types.addAll(builtIns)
    }

    override fun describe(json: JSONObject) {
        val typesJson = types.map { Serialiser.serialise(it) }

        json.put("context.types", typesJson)
    }
}

object TypeInferenceUtil {
    fun infer(context: Context, expression: Expression): Type
        = expression.infer(context)

    fun infer(context: Context, expressionNode: ExpressionNode) : Type = when (expressionNode) {
        is IdentifierNode -> infer(context, Variable(expressionNode.identifier))
        is BinaryExpressionNode -> {
            val leftType = infer(context, expressionNode.left)
            val rightType = infer(context, expressionNode.right)

            infer(context, Binary(expressionNode.operator, leftType, rightType))
        }
        is RValueNode -> infer(context, expressionNode.expressionNode)
        else ->
            throw RuntimeException("FATAL - Cannot determine type of expression '${expressionNode::class.java}'")
    }
}

