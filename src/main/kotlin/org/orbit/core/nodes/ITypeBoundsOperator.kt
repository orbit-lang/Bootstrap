package org.orbit.core.nodes

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.kinds.KindUtil
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.components.Token
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

sealed interface ITypeBoundsOperator {
    companion object : KoinComponent {
        private val invocation: Invocation by inject()

        fun valueOf(token: Token) : ITypeBoundsOperator = when {
            token.type == TokenTypes.Assignment -> Eq
            token.type == TokenTypes.Colon -> Like
            token.text == "^" -> KindEq
            token.type == TokenTypes.Identifier -> UserDefined(token.text)
            else -> throw invocation.make<Parser>("Illegal Constraint operator `${token.text}`", token)
        }
    }

    val op: String

    object Eq : ITypeBoundsOperator {
        override val op: String = "="

        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): IType.IMetaType<*> {
            return when (val result = TypeUtils.check(env, left, right)) {
                is IType.Never -> result
                else -> IType.Always
            }
        }
    }

    object Like : ITypeBoundsOperator {
        override val op: String = ":"

        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): IType.IMetaType<*> = when (right) {
            is IType.Trait -> when (right.isImplementedBy(left, env)) {
                true -> IType.Always
                else -> IType.Never("Conformance Constraint failed: Type $left does not conform to Trait $right")
            }

            else -> IType.Never("Conformance Constraint expects Trait on right-hand side, found $right")
        }
    }

    object KindEq : ITypeBoundsOperator {
        override val op: String = "^"

        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): IType.IMetaType<*> {
            val lKind = KindUtil.getKind(left, left::class.java.simpleName)
            val rKind = KindUtil.getKind(right, right::class.java.simpleName)

            return when (lKind == rKind) {
                true -> IType.Always
                else -> IType.Never("Kinds are not equal: $lKind & $rKind")
            }
        }
    }

    data class UserDefined(override val op: String) : ITypeBoundsOperator {
        override fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment): IType.IMetaType<*> {
            TODO("Unsupported TypeBoundsOperator: UserDefined")
        }
    }

    fun apply(left: AnyType, right: AnyType, env: ITypeEnvironment) : IType.IMetaType<*>
}