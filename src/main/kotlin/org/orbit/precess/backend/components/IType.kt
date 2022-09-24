package org.orbit.precess.backend.components

import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.core.components.IIntrinsicOperator
import org.orbit.core.nodes.OperatorFixity
import org.orbit.precess.backend.utils.*
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance
import java.time.Month

fun <M: IIntrinsicOperator> IIntrinsicOperator.Factory<M>.parse(symbol: String) : M? {
    for (modifier in all()) {
        if (modifier.symbol == symbol) return modifier
    }

    return null
}

enum class TypeAttribute(override val symbol: String) : IIntrinsicOperator {
    Uninhabited("!");

    companion object : IIntrinsicOperator.Factory<TypeAttribute> {
        override fun all(): List<TypeAttribute> = values().toList()
    }

    override fun toString(): String = symbol
}

enum class TypeOperator(override val symbol: String) : IIntrinsicOperator {
    Product("∏"), Sum("∑");

    companion object : IIntrinsicOperator.Factory<TypeOperator> {
        override fun all(): List<TypeOperator> = values().toList()
    }
}

enum class ContextOperator(override val symbol: String) : IIntrinsicOperator {
    Extend("+"), Reduce("-");

    companion object : IIntrinsicOperator.Factory<ContextOperator> {
        override fun all(): List<ContextOperator> = values().toList()
    }
}

sealed interface ITypeCardinality {
    object Zero : ITypeCardinality {
        override fun plus(other: ITypeCardinality): ITypeCardinality = when (other) {
            is Finite -> Finite(other.count + 1)
            is Infinite -> Infinite
            is Mono -> Finite(2)
            is Zero -> this
        }
    }

    object Mono : ITypeCardinality {
        override fun plus(other: ITypeCardinality): ITypeCardinality = when (other) {
            is Finite -> Finite(other.count + 1)
            is Infinite -> Infinite
            else -> this
        }
    }

    object Infinite : ITypeCardinality {
        override fun plus(other: ITypeCardinality): ITypeCardinality
            = this
    }

    data class Finite(val count: Int) : ITypeCardinality {
        override fun plus(other: ITypeCardinality): ITypeCardinality = when (other) {
            is Finite -> Finite(count + other.count)
            is Infinite -> Infinite
            is Mono -> Finite(count + 1)
            is Zero -> this
        }
    }

    operator fun plus(other: ITypeCardinality): ITypeCardinality
}

sealed interface IType<T : IType<T>> : Substitutable<T>, IPrecessComponent {
    interface UnifiableType<Self : UnifiableType<Self>> : IType<Self> {
        fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*>
    }

    sealed interface Entity<E : Entity<E>> : UnifiableType<E>
    sealed interface IMetaType<M: IMetaType<M>> : Entity<M> {
        fun toBoolean() : Boolean = when (this) {
            is Always -> true
            is Never -> false
        }

        operator fun plus(other: IMetaType<*>) : IMetaType<*>
        override fun exists(env: Env): AnyType = this
    }

    object Always : IMetaType<Always> {
        override val id: String = "*"
        override fun substitute(substitution: Substitution): Always = this
        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = other
        override fun plus(other: IMetaType<*>): IMetaType<*> = other
        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono
        override fun equals(other: Any?): Boolean = true

        override fun toString(): String = "✓"
    }

    data class Never(val message: String, override val id: String = "!") : IMetaType<Never>, IArrow<Never> {
        fun panic(): Nothing = throw RuntimeException(message)

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
            is Never -> Never("$message\n${other.message}", "$id:${other.id}")
            else -> this
        }

        override fun getDomain(): List<AnyType> = emptyList()
        override fun getCodomain(): AnyType = this
        override fun curry(): IArrow<*> = this
        override fun never(args: List<AnyType>): Never = this
        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono

        override fun substitute(substitution: Substitution): Never = this
        override fun equals(other: Any?): Boolean = this === other
        operator fun plus(other: Never) : Never = Never("$message\n${other.message}")
        override fun plus(other: IMetaType<*>): IMetaType<*> = when (other) {
            is Always -> this
            is Never -> this + other
        }

        override fun unbox(env: Env): AnyType = this
        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()

            return printer.apply(message, PrintableKey.Error)
        }

        override fun toString(): String = prettyPrint()
    }

    object Unit : Entity<Unit> {
        override val id: String = "Unit"

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = other
        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono

        override fun substitute(substitution: Substitution): Unit = this
        override fun equals(other: Any?): Boolean = when (other) {
            is Unit -> true
            else -> false
        }

        override fun exists(env: Env): AnyType = this

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()

            return printer.apply(OrbCoreTypes.unitType.name, PrintableKey.Bold)
        }

        override fun toString(): String = prettyPrint()
    }

    data class Safe(val type: AnyType) : IType<Safe> {
        override val id: String = type.id

        override fun getCardinality(): ITypeCardinality
            = type.getCardinality()

        override fun substitute(substitution: Substitution): Safe
            = Safe(type.substitute(substitution))

        override fun exists(env: Env): AnyType
            = type.exists(env)

        override fun getCanonicalName(): String
            = type.getCanonicalName()

        override fun equals(other: Any?): Boolean
            = type == other

        override fun flatten(env: Env): AnyType = this

        override fun getTypeCheckPosition(): TypeCheckPosition
            = type.getTypeCheckPosition()
    }

    data class Alias(val name: String, val type: AnyType) : IType<Alias>, UnboxableType {
        override val id: String = "$name:${type.id}"

        override fun getCardinality(): ITypeCardinality
            = type.getCardinality()

        override fun substitute(substitution: Substitution): Alias
            = Alias(name, type.substitute(substitution))

        override fun exists(env: Env): AnyType = type.exists(env)
        override fun getCanonicalName(): String = name
        override fun flatten(env: Env): AnyType
            = type.flatten(env)

        override fun equals(other: Any?): Boolean
            = type == other

        override fun getTypeCheckPosition(): TypeCheckPosition
            = type.getTypeCheckPosition()

        override fun unbox(env: Env): AnyType
            = TypeUtils.unbox(env, type)

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)

            return "$indent${type.prettyPrint(depth)}"
        }

        override fun toString(): String = prettyPrint()
    }

    sealed interface UnboxableType {
        fun unbox(env: Env) : AnyType
    }

    data class Box(val generator: AnyExpr) : IType<Box>, UnboxableType {
        override val id: String = "⎡$generator⎦"
        override fun substitute(substitution: Substitution): Box
            = Box(generator.substitute(substitution))

        override fun exists(env: Env): AnyType = this

        override fun getCardinality(): ITypeCardinality
            = ITypeCardinality.Infinite

        override fun equals(other: Any?): Boolean = when (other) {
            is Box -> other.generator == generator
            else -> false
        }

        override fun flatten(env: Env) : AnyType = this

        override fun unbox(env: Env): AnyType
            = generator.infer(env)

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()

            return printer.apply(id, PrintableKey.Bold)
        }

        override fun toString(): String = prettyPrint()
    }

    data class Type(val name: String, val attributes: List<TypeAttribute> = emptyList(), private val explicitCardinality: ITypeCardinality = ITypeCardinality.Mono) : Entity<Type> {
        companion object {
            val self = Type("__Self")
        }

        override fun getCardinality(): ITypeCardinality = explicitCardinality

        override val id: String = when (attributes.isEmpty()) {
            true -> name
            else -> name + attributes.joinToString("")
        }

        override fun getCanonicalName(): String = name

        override fun exists(env: Env): AnyType = when (val t = env.getElement(name)) {
            null -> Never("Unknown Type `$name` in current context:\n$env")
            else -> t
        }

        fun api(env: Env): Trait = Trait("$id.__api", env.getMembers(this), emptyList())

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
            this -> this
            Unit -> this
            is Type -> api(env) + other.api(env)
            is Trait -> api(env) + other
            is Never -> other
            else -> Never("Cannot unify Types $id & ${other.id}")
        }

        override fun substitute(substitution: Substitution): Type = when (substitution.old) {
            this -> when (substitution.new) {
                is Type -> substitution.new
                else -> this
            }
            else -> this
        }

        override fun equals(other: Any?): Boolean = when (other) {
            is Type -> when (other.id) {
                self.id -> true
                else -> id == other.id
            }
            else -> other == this
        }

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()

            return "$indent${printer.apply(name, PrintableKey.Bold)}"
        }

        override fun toString(): String = prettyPrint()
    }

    data class Member(val name: String, val type: AnyType, val owner: Type) : IType<Member> {
        override val id: String = "${owner.id}.${name}"

        override fun getCardinality(): ITypeCardinality
            = type.getCardinality()

        override fun substitute(substitution: Substitution): Member =
            Member(name, type.substitute(substitution), owner)

        override fun equals(other: Any?): Boolean = when (other) {
            is Member -> name == other.name && type == other.type
            else -> false
        }

        override fun exists(env: Env): AnyType = when (env.getElement(id)) {
            null -> Never("Unknown member `$this`")
            else -> this
        }

        override fun toString(): String = prettyPrint()
    }

    sealed interface IConstructor<T : AnyType> : IArrow<IConstructor<T>> {
        val constructedType: T
    }

    sealed interface ICompositeType<Self : ICompositeType<Self>> : Entity<Self>

    sealed interface IIndexType<I, Self : IIndexType<I, Self>> : IType<Self> {
        fun getElement(at: I): AnyType
    }

    interface IAlgebraicType<Self : IAlgebraicType<Self>> : IType<Self>, UnboxableType {
        fun getConstructors(): List<IConstructor<Self>>
        override fun getTypeCheckPosition(): TypeCheckPosition
            = TypeCheckPosition.AlwaysLeft
    }

    sealed interface IProductType<I, Self : IProductType<I, Self>> : IAlgebraicType<Self>, IIndexType<I, Self>
    sealed interface ISumType<Self : ISumType<Self>> : IAlgebraicType<Self>, IIndexType<AnyType, Self>

    data class Tuple(val left: AnyType, val right: AnyType) : IProductType<Int, Tuple> {
        override val id: String = "(${left.id} * ${right.id})"

        override fun getConstructors(): List<IConstructor<Tuple>> = emptyList()
        override fun getCardinality(): ITypeCardinality
            = ITypeCardinality.Finite(2)

        override fun getElement(at: Int): AnyType = when (at) {
            0 -> left
            1 -> right
            else -> Never("Attempt to retrieve element from Tuple at index $at")
        }

        override fun substitute(substitution: Substitution): Tuple = Tuple(left.substitute(substitution), right.substitute(substitution))

        override fun exists(env: Env): AnyType {
            val lType = left.exists(env)
            val rType = right.exists(env)

            return when (lType) {
                is Never -> when (rType) {
                    is Never -> lType + rType
                    else -> lType
                }

                else -> when (rType) {
                    is Never -> rType
                    else -> this
                }
            }
        }

        override fun flatten(env: Env): AnyType
            = Tuple(left.flatten(env), right.flatten(env))

        override fun unbox(env: Env): AnyType
            = Tuple(TypeUtils.unbox(env, left), TypeUtils.unbox(env, right))

        override fun toString(): String = prettyPrint()
    }

    data class Struct(val members: List<Member>) : IProductType<String, Struct>, IAlgebraicType<Struct> {
        override val id: String = "{${members.joinToString("; ") { it.id }}}"

        override fun getElement(at: String): AnyType = members.first { it.name == at }
        override fun getConstructors(): List<IConstructor<Struct>> = listOf(StructConstructor(this, members))
        override fun substitute(substitution: Substitution): Struct = Struct(members.substituteAll(substitution))
        override fun getCardinality(): ITypeCardinality = when (members.isEmpty()) {
            true -> ITypeCardinality.Mono
            else -> members.map { it.getCardinality() }.reduce(ITypeCardinality::plus)
        }

        override fun exists(env: Env): AnyType {
            TODO("Member exists")
        }

        override fun unbox(env: Env): AnyType {
            TODO("Not yet implemented")
        }

        override fun toString(): String = prettyPrint()
    }

    data class StructConstructor(override val constructedType: Struct, val args: List<Member>) : IConstructor<Struct> {
        override val id: String = "(${args.joinToString(", ") { it.id }}) -> ${constructedType.id}"

        override fun getDomain(): List<AnyType> = args.map { it.type }
        override fun getCodomain(): AnyType = constructedType
        override fun getCardinality(): ITypeCardinality
            = constructedType.getCardinality()

        override fun curry(): IArrow<*> = this

        override fun substitute(substitution: Substitution): IConstructor<Struct> =
            StructConstructor(constructedType.substitute(substitution), args.substituteAll(substitution))

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> =
            Never("Cannot unify Types $id & ${other.id}")

        override fun never(args: List<AnyType>): Never =
            Never("Cannot construct Type ${constructedType.id} with arguments (${args.joinToString("; ") { it.id }})")

        override fun exists(env: Env): AnyType {
            TODO("Not yet implemented")
        }

        override fun unbox(env: Env): AnyType {
            TODO("Not yet implemented")
        }

        override fun toString(): String = prettyPrint()
    }

    data class UnionConstructor(override val constructedType: Union, val arg: AnyType) : IConstructor<Union> {
        override val id: String = "(${arg.id}) -> ${constructedType.id}"

        override fun getDomain(): List<AnyType> = listOf(arg)
        override fun getCodomain(): AnyType = constructedType.getElement(arg)
        override fun getCardinality(): ITypeCardinality
            = constructedType.getCardinality()

        override fun curry(): IArrow<*> = this

        override fun substitute(substitution: Substitution): IConstructor<Union> =
            UnionConstructor(constructedType.substitute(substitution), arg.substitute(substitution))

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> =
            Never("Cannot unify Types $id & ${other.id}")

        override fun never(args: List<AnyType>): Never =
            Never("Union Type ${constructedType.id} cannot be constructed with argument ${arg.id}")

        override fun exists(env: Env): AnyType {
            TODO("Not yet implemented")
        }

        override fun unbox(env: Env): AnyType {
            TODO("Not yet implemented")
        }

        override fun toString(): String = prettyPrint()
    }

    data class Union(val left: AnyType, val right: AnyType) : ISumType<Union>, IAlgebraicType<Union> {
        override val id: String = "(${left.id} | ${right.id})"

        override fun getCardinality(): ITypeCardinality
            = ITypeCardinality.Finite(2)

        override fun getElement(at: AnyType): AnyType = when (at) {
            left -> left
            right -> right
            else -> Never("Sum Type $id will never contain a value of Type ${at.id}")
        }

        override fun getConstructors(): List<IConstructor<Union>> =
            listOf(left, right).map { UnionConstructor(this, it) }

        override fun substitute(substitution: Substitution): Union =
            Union(left.substitute(substitution), right.substitute(substitution))

        override fun exists(env: Env): AnyType {
            val lType = left.exists(env)
            val rType = right.exists(env)

            return when (lType) {
                is Never -> when (rType) {
                    is Never -> lType + rType
                    else -> lType
                }

                else -> when (rType) {
                    is Never -> rType
                    else -> this
                }
            }
        }

        override fun equals(other: Any?): Boolean = when (other) {
            is Union -> left == other.left && right == other.right
            is IType<*> -> left == other || right == other
            else -> false
        }

        override fun flatten(env: Env): AnyType
            = Union(left.flatten(env), right.flatten(env))

        override fun unbox(env: Env): AnyType
            = Union(TypeUtils.unbox(env, left), TypeUtils.unbox(env, right))

        override fun toString(): String = prettyPrint()
    }

    sealed interface IOperatorArrow<A: IArrow<A>, Self: IOperatorArrow<A, Self>> : IArrow<Self> {
        val fixity: OperatorFixity
        val symbol: String
        val identifier: String
        val arrow: A

        override val id: String get() = arrow.id

        override fun getDomain(): List<AnyType> = arrow.getDomain()
        override fun getCodomain(): AnyType = arrow.getCodomain()
        override fun curry(): IArrow<*> = arrow.curry()
        override fun never(args: List<AnyType>): Never = Never("")
        override fun exists(env: Env): AnyType = arrow.exists(env)
        override fun unbox(env: Env): AnyType = arrow.unbox(env)
        override fun getCardinality(): ITypeCardinality
            = arrow.getCodomain().getCardinality()

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()
            val prettyName = printer.apply("${fixity.name} $identifier `$symbol`", PrintableKey.Italics)

            return "$prettyName $arrow"
        }
    }

    data class PrefixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow1) : IOperatorArrow<Arrow1, PrefixOperator> {
        override val fixity: OperatorFixity = OperatorFixity.Prefix

        override fun substitute(substitution: Substitution): PrefixOperator
            = PrefixOperator(symbol, identifier, arrow.substitute(substitution))

        override fun toString(): String = prettyPrint()
    }

    data class PostfixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow1) : IOperatorArrow<Arrow1, PostfixOperator> {
        override val fixity: OperatorFixity = OperatorFixity.Postfix

        override fun substitute(substitution: Substitution): PostfixOperator
            = PostfixOperator(symbol, identifier, arrow.substitute(substitution))

        override fun toString(): String = prettyPrint()
    }

    data class InfixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow2) : IOperatorArrow<Arrow2, InfixOperator> {
        override val fixity: OperatorFixity = OperatorFixity.Infix

        override fun substitute(substitution: Substitution): InfixOperator
            = InfixOperator(symbol, identifier, arrow.substitute(substitution))

        override fun toString(): String = prettyPrint()
    }

    sealed interface IArrow<Self : IArrow<Self>> : UnifiableType<Self>, UnboxableType {
        fun getDomain(): List<AnyType>
        fun getCodomain(): AnyType
        override fun getCardinality(): ITypeCardinality
            = getCodomain().getCardinality()

        fun curry(): IArrow<*>
        fun never(args: List<AnyType>): Never

        fun maxCurry(): Arrow0 = when (this) {
            is Arrow0 -> this
            is Arrow1 -> curry()
            is Arrow2 -> curry().curry()
            is Arrow3 -> curry().curry().curry()
            else -> Arrow0(this)
        }

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> =
            Never("Cannot unify Arrow Type $id with ${other.id}")

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()
            val domainString = getDomain().joinToString(", ")

            return "($domainString) -> ${getCodomain()}"
        }
    }

    data class Arrow0(val gives: IType<*>) : IArrow<Arrow0> {
        override val id: String = "() -> ${gives.id}"

        override fun getDomain(): List<AnyType> = emptyList()
        override fun getCodomain(): AnyType = gives

        override fun substitute(substitution: Substitution): Arrow0 = Arrow0(gives.substitute(substitution))
        override fun curry(): Arrow0 = this
        override fun never(args: List<AnyType>): Never = Never("Unreachable")
        override fun exists(env: Env): AnyType = gives.exists(env)

        override fun flatten(env: Env): AnyType
            = Arrow0(gives.flatten(env))

        override fun equals(other: Any?): Boolean = when (other) {
            is Arrow0 -> gives == other.gives
            else -> false
        }

        override fun unbox(env: Env): AnyType
            = Arrow0(TypeUtils.unbox(env, gives))

        override fun toString(): String = prettyPrint()
    }

    data class Arrow1(val takes: IType<*>, val gives: IType<*>) : IArrow<Arrow1> {
        override val id: String = "(${takes.id}) -> ${gives.id}"

        override fun getDomain(): List<AnyType> = listOf(takes)
        override fun getCodomain(): AnyType = gives

        override fun substitute(substitution: Substitution): Arrow1 =
            Arrow1(takes.substitute(substitution), gives.substitute(substitution))

        override fun curry(): Arrow0 = Arrow0(Arrow1(takes, gives))

        override fun never(args: List<AnyType>): Never =
            Never("$id expects argument of Type ${takes.id}, found ${args[0].id}")

        override fun exists(env: Env): AnyType {
            val dType = takes.exists(env)
            val cType = gives.exists(env)

            return when (dType) {
                is Never -> when (cType) {
                    is Never -> dType + cType
                    else -> dType
                }

                else -> when (cType) {
                    is Never -> cType
                    else -> this
                }
            }
        }

        override fun flatten(env: Env): AnyType {
            val domain = takes.flatten(env)

            if (domain is Never) return domain

            val codomain = gives.flatten(env)

            if (domain is Never) return codomain

            return Arrow1(domain, codomain)
        }

        override fun equals(other: Any?): Boolean = when (other) {
            is Arrow1 -> takes == other.takes && gives == other.gives
            is Safe -> when (val t = other.type) {
                is Arrow1 -> this == t
                else -> false
            }
            else -> false
        }

        override fun unbox(env: Env): AnyType
            = Arrow1(TypeUtils.unbox(env, takes), TypeUtils.unbox(env, gives))

        override fun toString(): String = prettyPrint()
    }

    data class Arrow2(val a: IType<*>, val b: IType<*>, val gives: IType<*>) : IArrow<Arrow2> {
        override val id: String = "(${a.id}, ${b.id}) -> ${gives.id}"

        override fun getDomain(): List<AnyType> = listOf(a, b)
        override fun getCodomain(): AnyType = gives

        override fun substitute(substitution: Substitution): Arrow2 =
            Arrow2(a.substitute(substitution), b.substitute(substitution), gives.substitute(substitution))

        override fun curry(): Arrow1 = Arrow1(a, Arrow1(b, gives))

        override fun never(args: List<AnyType>): Never =
            Never("$id expects arguments of (${a.id}, ${b.id}), found (${args.joinToString(", ") { it.id }})")

        override fun exists(env: Env): AnyType {
            val type1 = a.exists(env)
            val type2 = b.exists(env)
            val type3 = gives.exists(env)

            return when (type1) {
                is Never -> when (type2) {
                    is Never -> when (type3) {
                        is Never -> type1 + type2 + type3
                        else -> type1 + type2
                    }

                    else -> when (type3) {
                        is Never -> type1 + type3
                        else -> type1
                    }
                }

                else -> when (type2) {
                    is Never -> when (type3) {
                        is Never -> type2 + type3
                        else -> type2
                    }

                    else -> when (type3) {
                        is Never -> type3
                        else -> this
                    }
                }
            }
        }

        override fun unbox(env: Env): AnyType {
            TODO("Not yet implemented")
        }

        override fun toString(): String = prettyPrint()
    }

    data class Arrow3(val a: IType<*>, val b: IType<*>, val c: IType<*>, val gives: IType<*>) : IArrow<Arrow3> {
        override val id: String = "(${a.id}, ${b.id}, ${c.id}) -> ${gives.id}"

        override fun getDomain(): List<AnyType> = listOf(a, b, c)
        override fun getCodomain(): AnyType = gives

        override fun substitute(substitution: Substitution): Arrow3 = Arrow3(
            a.substitute(substitution),
            b.substitute(substitution),
            c.substitute(substitution),
            gives.substitute(substitution)
        )

        override fun curry(): Arrow2 = Arrow2(a, b, Arrow1(c, gives))

        override fun never(args: List<AnyType>): Never =
            Never("$id expects arguments of (${a.id}, ${b.id}, ${c.id}), found (${args.joinToString(", ") { it.id }})")

        override fun exists(env: Env): AnyType {
            TODO("Fill in 'when table' for Arrow3")
        }

        override fun unbox(env: Env): AnyType {
            TODO("Not yet implemented")
        }

        override fun toString(): String = prettyPrint()
    }

    data class Signature(val receiver: IType<*>, val name: String, val parameters: List<IType<*>>, val returns: IType<*>, val isInstanceSignature: Boolean) : IArrow<Signature> {
        override val id: String get() {
            val pParams = parameters.joinToString(", ") { it.id }

            return "${receiver.id}.$name($pParams)(${returns.id})"
        }

        override fun getDomain(): List<AnyType> = toArrow().getDomain()
        override fun getCodomain(): AnyType = toArrow().getCodomain()

        override fun curry(): IArrow<*> = toArrow().curry()
        override fun never(args: List<AnyType>): Never = toArrow().never(args)
        override fun unbox(env: Env): AnyType = this

        override fun getCardinality(): ITypeCardinality
            = returns.getCardinality()

        private fun toInstanceArrow() = when (parameters.count()) {
            0 -> Arrow1(receiver, returns)
            1 -> Arrow2(receiver, parameters[0], returns)
            2 -> Arrow3(receiver, parameters[0], parameters[1], returns)
            else -> TODO("3+-ary instance Arrows")
        }

        fun toStaticArrow(): AnyArrow = when (parameters.count()) {
            0 -> Arrow0(returns)
            1 -> Arrow1(parameters[0], returns)
            2 -> Arrow2(parameters[0], parameters[1], returns)
            3 -> Arrow3(parameters[0], parameters[1], parameters[2], returns)
            else -> TODO("4+-ary Arrows")
        }

        fun toArrow() : AnyArrow = when (isInstanceSignature) {
            true -> toInstanceArrow()
            else -> toStaticArrow()
        }

        override fun substitute(substitution: Substitution): Signature
            = Signature(receiver.substitute(substitution), name, parameters.map { it.substitute(substitution) }, returns.substitute(substitution), isInstanceSignature)

        override fun equals(other: Any?): Boolean = when (other) {
            is Signature -> other.name == name && other.receiver == receiver && other.parameters == parameters && other.returns == returns
            else -> false
        }

        override fun exists(env: Env): AnyType = this

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val prettyParams = parameters.joinToString(", ") { it.prettyPrint(0) }
            val printer = getKoinInstance<Printer>()
            val prettyName = printer.apply(name, PrintableKey.Italics)

            return "$indent(${receiver.prettyPrint(0)}) $prettyName ($prettyParams) (${returns.prettyPrint(0)})"
        }

        override fun toString(): String = prettyPrint()
    }

    data class TypeVar(val name: String) : IType<TypeVar> {
        override val id: String = "?$name"

        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Infinite
        override fun substitute(substitution: Substitution): TypeVar = this

        override fun equals(other: Any?): Boolean = when (other) {
            is TypeVar -> id == other.id
            else -> false
        }

        override fun exists(env: Env): AnyType {
            TODO("Not yet implemented")
        }

        override fun toString(): String = prettyPrint()
    }

    data class Trait(override val id: String, val members: List<Member>, val signatures: List<Signature>) : Entity<Trait> {
        override fun substitute(substitution: Substitution): Trait
            = Trait(id, members.map { it.substitute(substitution) }, signatures.map { it.substitute(substitution) })

        override fun exists(env: Env): AnyType = this

        override fun getCardinality(): ITypeCardinality
            = ITypeCardinality.Zero

        override fun equals(other: Any?): Boolean = when (other) {
            is Trait -> other.id == id
            else -> false
        }

        fun isImplementedBy(type: AnyType, env: Env) : Boolean {
            val projections = env.getProjections(type)

            return projections.any { it.target.id == id }
        }

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
            is Trait -> Trait("$id*${other.id}", members + other.members, signatures + other.signatures)
            else -> TODO()
        }

        operator fun plus(other: Trait) : Trait
            = Trait("$id*${other.id}", members + other.members, signatures + other.signatures)

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()

            return "$indent${printer.apply(id, PrintableKey.Bold)}"
        }

        override fun toString(): String = prettyPrint()
    }

    val id: String

    fun getCanonicalName() : String = id

    fun exists(env: Env) : AnyType
    fun flatten(env: Env) : AnyType = this
    fun getTypeCheckPosition() : TypeCheckPosition = TypeCheckPosition.Any
    fun getCardinality() : ITypeCardinality

    fun prettyPrint(depth: Int = 0) : String {
        val indent = "\t".repeat(depth)

        return "$indent$id"
    }
}