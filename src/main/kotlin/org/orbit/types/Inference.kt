package org.orbit.types

interface Expression {
    fun infer(context: Context) : Type
}

data class Variable(val name: String) : Expression {
    override fun infer(context: Context) : Type {
        return context.get(name) ?: throw Exception("Failed to infer type of variable '$name'")
    }
}

data class Binary(val op: String, val left: Type, val right: Type) : Expression {
    override fun infer(context: Context): Type {
        val opFuncName = "${left.name}$op${right.name}"

        return context.get(opFuncName) as? Lambda
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

class Context(builtIns: Set<Type> = emptySet()) {
    constructor(builtIns: List<Type>) : this(builtIns.toSet())
    constructor(vararg builtIns: Type) : this(builtIns.toSet())
    internal constructor(vararg builtIns: String) : this(builtIns.map { Entity(it) })

    private val types: MutableSet<Type> = builtIns.toMutableSet()
    private val bindings = mutableMapOf<String, Type>()
    private var next = 0

    fun bind(name: String, type: Type) {
        bindings[name] = type
        next += 1
    }

    fun add(type: Type) = types.add(type)
    fun get(name: String) : Type? = bindings[name]

    init {
        types.addAll(builtIns)
    }
}

object TypeInferenceUtil {
    fun infer(context: Context, expression: Expression): Type
        = expression.infer(context)
}


