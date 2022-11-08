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

    private fun inferIntLiteral(node: IntLiteralNode) : IntValue {
        return IntValue(node.value.second)
    }

    private fun inferBoolLiteral(node: BoolLiteralNode) : AnyType = when (node.value) {
        true -> OrbCoreBooleans.trueType
        else -> OrbCoreBooleans.falseType
    }

    override fun infer(node: ExpandNode, env: ITypeEnvironment): AnyType = when (node.expressionNode) {
        is IntLiteralNode -> inferIntLiteral(node.expressionNode)
        is BoolLiteralNode -> inferBoolLiteral(node.expressionNode)
        is IdentifierNode -> env.getBinding(node.expressionNode.identifier)?.type ?: IType.Never("`${node.expressionNode.identifier}` is not defined in the current context")
        is ConstructorInvocationNode -> {
            val type = TypeInferenceUtils.infer(node.expressionNode, env)
            val flat = type.flatten(type, env)
            val struct = flat as? IType.Struct
                ?: throw invocation.make<TypeSystem>("Cannot construct compile-time instance of non-Structural Type $flat", node.expressionNode.typeExpressionNode)
            val args = TypeInferenceUtils.inferAll(node.expressionNode.parameterNodes, env)
            val pMap = struct.members
                .zip(args).withIndex()
                .associate {
                    val nth = when (it.index) {
                        1 -> "1st"
                        else -> "${it.index}th"
                    }
                    val constValue = it.value.second as? IValue<*, *>
                        ?: throw invocation.make<TypeSystem>("In order to construct a compile-time instance of Structural Type $struct, all constructor arguments must also be compile-time values, found runtime value of type ${it.value.second} at argument index ${it.index}\n\tSuggestion: expand $nth constructor argument", node.expressionNode)

                    it.value.first.first to constValue
                }

            InstanceValue(flat, pMap)
        }

        is TypeIdentifierNode -> {
            val type = TypeInferenceUtils.infer(node.expressionNode, env)
            val flat = type.flatten(type, env)

            if (flat !is IType.IConstructableType<*>) {
                throw invocation.make<TypeSystem>("Cannot expand instance of non-Constructable Type $type", node.expressionNode)
            }

//            if (flat.getCardinality() != ITypeCardinality.Mono) {
//                throw invocation.make<TypeSystem>("Cannot expand instance of non-Singleton Type $type", node.expressionNode)
//            }

//            val struct = flat.toStruct()

            val singleton = GlobalEnvironment.getSingletonValue(flat)
                ?: InstanceValue(flat, emptyMap())

            GlobalEnvironment.register(singleton)

            singleton
        }

        is MethodCallNode -> {
            val receiver = TypeInferenceUtils.infer(node.expressionNode.receiverExpression, env)

            if (receiver !is IValue<*, *>) {
                throw invocation.make<TypeSystem>("Cannot call method `${node.expressionNode.messageIdentifier.identifier}` at compile-time because receiver $receiver is not a compile-time value", node.expressionNode)
            }

            val args = TypeInferenceUtils.inferAll(node.expressionNode.parameterNodes, env)
            for (arg in args.withIndex()) {
                if (arg.value !is IValue<*, *>) {
                    throw invocation.make<TypeSystem>("Cannot call method `${node.expressionNode.messageIdentifier.identifier}` at compile-time because argument at index ${arg.index} `$receiver` is not a compile-time value", node.expressionNode)
                }
            }

            TypeInferenceUtils.infer(node.expressionNode, env)
        }

        else -> throw invocation.make<TypeSystem>("Cannot expand value at compile-time: `${node.expressionNode}`", node.expressionNode)
    }
}