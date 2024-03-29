package org.orbit.core.nodes

import org.koin.core.component.KoinComponent
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.kinds.KindUtil
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.components.Token
import org.orbit.core.components.TokenTypes

sealed interface ITypeBoundsOperator {
    companion object : KoinComponent {
        fun valueOf(token: Token) : ITypeBoundsOperator? = when {
            token.type == TokenTypes.Assignment -> Eq
            token.type == TokenTypes.Colon -> Like
            token.text == "^" -> KindEq
            token.type == TokenTypes.Identifier -> UserDefined(token.text)
            token.type == TokenTypes.RAngle -> Gt
            token.type == TokenTypes.LAngle -> Lt
            else -> null
        }
    }

    val op: String

    object Eq : ITypeBoundsOperator {
        override val op: String = "="

        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): AnyMetaType {
            return when (val result = TypeUtils.check(env, left, right)) {
                is Never -> result
                else -> Always
            }
        }

        override fun toString(): String = op
    }

    object Gt : ITypeBoundsOperator {
        override val op: String = ">"

        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): IMetaType<*> = when (left) {
            is IntValue -> when (right) {
                is IntValue -> when (left.value > right.value) {
                    true -> Always
                    else -> Never("Attribute Operator `>` failed because `${left.value}` > `${right.value}` is false")
                }
                else -> Never("Attribute Operator '>' failed because $right cannot be compared to $left")
            }

            else -> Never("Attribute Operator '>' failed because $left and $right are not comparable")
        }

        override fun toString(): String = op
    }

    object Lt : ITypeBoundsOperator {
        override val op: String = "<"

        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): AnyMetaType = when (left) {
            is IntValue -> when (right) {
                is IntValue -> when (left.value < right.value) {
                    true -> Always
                    else -> Never("Attribute Operator `<` failed because `${left.value}` < `${right.value}` is false")
                }
                else -> Never("Attribute Operator '<' failed because $right cannot be compared to $left")
            }

            else -> Never("Attribute Operator '<' failed because $left and $right are not comparable")
        }

        override fun toString(): String = op
    }

    object Like : ITypeBoundsOperator {
        override val op: String = ":"

        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): AnyMetaType = when (right) {
            is Trait -> when (right.isImplementedBy(left, env)) {
                true -> Always
                else -> Never("Conformance Constraint failed: Type $left does not conform to Trait $right")
            }

            else -> Never("Conformance Constraint expects Trait on right-hand side, found $right")
        }

        override fun toString(): String = op
    }

    object KindEq : ITypeBoundsOperator {
        override val op: String = "^"

        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): AnyMetaType {
            val lKind = KindUtil.getKind(left, left::class.java.simpleName)
            val rKind = KindUtil.getKind(right, right::class.java.simpleName)

            return when (lKind == rKind) {
                true -> Always
                else -> Never("Kinds are not equal: $lKind & $rKind")
            }
        }

        override fun toString(): String = op
    }

    data class UserDefined(override val op: String) : ITypeBoundsOperator {
        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): AnyMetaType {
            TODO("Unsupported TypeBoundsOperator: UserDefined")
        }

        override fun toString(): String = op
    }

    fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment) : AnyMetaType
}