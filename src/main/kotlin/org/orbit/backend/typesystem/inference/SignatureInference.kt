package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.Unit
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.TypeIdentifierNode

object SignatureInference : ITypeInference<MethodSignatureNode, IMutableTypeEnvironment> {
    sealed interface Option {
        object None : Option
        object Persistent : Option
        object Virtual : Option
        data class Options(val options: List<Option>) : Option

        fun contains(option: Option) : Boolean = when (this) {
            is None -> false
            is Options -> options.contains(option)
            else -> this == option
        }

        operator fun plus(other: Option) : Option = when (other) {
            is None -> this
            is Options -> when (this) {
                is Options -> Options(options + other.options)
                else -> Options(other.options + this)
            }

            else -> when (this) {
                is None -> other
                is Options -> Options(options + other)
                else -> Options(listOf(this, other))
            }
        }
    }

    override fun infer(node: MethodSignatureNode, env: IMutableTypeEnvironment): AnyType {
        val options = env.consume(Option.None) as? Option
            ?: Option.None

        val receiver = TypeInferenceUtils.infer(node.receiverTypeNode, env)
        val params = TypeInferenceUtils.inferAll(node.parameterNodes, env)
        val ret = when (val r = node.returnTypeNode) {
            null -> Unit
            else -> TypeInferenceUtils.infer(r, env)
        }

        val effects = TypeInferenceUtils.inferAllAs<TypeIdentifierNode, Effect>(node.effects, env)
        val signature = Signature(receiver, node.identifierNode.identifier, params, ret, node.isInstanceMethod, effects, isVirtual = options.contains(Option.Virtual))

        effects.forEach { env.track(it) }

        if (options.contains(Option.Persistent)) {
            GlobalEnvironment.add(signature)
        }

        return signature
    }
}