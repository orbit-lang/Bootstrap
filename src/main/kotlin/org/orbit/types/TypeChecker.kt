package org.orbit.types

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.nodes.*
import org.orbit.frontend.Parser
import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.util.Invocation

interface TypeResolver {
    fun resolve(environment: Environment, context: Context, binding: Binding) : TypeProtocol
}

class TypeChecker(override val invocation: Invocation, private val context: Context = Context()) : AdaptablePhase<Environment, Context>() {
    override val inputType: Class<Environment> = Environment::class.java
    override val outputType: Class<Context> = Context::class.java

    override fun execute(input: Environment): Context {
        val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser).ast
        val typeDefNodes = ast.search(TypeDefNode::class.java)

        typeDefNodes.forEach {
            context.add(Type(it.getPath()))
        }

        input.scopes
            .flatMap { it.bindings }
            .filter { it.kind is Binding.Kind.Entity }
            .forEach {
                val typeDefNode = typeDefNodes.find { n -> n.getPath() == it.path }
                    ?: TODO("@TypeChecker:31")

                val resolver = TypeDefTypeResolver(typeDefNode)

                resolver.resolve(input, context, it)
            }

        val methodResolver = MethodTypeResolver()
        val methods = input.scopes.flatMap { it.bindings.filter { b -> b.kind == Binding.Kind.Method } }

        methods.forEach { methodResolver.resolve(input, context, it) }

        invocation.storeResult(this::class.java.simpleName, context)

        return context
    }
}

class TypeDefTypeResolver(private val typeDefNode: TypeDefNode) : TypeResolver, KoinComponent {
    private val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context, binding: Binding): TypeProtocol {
        var type = Type(typeDefNode.getPath(), emptyList())

        val members = mutableListOf<Property>()
        for (propertyPair in typeDefNode.propertyPairs) {
            val propertyType = context.getType(propertyPair.getPath())

            if (propertyType == type) {
                throw invocation.make<TypeChecker>("Types must not declare properties of their own type: Found property (${propertyPair.identifierNode.identifier} ${propertyType.name}) in type ${type.name}", propertyPair.typeIdentifierNode)
            }

            if (propertyType is Entity) {
                val cyclicProperties = propertyType.properties.filter { it.type == type }

                if (cyclicProperties.isNotEmpty()) {
                    throw invocation.make<TypeChecker>("Detected cyclic definition between type '${type.name}' and its property (${propertyPair.identifierNode.identifier} ${propertyType.name})", propertyPair.typeIdentifierNode)
                }
            }

            members.add(Property(propertyPair.identifierNode.identifier, propertyType))
        }

        type = Type(typeDefNode.getPath(), members)

        context.add(type)

        return type
    }
}

class MethodTypeResolver : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding) : TypeProtocol {
        val parameterBindings = mutableListOf<String>()

        try {
            val methodNodes = environment.ast.search(MethodDefNode::class.java)
                .filter {
                    it.signature.getPathOrNull() == binding.path
                }

            if (methodNodes.size > 1 || methodNodes.isEmpty()) {
                throw TODO("MethodTypeResolver:47")
            }

            val signature = methodNodes[0].signature
            val receiver = signature.receiverTypeNode
            val argTypes = mutableListOf<Parameter>()

            var isInstanceMethod = false
            if (receiver.identifierNode.identifier != "Self") {
                isInstanceMethod = true
                // TODO - Handle Type methods (no instance receiver)
                val t = context.getType(receiver.getPath())

                context.bind(receiver.identifierNode.identifier, t)
                argTypes.add(Parameter(receiver.identifierNode.identifier, t))
            }

            signature.parameterNodes.forEach {
                val t = context.getType(it.getPath())

                context.bind(it.identifierNode.identifier, t)
                parameterBindings.add(it.identifierNode.identifier)

                argTypes.add(Parameter(it.identifierNode.identifier, t))
            }

            val returnType: ValuePositionType = if (signature.returnTypeNode == null) {
                IntrinsicTypes.Unit.type
            } else {
                context.getType(signature.returnTypeNode.getPath()) as ValuePositionType
            }

            val receiverType = context.getType(receiver.getPath()) as ValuePositionType
            val funcType = if (isInstanceMethod) {
                InstanceSignature(signature.identifierNode.identifier, Parameter(receiver.identifierNode.identifier, receiverType), argTypes, returnType)
            } else {
                TypeSignature(signature.identifierNode.identifier, receiverType, argTypes, returnType)
            }

            val body = methodNodes[0].body

            if (body.isEmpty) {
                // Return type is implied to be Unit, check signature agrees
                val equalitySemantics = returnType.equalitySemantics as AnyEquality
                if (!equalitySemantics.isSatisfied(context, returnType, IntrinsicTypes.Unit.type)) {
                    throw Exception("Method '${signature.identifierNode.identifier}' declares a return type of '${returnType.name}', found 'Unit'")
                }
            } else {
                val methodBodyTypeResolver = MethodBodyTypeResolver(body, returnType)

                methodBodyTypeResolver.resolve(environment, context, binding)
            }

            context.bind(signature.identifierNode.identifier, funcType)
            //context.add(funcType)

            return funcType
        } finally {
            // Garbage collect method parameter types
            context.removeAll(parameterBindings)
        }
    }
}

class MethodBodyTypeResolver(private val block: BlockNode, private val returnType: TypeProtocol) : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding) : TypeProtocol {
        // Derive a new scope from the parent scope so we can throw away local bindings when we're done
        val localContext = Context(context)

        for (node in block.body) {
            when (node) {
                is ExpressionNode -> {
                    // TODO - Raise a warning about unused expression value
                    TypeInferenceUtil.infer(localContext, node)
                }

                is AssignmentStatementNode -> AssignmentTypeResolver(node).resolve(environment, localContext, binding)

                is PrintNode -> TypeInferenceUtil.infer(localContext, node.expressionNode)

                is ReturnStatementNode -> {
                    val varExpr = node.valueNode.expressionNode
                    val varType = TypeInferenceUtil.infer(localContext, varExpr, returnType)
                    val equalitySemantics = varType.equalitySemantics as AnyEquality

                    if (!equalitySemantics.isSatisfied(context, returnType, varType)) {
                        throw Exception("Method '${binding.simpleName}' declares a return type of '${returnType.name}', found '${varType.name}'")
                    }
                }
            }
        }

        // All return paths have been evaluated at this point. No conflicts were found,
        // which means its safe to just return the expected return type
        return returnType
    }
}

class AssignmentTypeResolver(private val assignmentStatementNode: AssignmentStatementNode) : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding): TypeProtocol {
        // 1. Ensure we aren't trying to reassign a binding
        val v = context.get(assignmentStatementNode.identifier.identifier)
        if (v != null) {
            // TODO
            throw RuntimeException("FATAL - Attempting to reassign name '${assignmentStatementNode.identifier.identifier}'")
        }

        // 2. Infer the type of the right-hand side
        val rhsType = TypeInferenceUtil.infer(context, assignmentStatementNode.value)

        context.bind(assignmentStatementNode.identifier.identifier, rhsType)

        return IntrinsicTypes.Unit.type
    }
}

class CallTypeResolver(private val callNode: CallNode, private val expectedType: TypeProtocol? = null) : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding): TypeProtocol {
        val receiverType = TypeInferenceUtil.infer(context, callNode.receiverExpression)
        val functionType = TypeInferenceUtil.infer(context, callNode.messageIdentifier) as? Function
            ?: throw RuntimeException("Right-hand side of method call must resolve to a function type")

        // TODO - Infer parameter types from callNode
        val parameterTypes = listOf(receiverType) + callNode.parameterNodes.map {
            TypeInferenceUtil.infer(context, it)
        }

        val argumentTypes = functionType.inputTypes

        if (parameterTypes.size != argumentTypes.size) {
            // TODO - It would be nice to send these errors up to Invocation
            throw RuntimeException("Method '${callNode.messageIdentifier.identifier}' declares ${argumentTypes.size} arguments (including receiver), found ${parameterTypes.size}")
        }

        for ((idx, pair) in argumentTypes.zip(parameterTypes).withIndex()) {
            // TODO - Named parameters
            // NOTE - For now, parameters must match order of declared arguments 1-to-1
            val equalitySemantics = pair.first.equalitySemantics as AnyEquality
            if (!equalitySemantics.isSatisfied(context, pair.first, pair.second)) {
                throw RuntimeException("Method '${callNode.messageIdentifier.identifier}' declares a parameter of type '${pair.first.name}' at position $idx, found '${pair.second.name}'")
            }

        }

        return receiverType
    }
}