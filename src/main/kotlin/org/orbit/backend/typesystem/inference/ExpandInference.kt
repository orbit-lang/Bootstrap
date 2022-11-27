package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.intrinsics.OrbCoreBooleans
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.*
import org.orbit.util.Invocation

object ExpandInference : ITypeInference<ExpandNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private fun inferIntLiteral(node: IntLiteralNode) : IntValue
        = IntValue(node.value.second)

    private fun inferBoolLiteral(node: BoolLiteralNode) : AnyType = when (node.value) {
        true -> OrbCoreBooleans.trueType
        else -> OrbCoreBooleans.falseType
    }

    private fun inferConstructorInvocation(node: ConstructorInvocationNode, env: ITypeEnvironment) : IValue<*, *> {
        val type = TypeInferenceUtils.infer(node, env)
        val flat = type.flatten(type, env)
        val struct = flat as? IType.Struct
            ?: throw invocation.make<TypeSystem>("Cannot construct compile-time instance of non-Structural Type $flat", node.typeExpressionNode)
        val args = TypeInferenceUtils.inferAll(node.parameterNodes, env)
        val pMap = struct.members
            .zip(args).withIndex()
            .associate {
                val nth = when (it.index) {
                    1 -> "1st"
                    else -> "${it.index}th"
                }
                val constValue = it.value.second as? IValue<*, *>
                    ?: throw invocation.make<TypeSystem>("In order to construct a compile-time instance of Structural Type $struct, all constructor arguments must also be compile-time values, found runtime value of type ${it.value.second} at argument index ${it.index}\n\tSuggestion: expand $nth constructor argument", node)

                it.value.first.first to constValue
            }

        return InstanceValue(flat, pMap)
    }

    private fun inferTypeIdentifier(node: TypeIdentifierNode, env: ITypeEnvironment) : IValue<*, *> {
        val type = TypeInferenceUtils.infer(node, env)
        val flat = type.flatten(type, env)

        if (flat !is IType.IConstructableType<*>) {
            throw invocation.make<TypeSystem>("Cannot expand instance of non-Constructable Type $type", node)
        }

        val singleton = GlobalEnvironment.getSingletonValue(flat)
            ?: InstanceValue(flat, emptyMap())

        GlobalEnvironment.register(singleton)

        return singleton
    }

    private fun inferMethodCall(node: MethodCallNode, env: ITypeEnvironment) : AnyType {
        val receiver = TypeInferenceUtils.infer(node.receiverExpression, env)

        if (receiver !is IValue<*, *>) {
            throw invocation.make<TypeSystem>("Cannot call method `${node.messageIdentifier.identifier}` at compile-time because receiver $receiver is not a compile-time value", node)
        }

        val args = TypeInferenceUtils.inferAll(node.arguments, env)
        for (arg in args.withIndex()) {
            if (arg.value !is IValue<*, *>) {
                throw invocation.make<TypeSystem>("Cannot call method `${node.messageIdentifier.identifier}` at compile-time because argument at index ${arg.index} `$receiver` is not a compile-time value", node)
            }
        }

        return TypeInferenceUtils.infer(node, env)
    }

    private fun inferCollectionLiteral(node: CollectionLiteralNode, env: ITypeEnvironment) : IValue<*, *> {
        val array = TypeInferenceUtils.inferAs<CollectionLiteralNode, IType.Array>(node, env)
        val elements = TypeInferenceUtils.inferAll(node.elements, env)
        val nElements = mutableListOf<AnyType>()
        for (element in elements.withIndex()) {
            if (element.value is IValue<*, *>) {
                nElements.add(element.value as IValue<*, *>)
            } else {
                val elementNode = when (val n = node.elements[element.index]) {
                    is RValueNode -> n.expressionNode
                    else -> n
                }

                if (elementNode !is IConstantExpressionNode) {
                    throw invocation.make<TypeSystem>("Cannot initialise compile-time Array literal because element at index ${element.index} is not a compile-time constant", elementNode)
                }

                nElements.add(inferExpression(elementNode, env))
            }
        }

        return ArrayValue(array, nElements)
    }

    private fun inferExpression(node: IConstantExpressionNode, env: ITypeEnvironment) : AnyType = when (node) {
        is IntLiteralNode -> inferIntLiteral(node)
        is BoolLiteralNode -> inferBoolLiteral(node)
        is IdentifierNode -> env.getBinding(node.identifier, node.index)?.type ?: IType.Never("`${node.identifier}` is not defined in the current context")
        is ConstructorInvocationNode -> inferConstructorInvocation(node, env)
        is TypeIdentifierNode -> inferTypeIdentifier(node, env)
        is MethodCallNode -> inferMethodCall(node, env)
        is CollectionLiteralNode -> inferCollectionLiteral(node, env)

        else -> throw invocation.make<TypeSystem>("Cannot expand value at compile-time: `$node`", node)
    }

    override fun infer(node: ExpandNode, env: ITypeEnvironment): AnyType
        = inferExpression(node.expressionNode, env)
}