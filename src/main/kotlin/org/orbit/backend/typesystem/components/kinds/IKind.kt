package org.orbit.backend.typesystem.components.kinds

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeCardinality
import org.orbit.backend.typesystem.components.Substitution
import org.orbit.backend.typesystem.inference.ITypeInference
import org.orbit.backend.typesystem.inference.run
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance
import java.util.Arrays

interface IKind : IType {
    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun substitute(substitution: Substitution): AnyType = this

    fun isInhabitable(): Boolean
}

sealed interface IntrinsicKinds : IKind {
    object Meta : IntrinsicKinds {
        override val id: String = "Meta"

        override fun isInhabitable(): Boolean = false
        override fun equals(other: Any?): Boolean = true

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()
            val indent = "\t".repeat(depth)

            return "$indent${printer.apply(id, PrintableKey.Bold)}"
        }

        override fun toString(): String
            = prettyPrint()
    }

    data class Level0(val name: String, private val inhabitable: Boolean) : IntrinsicKinds {
        companion object {
            val type = Level0("Type", true)
            val trait = Level0("Trait", false)
            val attribute = Level0("Attribute", false)
        }

        override val id: String = name

        override fun isInhabitable(): Boolean = inhabitable

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()
            val indent = "\t".repeat(depth)
            val pretty = printer.apply(name, PrintableKey.Bold)

            return "$indent$pretty"
        }

        override fun equals(other: Any?): Boolean = when (other) {
            is Level0 -> other.name == name
            is Meta -> true
            else -> false
        }

        override fun toString(): String
            = prettyPrint()
    }

    data class Arrow(val left: IKind, val right: IKind) : IntrinsicKinds {
        override val id: String = "($left) => $right"

        override fun isInhabitable(): Boolean
            = right.isInhabitable()

        override fun equals(other: Any?): Boolean = when (other) {
            is Arrow -> other.left == left && other.right == right
            else -> false
        }

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)

            return "$indent($left) => $right"
        }

        override fun toString(): String
            = prettyPrint()
    }
}

interface IKindInspector<T: AnyType> {
    fun inspect(type: T) : IKind
}

sealed interface IntrinsicKindInspector<T: AnyType> : IKindInspector<T> {
    object TypeKindInspector : IntrinsicKindInspector<IType.Type> {
        override fun inspect(type: IType.Type): IKind = IntrinsicKinds.Level0.type
    }

    object TraitKindInspector : IntrinsicKindInspector<IType.Trait> {
        override fun inspect(type: IType.Trait): IKind = IntrinsicKinds.Level0.trait
    }

    sealed interface IFunctionKindInspector<A: AnyArrow> : IntrinsicKindInspector<A> {
        object F0 : IFunctionKindInspector<IType.Arrow0>
        object F1 : IFunctionKindInspector<IType.Arrow1>
        object F2 : IFunctionKindInspector<IType.Arrow2>
        object F3 : IFunctionKindInspector<IType.Arrow3>

        override fun inspect(type: A): IKind = IntrinsicKinds.Level0.type
    }

    object TypeVariableKindInspector : IntrinsicKindInspector<IType.TypeVar> {
        override fun inspect(type: IType.TypeVar): IKind = IntrinsicKinds.Level0.type
    }

    object AliasKindInspector : IntrinsicKindInspector<IType.Alias> {
        override fun inspect(type: IType.Alias): IKind = KindUtil.getKind(type.type, type.type::class.java.simpleName)
    }

    object AlwaysKindInspector : IKindInspector<IType.Always> {
        override fun inspect(type: IType.Always): IKind = IntrinsicKinds.Meta
    }

    object HigherKindInspector : IntrinsicKindInspector<IType.ConstrainedArrow> {
        override fun inspect(type: IType.ConstrainedArrow): IKind = when (type.arrow) {
            is IType.Arrow0 -> IntrinsicKinds.Arrow(IntrinsicKinds.Level0.type, KindUtil.getKind(type.arrow.gives, type.arrow.gives::class.java.simpleName))
            is IType.Arrow1 -> IntrinsicKinds.Arrow(KindUtil.getKind(type.arrow.takes, type.arrow.takes::class.java.simpleName), KindUtil.getKind(type.arrow.gives, type.arrow.gives::class.java.simpleName))
            else -> TODO("UNSUPPORTED ARROW ARITY: ${type.arrow}")
        }
    }
}

object KindUtil : KoinComponent {
    val invocation: Invocation by inject()

    inline fun <reified T: AnyType> getKind(type: T, key: String, node: INode? = null) : IKind {
        val inference = KoinPlatformTools.defaultContext().get().getOrNull<IKindInspector<T>>(named("kind$key"))
            ?: throw invocation.compilerError<TypeSystem>("Could not inject Kind Inspector for Type $type ($key)", node?.firstToken ?: Token.empty)

        return inference.inspect(type)
    }
}