package org.orbit.backend.typesystem.components.kinds

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.Array
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

interface IKind : IType {
    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun substitute(substitution: Substitution): AnyType = this

    fun isInhabitable(): Boolean
}

sealed interface IntrinsicKinds : IKind {
    object AnyKind : IntrinsicKinds {
        override val id: String = "AnyKind"

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
            is AnyKind -> true
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
    object Level0Inspector : IntrinsicKindInspector<IntrinsicKinds.Level0> {
        override fun inspect(type: IntrinsicKinds.Level0): IKind = type
    }

    object TypeKindInspector : IntrinsicKindInspector<Type> {
        override fun inspect(type: Type): IKind = IntrinsicKinds.Level0.type
    }

    object TraitKindInspector : IntrinsicKindInspector<Trait> {
        override fun inspect(type: Trait): IKind = IntrinsicKinds.Level0.trait
    }

    sealed interface IFunctionKindInspector<A: AnyArrow> : IntrinsicKindInspector<A> {
        object F0 : IFunctionKindInspector<Arrow0>
        object F1 : IFunctionKindInspector<Arrow1>
        object F2 : IFunctionKindInspector<Arrow2>
        object F3 : IFunctionKindInspector<Arrow3>

        override fun inspect(type: A): IKind = IntrinsicKinds.Level0.type
    }

    object TypeVariableKindInspector : IntrinsicKindInspector<TypeVar> {
        override fun inspect(type: TypeVar): IKind = IntrinsicKinds.Level0.type
    }

    object TupleKindInspector : IntrinsicKindInspector<Tuple> {
        override fun inspect(type: Tuple): IKind = IntrinsicKinds.Level0.type
    }

    object StructKindInspector : IntrinsicKindInspector<Struct> {
        override fun inspect(type: Struct): IKind = IntrinsicKinds.Level0.type
    }

    object UnionKindInspector : IntrinsicKindInspector<Union> {
        override fun inspect(type: Union): IKind = IntrinsicKinds.Level0.type
    }

    object ArrayKindInspector : IntrinsicKindInspector<Array> {
        override fun inspect(type: Array): IKind = IntrinsicKinds.Level0.type
    }

    object AliasKindInspector : IntrinsicKindInspector<TypeAlias> {
        override fun inspect(type: TypeAlias): IKind = KindUtil.getKind(type.type, type.type::class.java.simpleName)
    }

    object AlwaysKindInspector : IKindInspector<Always> {
        override fun inspect(type: Always): IKind = IntrinsicKinds.AnyKind
    }

    object HigherKindInspector : IntrinsicKindInspector<ConstrainedArrow> {
        override fun inspect(type: ConstrainedArrow): IKind = when (type.arrow) {
            is Arrow0 -> IntrinsicKinds.Arrow(IntrinsicKinds.Level0.type, KindUtil.getKind(type.arrow.gives, type.arrow.gives::class.java.simpleName))
            is Arrow1 -> IntrinsicKinds.Arrow(KindUtil.getKind(type.arrow.takes, type.arrow.takes::class.java.simpleName), KindUtil.getKind(type.arrow.gives, type.arrow.gives::class.java.simpleName))
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