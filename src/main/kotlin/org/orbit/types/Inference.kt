package org.orbit.types

import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.nodes.*
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser
import org.orbit.util.Invocation

interface Expression {
    fun infer(context: Context) : Type
}

data class Variable(val name: String) : Expression {
    override fun infer(context: Context) : Type {
        return context.get(name)
            ?: throw Exception("Failed to infer type of variable '$name'")
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
        return body.lastOrNull()?.infer(context) ?: IntrinsicTypes.Unit.type
    }
}

class Context(builtIns: Set<Type> = IntrinsicTypes.allTypes) : Serial, CompilationEventBusAware by CompilationEventBusAwareImpl {
    sealed class Events(override val identifier: String) : CompilationEvent {
        class TypeCreated(type: Type) : Events("(Context) Type Added: ${type.name}")
        class BindingCreated(name: String, type: Type) : Events("(Context) Binding Created: $name -> ${type.name}")
    }

    constructor(builtIns: List<Type>) : this(builtIns.toSet())
    constructor(vararg builtIns: Type) : this(builtIns.toSet())
    internal constructor(vararg builtIns: String) : this(builtIns.map { Entity(it) })

    constructor(other: Context) : this() {
        this.types.addAll(other.types)
        this.bindings.putAll(other.bindings)
    }

    val types: MutableSet<Type> = builtIns.toMutableSet()
    val bindings = mutableMapOf<String, Type>()

    private var next = 0

    init {
        types.addAll(builtIns)
    }

    fun bind(name: String, type: Type) {
        bindings[name] = type
        next += 1

        compilationEventBus.notify(Events.BindingCreated(name, type))
    }

    fun add(type: Type) {
        types.add(type)
        compilationEventBus.notify(Events.TypeCreated(type))
    }

    fun get(name: String) : Type? = bindings[name]

    fun getType(name: String) : Type {
        return getTypeOrNull(name)!!
    }

    fun getType(path: Path) : Type = getType(path.toString(OrbitMangler))
    fun getTypeOrNull(path: Path) : Type? = getTypeOrNull(path.toString(OrbitMangler))

    fun getTypeOrNull(name: String) : Type? {
        val matches = types.filter { it.name == name }

        return when (matches.size) {
            0 -> null
            1 -> matches.first()
            else -> throw RuntimeException("TODO - Multiple types named '$name'")
        }
    }

    fun remove(name: String) {
        bindings.remove(name)
    }

    fun removeAll(names: List<String>) {
        names.forEach { remove(it) }
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

        is TypeIdentifierNode -> {
            context.getType(expressionNode.getPath().toString(OrbitMangler))
        }

        is BinaryExpressionNode -> {
            val leftType = infer(context, expressionNode.left)
            val rightType = infer(context, expressionNode.right)

            infer(context, Binary(expressionNode.operator, leftType, rightType))
        }

        is RValueNode -> infer(context, expressionNode.expressionNode)
        is IntLiteralNode -> IntrinsicTypes.Int.type
        is SymbolLiteralNode -> IntrinsicTypes.Symbol.type
        is InstanceMethodCallNode -> {
            val receiverType = infer(context, expressionNode.receiverNode)
            val functionType = infer(context, expressionNode.methodIdentifierNode) as? Function
                ?: throw RuntimeException("Right-hand side of method call must resolve to a function type")

            val parameterTypes = listOf(receiverType) + expressionNode.parameterNodes.map {
                infer(context, it)
            }

            val argumentTypes = functionType.inputTypes

            if (parameterTypes.size != argumentTypes.size) {
                // TODO - It would be nice to send these errors up to Invocation
                throw RuntimeException("Method '${expressionNode.methodIdentifierNode.identifier}' declares ${argumentTypes.size} arguments (including receiver), found ${parameterTypes.size}")
            }

            for ((idx, pair) in argumentTypes.zip(parameterTypes).withIndex()) {
                // TODO - Nominal vs Structural should be programmable
                // TODO - Named parameters
                // NOTE - For now, parameters must match order of declared arguments 1-to-1
                if (!NominalEquality(pair.first, pair.second).satisfied()) {
                    throw RuntimeException("Method '${expressionNode.methodIdentifierNode.identifier}' declares a parameter of type '${pair.first.name}' at position $idx, found '${pair.second.name}'")
                }

            }

            functionType.outputType
        }

        is ConstructorNode -> ConstructorInference.infer(context, expressionNode)

        else ->
            throw RuntimeException("FATAL - Cannot determine type of expression '${expressionNode::class.java}'")
    }
}

private interface TypeInference<N: Node> {
    fun infer(context: Context, node: N) : Type
}

private object ConstructorInference : TypeInference<ConstructorNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(context: Context, node: ConstructorNode): Type {
        val receiverType = TypeInferenceUtil.infer(context, node.typeIdentifierNode)
        val parameterTypes = receiverType.members

        if (node.parameterNodes.size != parameterTypes.size) {
            throw invocation.make<TypeChecker>("Type '${receiverType.name}' expects ${parameterTypes.size} constructor parameters, found ${node.parameterNodes.size}", node.firstToken.position)
        }

        for ((idx, pair) in parameterTypes.zip(node.parameterNodes).withIndex()) {
            val argumentType = TypeInferenceUtil.infer(context, pair.second)

            if (!NominalEquality(pair.first.type, argumentType).satisfied()) {
                throw invocation.make<TypeChecker>("Constructor expects parameter of type '${pair.first.type.name}' at position ${idx}, found '${argumentType.name}'", pair.second.firstToken.position)
            }
        }

        return receiverType
    }
}

