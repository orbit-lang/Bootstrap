package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.*

enum class TypeAttribute(val symbol: String) {
    Uninhabited("!");

    companion object {
        fun parse(symbol: String) : TypeAttribute? {
            for (attribute in values()) {
                if (attribute.symbol == symbol) return attribute
            }

            return null
        }
    }

    override fun toString(): String = symbol
}

sealed interface IType<T : IType<T>> : Substitutable<T> {
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
    }

    object Always : IMetaType<Always> {
        override val id: String = "*"
        override fun substitute(substitution: Substitution): Always = this
        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = other
        override fun plus(other: IMetaType<*>): IMetaType<*> = other
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

        override fun substitute(substitution: Substitution): Never = this
        override fun equals(other: Any?): Boolean = this === other
        operator fun plus(other: Never) : Never = Never("$message\n${other.message}")
        override fun plus(other: IMetaType<*>): IMetaType<*> = when (other) {
            is Always -> this
            is Never -> this + other
        }
    }

    object Unit : Entity<Unit> {
        override val id: String = "_"

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = other

        override fun substitute(substitution: Substitution): Unit = this
        override fun equals(other: Any?): Boolean = when (other) {
            is Unit -> true
            else -> false
        }
    }

    data class Type(val name: String, val attributes: List<TypeAttribute> = emptyList()) : Entity<Type> {
        companion object {
            val self = Type("__Self")
        }

        override val id: String = when (attributes.isEmpty()) {
            true -> name
            else -> name + attributes.joinToString("")
        }

        override fun getCanonicalName(): String = name

        fun api(env: Env): ITrait = ITrait.MembershipTrait("$id.__api", env.getMembers(this))

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
            this -> this
            Unit -> this
            is Type -> api(env) + other.api(env)
            is ITrait -> api(env) + other
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
            else -> false
        }
    }

    data class Member(val name: String, val type: Entity<*>, val owner: Type) : IType<Member> {
        override val id: String = "${type.id}.${name}"

        override fun substitute(substitution: Substitution): Member =
            Member(name, type.substitute(substitution), owner)

        override fun equals(other: Any?): Boolean = when (other) {
            is Member -> name == other.name && type == other.type
            else -> false
        }
    }

    sealed interface IConstructor<T : AnyType> : IArrow<IConstructor<T>> {
        val constructedType: T
    }

    sealed interface ICompositeType<Self : ICompositeType<Self>> : Entity<Self>

    sealed interface IIndexType<I, Self : IIndexType<I, Self>> : IType<Self> {
        fun getElement(at: I): AnyType
    }

    interface IConstructableType<Self : IConstructableType<Self>> : IType<Self> {
        fun getConstructors(): List<IConstructor<Self>>
    }

    sealed interface IProductType<I, Self : IProductType<I, Self>> : ICompositeType<Self>, IIndexType<I, Self>
    sealed interface ISumType<Self : ISumType<Self>> : ICompositeType<Self>, IIndexType<AnyType, Self>

    data class Tuple(val elementTypes: List<AnyType>) : IProductType<Int, Tuple> {
        constructor(first: AnyType, second: AnyType) : this(listOf(first, second))

        override val id: String = "(${elementTypes.joinToString(" & ") { it.id }})"

        val numberOfElements: Int
            get() = elementTypes.count()

        init {
            if (numberOfElements < 2) throw Exception("A Tuple must have at least 2 elements")
        }

        override fun getElement(at: Int): AnyType = when (at < numberOfElements) {
            true -> elementTypes[at]
            else -> Never("Attempt to retrieve element from Tuple of size $numberOfElements at index $at")
        }

        override fun substitute(substitution: Substitution): Tuple = Tuple(elementTypes.substituteAll(substitution))

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
            is Tuple -> Tuple(this, other)
            else -> Never("Cannot unify Types $id & ${other.id}")
        }
    }

    data class Struct(val members: List<Member>) : IProductType<String, Struct>, IConstructableType<Struct> {
        override val id: String = "{${members.joinToString("; ") { it.id }}}"

        override fun getElement(at: String): AnyType = members.first { it.name == at }

        override fun getConstructors(): List<IConstructor<Struct>> = listOf(StructConstructor(this, members))

        override fun substitute(substitution: Substitution): Struct = Struct(members.substituteAll(substitution))

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
            is Struct -> Union(this, other)
            else -> Never("Cannot unify Types $id & ${other.id}")
        }
    }

    data class StructConstructor(override val constructedType: Struct, val args: List<Member>) :
        IConstructor<Struct> {
        override val id: String = "(${args.joinToString(", ") { it.id }}) -> ${constructedType.id}"

        override fun getDomain(): List<AnyType> = args.map { it.type }
        override fun getCodomain(): AnyType = constructedType

        override fun curry(): IArrow<*> = this

        override fun substitute(substitution: Substitution): IConstructor<Struct> =
            StructConstructor(constructedType.substitute(substitution), args.substituteAll(substitution))

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> =
            Never("Cannot unify Types $id & ${other.id}")

        override fun never(args: List<AnyType>): Never =
            Never("Cannot construct Type ${constructedType.id} with arguments (${args.joinToString("; ") { it.id }})")
    }

    data class UnionConstructor(override val constructedType: Union, val arg: AnyType) : IConstructor<Union> {
        override val id: String = "(${arg.id}) -> ${constructedType.id}"

        override fun getDomain(): List<AnyType> = listOf(arg)
        override fun getCodomain(): AnyType = constructedType.getElement(arg)

        override fun curry(): IArrow<*> = this

        override fun substitute(substitution: Substitution): IConstructor<Union> =
            UnionConstructor(constructedType.substitute(substitution), arg.substitute(substitution))

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> =
            Never("Cannot unify Types $id & ${other.id}")

        override fun never(args: List<AnyType>): Never =
            Never("Union Type ${constructedType.id} cannot be constructed with argument ${arg.id}")
    }

    data class Union(val left: AnyType, val right: AnyType) : ISumType<Union>, IConstructableType<Union> {
        override val id: String = "(${left.id} | ${right.id})"

        override fun getElement(at: AnyType): AnyType = when (at) {
            left -> left
            right -> right
            else -> Never("Sum Type $id will never contain a value of Type ${at.id}")
        }

        override fun getConstructors(): List<IConstructor<Union>> =
            listOf(left, right).map { UnionConstructor(this, it) }

        override fun substitute(substitution: Substitution): Union =
            Union(left.substitute(substitution), right.substitute(substitution))

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
            is Union -> Union(this, other)
            else -> Never("Cannot unify Types $id & ${other.id}")
        }
    }

    sealed interface IArrow<Self : IArrow<Self>> : UnifiableType<Self> {
        fun getDomain(): List<AnyType>
        fun getCodomain(): AnyType

        fun curry(): IArrow<*>
        fun never(args: List<AnyType>): IType.Never

        fun maxCurry(): Arrow0 = when (this) {
            is Arrow0 -> this
            is Arrow1 -> curry()
            is Arrow2 -> curry().curry()
            is Arrow3 -> curry().curry().curry()
            else -> Arrow0(this)
        }

        override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> =
            Never("Cannot unify Arrow Type $id with ${other.id}")
    }

    data class Arrow0(val gives: IType<*>) : IArrow<Arrow0> {
        override val id: String = "() -> ${gives.id}"

        override fun getDomain(): List<AnyType> = emptyList()
        override fun getCodomain(): AnyType = gives

        override fun substitute(substitution: Substitution): Arrow0 = Arrow0(gives.substitute(substitution))

        override fun curry(): Arrow0 = Arrow0(Arrow0(gives))

        override fun never(args: List<AnyType>): Never = Never("Unreachable")
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
    }

    data class Signature(
        val receiver: IType<*>,
        val name: String,
        val parameters: List<IType<*>>,
        val returns: IType<*>
    ) : IType<Signature> {
        override val id: String
            get() {
                val pParams = parameters.joinToString(", ") { it.id }

                return "${receiver.id}.$name($pParams)(${returns.id})"
            }

        fun toArrow(): AnyArrow {
            val takes = listOf(receiver) + parameters

            return when (takes.count()) {
                1 -> Arrow1(takes[0], returns)
                2 -> Arrow2(takes[0], takes[1], returns)
                3 -> Arrow3(takes[0], takes[1], takes[2], returns)
                else -> TODO("4+-ary Arrows")
            }
        }

        override fun substitute(substitution: Substitution): Signature = Signature(
            receiver.substitute(substitution),
            name,
            parameters.map { it.substitute(substitution) },
            returns.substitute(substitution)
        )

        override fun equals(other: Any?): Boolean = when (other) {
            is Signature -> other.name == name && other.receiver == receiver && other.parameters == parameters && other.returns == returns
            else -> false
        }
    }

    data class TypeVar(val name: String) : IType<TypeVar> {
        override val id: String = "?$name"

        override fun substitute(substitution: Substitution): TypeVar = this

        override fun equals(other: Any?): Boolean = when (other) {
            is TypeVar -> id == other.id
            else -> false
        }
    }

    sealed interface ITrait : Entity<ITrait> {
        data class MembershipTrait(override val id: String, val requiredMembers: List<Member>) : ITrait {
            private val env: Env by Env

            override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
                is MembershipTrait -> plus(other)
                else -> other.unify(env, this)
            }

            override fun plus(other: ITrait): ITrait = when (other) {
                is MembershipTrait -> MembershipTrait(
                    "($id & ${other.id})",
                    requiredMembers + other.requiredMembers
                )
            }

            override fun equals(other: Any?): Boolean = when (other) {
                is MembershipTrait -> other.id == id
                is Type -> when (Contract.Implements.Membership(other, this).verify(env)) {
                    is Contract.ContractResult.Verified -> true
                    is Contract.ContractResult.Violated -> false
                }
                else -> false
            }
        }

        override fun substitute(substitution: Substitution): ITrait = when (substitution.old.id) {
            id -> when (substitution.new) {
                is ITrait -> substitution.new
                else -> this
            }

            else -> this
        }

        operator fun plus(other: ITrait): ITrait
    }

    val id: String

    fun getCanonicalName() : String = id
}