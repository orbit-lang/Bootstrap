package org.orbit.types

import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.nodes.*
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser
import org.orbit.util.Invocation
import org.orbit.util.partialAlt
import org.orbit.util.pluralise

interface Expression {
    fun infer(context: Context, typeAnnotation: TypeProtocol? = null) : TypeProtocol
}

data class Variable(val name: String) : Expression {
    override fun infer(context: Context, typeAnnotation: TypeProtocol?) : TypeProtocol {
        return context.get(name)
            ?: throw Exception("Failed to infer type of variable '$name'")
    }
}

data class Binary(val op: String, val left: TypeProtocol, val right: TypeProtocol) : Expression, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(context: Context, typeAnnotation: TypeProtocol?) : TypeProtocol {
        val opFuncName = "${left.name}$op${right.name}"

        var matches = context.types
            .filterIsInstance<Function>()
            .filter { it.inputTypes == listOf(left, right) }

        if (matches.isEmpty()) {
            // TODO - Source position should be retained as Type metadata
            throw invocation.make<TypeChecker>("", SourcePosition.unknown)
        }

        if (matches.size > 1) {
            // We have multiple signatures matching on function parameter types.
            // We need to refine the search by using the type annotation as the expected return type
            if (typeAnnotation == null) {
                val types = matches.flatMap { it.inputTypes.map(TypeProtocol::name) }.joinToString(", ")
                throw invocation.make<TypeChecker>("Multiple candidates found operator with operand types $types", SourcePosition.unknown)
            } else {
                // TODO - We need to account for variance here?
                val sourceEqualitySemantics = typeAnnotation.equalitySemantics as Equality<TypeProtocol>
                val fn = partialAlt(sourceEqualitySemantics::isSatisfied, context, typeAnnotation)

                matches = matches.filter(fn)

                if (matches.size == 1) {
                    // We have a winner!
                    // NOTE - We can't just return typeAnnotation here because that would effectively
                    // erase the concrete operator return type if it is ±variant on typeAnnotation.
                    // e.g. typeAnnotation is Number and 1 + 1 returns Int (which conforms to Number)
                    return matches.first().outputType
                }
            }
        }

        return (context.get(opFuncName) as? Lambda)?.outputType
            ?: throw invocation.make<TypeChecker>("Failed to infer type of binary expression: '$opFuncName'", SourcePosition.unknown)
    }
}

data class Assignment(val lhs: String, val rhs: Expression) : Expression {
    override fun infer(context: Context, typeAnnotation: TypeProtocol?): TypeProtocol {
        val type = rhs.infer(context)

        context.bind(lhs, type)

        return type
    }
}

data class Call(val func: Lambda, val args: List<Expression>) : Expression {
    internal constructor(func: Lambda, arg: Expression) : this(func, listOf(arg))

    override fun infer(context: Context, typeAnnotation: TypeProtocol?): TypeProtocol {
        val argType = args[0].infer(context)
        val equalitySemantics = argType.equalitySemantics as AnyEquality
        val equal = equalitySemantics.isSatisfied(context, func.inputType, argType)

        if (!equal) {
            throw Exception("Cannot invoke lambda '${func.name}' with argument type '${argType.name}'")
        }

        if (argType is Lambda) {
            return Call(argType, args.drop(1)).infer(context)
        }

        return func.outputType
    }
}

data class Block(val body: List<Expression>) : Expression {
    override fun infer(context: Context, typeAnnotation: TypeProtocol?): TypeProtocol {
        // NOTE - Empty blocks resolve to Unit type
        return body.lastOrNull()?.infer(context) ?: IntrinsicTypes.Unit.type
    }
}

class Context(builtIns: Set<TypeProtocol> = IntrinsicTypes.allTypes + IntOperators.all()) : Serial, CompilationEventBusAware by CompilationEventBusAwareImpl {
    sealed class Events(override val identifier: String) : CompilationEvent {
        class TypeCreated(type: TypeProtocol) : Events("(Context) Type Added: ${type.name}")
        class BindingCreated(name: String, type: TypeProtocol) : Events("(Context) Binding Created: $name -> ${type.name}")
    }

    constructor(builtIns: List<TypeProtocol>) : this(builtIns.toSet())
    constructor(vararg builtIns: TypeProtocol) : this(builtIns.toSet())
    internal constructor(vararg builtIns: String) : this(builtIns.map { Type(it) })

    constructor(other: Context) : this() {
        this.types.addAll(other.types)
        this.bindings.putAll(other.bindings)
    }

    val types: MutableSet<TypeProtocol> = builtIns.toMutableSet()
    val bindings = mutableMapOf<String, TypeProtocol>()

    private var next = 0

    init {
        types.addAll(builtIns)
    }

    fun bind(name: String, type: TypeProtocol) {
        bindings[name] = type
        next += 1

        compilationEventBus.notify(Events.BindingCreated(name, type))
    }

    fun add(type: TypeProtocol) {
        types.removeIf { it::class.java == type::class.java && it.name == type.name }
        types.add(type)
        compilationEventBus.notify(Events.TypeCreated(type))
    }

    fun get(name: String) : TypeProtocol? = bindings[name]

    fun getType(name: String) : TypeProtocol {
        return getTypeOrNull(name)!!
    }

    fun getType(path: Path) : TypeProtocol = getType(path.toString(OrbitMangler))
    fun getTypeOrNull(path: Path) : TypeProtocol? = getTypeOrNull(path.toString(OrbitMangler))

    fun getTypeOrNull(name: String) : TypeProtocol? {
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

object TypeInferenceUtil : KoinComponent {
    private val invocation: Invocation by inject()

    fun infer(context: Context, expression: Expression, typeAnnotation: TypeProtocol?): TypeProtocol
        = expression.infer(context, typeAnnotation)

    fun infer(context: Context, expressionNode: ExpressionNode, typeAnnotation: TypeProtocol? = null) : TypeProtocol = when (expressionNode) {
        is IdentifierNode -> infer(context, Variable(expressionNode.identifier), typeAnnotation)

        is TypeIdentifierNode -> {
            // NOTE - HERE: Node has no path
            context.getType(expressionNode.getPath().toString(OrbitMangler))
        }

        is BinaryExpressionNode -> {
            val leftType = infer(context, expressionNode.left)
            val rightType = infer(context, expressionNode.right)

            infer(context, Binary(expressionNode.operator, leftType, rightType), typeAnnotation)
        }

        is RValueNode -> infer(context, expressionNode.expressionNode)
        is IntLiteralNode -> IntrinsicTypes.Int.type
        is SymbolLiteralNode -> IntrinsicTypes.Symbol.type
        is InstanceMethodCallNode -> {
            val receiverType = infer(context, expressionNode.receiverNode)
            val functionType = infer(context, expressionNode.methodIdentifierNode) as? Function
                ?: throw invocation.make<TypeChecker>("Right-hand side of method call must resolve to a function type", expressionNode.firstToken.position)

            val parameterTypes = listOf(receiverType) + expressionNode.parameterNodes.map {
                infer(context, it)
            }

            val argumentTypes = functionType.inputTypes

            if (parameterTypes.size != argumentTypes.size) {
                throw invocation.make<TypeChecker>("Method '${expressionNode.methodIdentifierNode.identifier}' declares ${argumentTypes.size} arguments (including receiver), found ${parameterTypes.size}", expressionNode.firstToken.position)
            }

            for ((idx, pair) in argumentTypes.zip(parameterTypes).withIndex()) {
                // TODO - Named parameters
                // NOTE - For now, parameters must match order of declared arguments 1-to-1
                val equalitySemantics = pair.first.equalitySemantics as AnyEquality
                if (!equalitySemantics.isSatisfied(context, pair.first, pair.second)) {
                    throw invocation.make<TypeChecker>("Method '${expressionNode.methodIdentifierNode.identifier}' declares a parameter of type '${pair.first.name}' at position $idx, found '${pair.second.name}'", expressionNode.firstToken.position)
                }

            }

            functionType.outputType
        }

        is ConstructorNode ->
            ConstructorInference.infer(context, expressionNode)

        else ->
            throw RuntimeException("FATAL - Cannot determine type of expression '${expressionNode::class.java}'")
    }
}

private interface TypeInference<N: Node> {
    fun infer(context: Context, node: N) : TypeProtocol
}

private object ConstructorInference : TypeInference<ConstructorNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(context: Context, node: ConstructorNode): TypeProtocol {
        val receiverType = TypeInferenceUtil.infer(context, node.typeIdentifierNode)

        if (receiverType !is Type) {
            throw invocation.make<TypeChecker>(
                "Only types may be initialised via a constructor call. Found $receiverType",
                node.typeIdentifierNode
            )
        }

        val parameterTypes = receiverType.properties

        if (node.parameterNodes.size != parameterTypes.size) {
            throw invocation.make<TypeChecker>("Type '${receiverType.name}' expects ${parameterTypes.size} constructor ${"parameter".pluralise(parameterTypes.size)}, found ${node.parameterNodes.size}", node.firstToken.position)
        }

        for ((idx, pair) in parameterTypes.zip(node.parameterNodes).withIndex()) {
            val argumentType = TypeInferenceUtil.infer(context, pair.second)
            val equalitySemantics = argumentType.equalitySemantics as AnyEquality

            if (!equalitySemantics.isSatisfied(context, pair.first.type, argumentType)) {
                throw invocation.make<TypeChecker>("Constructor expects parameter of type '${pair.first.type.name}' at position ${idx}, found '${argumentType.name}'", pair.second.firstToken.position)
            }
        }

        return receiverType
    }
}

