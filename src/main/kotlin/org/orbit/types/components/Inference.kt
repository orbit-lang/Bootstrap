package org.orbit.types.components

import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.*
import org.orbit.core.components.CompilationEvent
import org.orbit.core.components.CompilationEventBusAware
import org.orbit.core.components.CompilationEventBusAwareImpl
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation
import org.orbit.util.partial
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

data class Unary(val op: String, val operand: TypeProtocol) : Expression, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(context: Context, typeAnnotation: TypeProtocol?): TypeProtocol {
        var matches = context.types
            .filterIsInstance<PrefixOperator>()
            .filter { it.operandType == operand }

        if (matches.isEmpty()) {
            throw invocation.make<TypeChecker>("Cannot find binary operator matching signature '$op${operand.name}'", SourcePosition.unknown)
        }

        if (matches.size > 1) {
            matches = matches.filter { it.symbol == op }

            if (matches.size == 1) {
                val resultType = matches.first().resultType

                if (typeAnnotation != null) {
                    val equalitySemantics = typeAnnotation.equalitySemantics as AnyEquality

                    if (!equalitySemantics.isSatisfied(context, typeAnnotation, resultType)) {
                        throw invocation.make<TypeChecker>("Type '${resultType.name} is not equal to type '${typeAnnotation.name}' using equality semantics '${equalitySemantics}", SourcePosition.unknown)
                    }
                }

                return resultType
            }
        }

        throw invocation.make<TypeChecker>("Failed to infer type of unary expression: '$op${operand.name}'", SourcePosition.unknown)
    }
}

data class Binary(val op: String, val left: TypeProtocol, val right: TypeProtocol) : Expression, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(context: Context, typeAnnotation: TypeProtocol?) : TypeProtocol {
        var matches = context.types
            .filterIsInstance<InfixOperator>()
            .filter { it.leftType == left && it.rightType == right }

        if (matches.isEmpty()) {
            // TODO - Source position should be retained as Type metadata
            throw invocation.make<TypeChecker>("Cannot find binary operator matching signature '${left.name} $op ${right.name}'", SourcePosition.unknown)
        }

        if (matches.size > 1) {
            // We have multiple signatures matching on function parameter types.
            // We need to refine the search by using the type annotation as the expected return type

            // TODO - We need to account for variance here?
            matches = matches.filter { it.symbol == op }

            if (matches.size == 1) {
                // We have a winner!
                // NOTE - We can't just return typeAnnotation here because that would effectively
                // erase the concrete operator return type if it is ±variant on typeAnnotation.
                // e.g. typeAnnotation is Number and 1 + 1 returns Int (which conforms to Number)
                val resultType = matches.first().resultType

                if (typeAnnotation != null) {
                    val equalitySemantics = typeAnnotation.equalitySemantics as AnyEquality

                    if (!equalitySemantics.isSatisfied(context, typeAnnotation, resultType)) {
                        throw invocation.make<TypeChecker>("Type '${resultType.name} is not equal to type '${typeAnnotation.name}' using equality semantics '${equalitySemantics}", SourcePosition.unknown)
                    }
                }

                return resultType
            }
        }

        throw invocation.make<TypeChecker>("Failed to infer type of binary expression: '${left.name} $op ${right.name}'", SourcePosition.unknown)
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

    fun addAll(types: List<TypeProtocol>) = types.forEach(::add)

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

        is UnaryExpressionNode -> {
            val operand = infer(context, expressionNode.operand)

            infer(context, Unary(expressionNode.operator, operand), typeAnnotation)
        }

        is RValueNode -> infer(context, expressionNode.expressionNode)
        is IntLiteralNode -> IntrinsicTypes.Int.type
        is SymbolLiteralNode -> IntrinsicTypes.Symbol.type
        is CallNode -> {
            val receiverType = infer(context, expressionNode.receiverExpression)
                as? Entity
                ?: throw invocation.make<TypeChecker>("Only entity types may appear on the left-hand side of a call expression", expressionNode.receiverExpression)

            if (expressionNode.isPropertyAccess) {
                val matches = receiverType.properties.filter { it.name == expressionNode.messageIdentifier.identifier }

                if (matches.isEmpty()) {
                    throw invocation.make<TypeChecker>("Type '${receiverType.name} has no property named '${expressionNode.messageIdentifier.identifier}", expressionNode.messageIdentifier)
                } else if (matches.size > 1) {
                    throw invocation.make<TypeChecker>("Type '${receiverType.name} has multiple properties named '${expressionNode.messageIdentifier.identifier}", expressionNode.messageIdentifier)
                }

                matches.first().type
            } else {
                val receiverType = infer(context, expressionNode.receiverExpression)
                val parameterTypes = expressionNode.parameterNodes.map(partialAlt(TypeInferenceUtil::infer, context))

                val matches = mutableListOf<SignatureProtocol<*>>()
                for (binding in context.bindings.values) {
                    if (binding is InstanceSignature) {
                        val phantomSignature = InstanceSignature(expressionNode.messageIdentifier.identifier, Parameter("", receiverType), listOf(
                            Parameter("", receiverType)
                        ) + parameterTypes.map(
                            partialAlt(::Parameter, "")
                        ), IntrinsicTypes.Unit.type)

                        val receiverSemantics = binding.receiver.type.equalitySemantics as AnyEquality

                        if (receiverSemantics.isSatisfied(context, binding.receiver.type, receiverType)) {
                            val all = binding.parameters
                                .map(Parameter::type)
                                .zip(parameterTypes)
                                .all {
                                    val semantics = it.first.equalitySemantics as AnyEquality
                                    semantics.isSatisfied(context, it.first, it.second)
                                }

                            if (all) {
                                matches.add(binding)
                            }
                        }
                    } else if (binding is TypeSignature) {
                        val receiverSemantics = binding.receiver.equalitySemantics as AnyEquality

                        if (receiverSemantics.isSatisfied(context, binding.receiver, receiverType)) {
                            val all = binding.parameters
                                .map(Parameter::type)
                                .zip(parameterTypes)
                                .all {
                                    val semantics = it.first.equalitySemantics as AnyEquality
                                    semantics.isSatisfied(context, it.first, it.second)
                                }

                            if (all) {
                                matches.add(binding)
                            }
                        }
                    }
                }

                if (matches.isEmpty()) {
                    throw invocation.make<TypeChecker>("Receiver type '${receiverType.name}' does not respond to message '${expressionNode.messageIdentifier.identifier}'", expressionNode.messageIdentifier)
                } else if (matches.size > 1) {
                    // TODO - Introduce some syntactic construct to manually allow differentiation in cases
                    //  where 2 or more methods exist with the same name, same receiver & same parameters, but differ in the return type
                    val candidates = matches.joinToString("\n\t\t",
                        transform = partial(SignatureProtocol<*>::toString, OrbitMangler)
                    )

                    throw invocation.make<TypeChecker>("Ambiguous method call '${expressionNode.messageIdentifier.identifier}' on receiver type '${receiverType.name}'. Found multiple candidates: \n\t\t${candidates}", expressionNode.messageIdentifier)
                }

                expressionNode.annotate(matches.first(), Annotations.Type)

                receiverType
            }
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

