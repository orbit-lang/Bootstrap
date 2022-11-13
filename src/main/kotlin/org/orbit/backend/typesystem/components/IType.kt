package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.intrinsics.OrbCoreBooleans
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeCheckPosition
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.IIntrinsicOperator
import org.orbit.core.nodes.OperatorFixity
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

fun <M: IIntrinsicOperator> IIntrinsicOperator.Factory<M>.parse(symbol: String) : M? {
    for (modifier in all()) {
        if (modifier.symbol == symbol) return modifier
    }

    return null
}

private object TypeIndexer {
    private var index = 0

    fun next() : Int {
        index += 1
        return index
    }
}

sealed interface IType : IContextualComponent, Substitutable<AnyType> {
    sealed interface Entity<E : Entity<E>> : IType
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
        override fun plus(other: IMetaType<*>): IMetaType<*> = other
        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono
        override fun equals(other: Any?): Boolean = true
        override fun toString(): String = "✓"
    }

    data class Never(val message: String, override val id: String = "!") : IMetaType<Never>, IArrow<Never> {
        fun panic(): Nothing = throw getKoinInstance<Invocation>().make<TypeSystem>(message)

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

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()

            return printer.apply(message, PrintableKey.Error)
        }

        override fun toString(): String = prettyPrint()
    }

    object Unit : Entity<Unit> {
        override val id: String = "Unit"

        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono
        override fun substitute(substitution: Substitution): Unit = this
        override fun equals(other: Any?): Boolean = when (other) {
            is Unit -> true
            else -> false
        }

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()

            return printer.apply(OrbCoreTypes.unitType.name, PrintableKey.Bold)
        }

        override fun toString(): String = prettyPrint()
    }

    data class Safe(val type: AnyType) : AnyType {
        override val id: String = type.id

        override fun getCardinality(): ITypeCardinality
            = type.getCardinality()

        override fun substitute(substitution: Substitution): Safe
            = Safe(type.substitute(substitution))

        override fun getCanonicalName(): String
            = type.getCanonicalName()

        override fun equals(other: Any?): Boolean
            = type == other

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType = this

        override fun getTypeCheckPosition(): TypeCheckPosition
            = type.getTypeCheckPosition()
    }

    sealed interface ISpecialisedType : AnyType {
        fun isSpecialised() : Boolean
    }

    data class Lazy(val type: AnyType) : IType {
        override val id: String = "⎡$type⎦"

        override fun getCardinality(): ITypeCardinality
            = type.getCardinality()

        override fun substitute(substitution: Substitution): AnyType
            = Lazy(type.substitute(substitution))

        override fun prettyPrint(depth: Int): String
            = "${"\t".repeat(depth)}⎡$type⎦"

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType = type.flatten(from, env)
        //= when (val t = type.flatten(from, env)) {
//            is Never -> t.panic()
//            else -> t
//        }

        override fun toString(): String
            = prettyPrint()
    }

    data class Alias(val name: String, val type: AnyType) : ISpecialisedType {
        constructor(path: Path, type: AnyType) : this(path.toString(OrbitMangler), type)

        override val id: String = "${type.id} as $name"

        override fun getConstructors(): List<IConstructor<*>>
            = type.getConstructors()

        override fun getUnsolvedTypeVariables(): List<TypeVar>
            = type.getUnsolvedTypeVariables()

        override fun isSpecialised(): Boolean = when (type) {
            is ISpecialisedType -> type.isSpecialised()
            else -> false
        }

        override fun getCardinality(): ITypeCardinality
            = type.getCardinality()

        override fun substitute(substitution: Substitution): AnyType
            = Alias(name, type.substitute(substitution))

        override fun getCanonicalName(): String = name
        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
            = type.flatten(from, env)

        override fun equals(other: Any?): Boolean
            = type == other

        override fun getTypeCheckPosition(): TypeCheckPosition
            = type.getTypeCheckPosition()

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()
            val path = OrbitMangler.unmangle(name)
            val simpleName = path.last()
            val prettyName = printer.apply(simpleName, PrintableKey.Bold)

            return "$indent($prettyName = ${type.prettyPrint(depth)})"
        }

        override fun toString(): String = prettyPrint()
    }

    data class SingletonConstructor(val type: AnyType) : IConstructor<AnyType> {
        override val id: String = "() -> ${type.id}"
        override val constructedType: AnyType = type

        override fun getDomain(): List<AnyType> = emptyList()
        override fun getCodomain(): AnyType = type
        override fun curry(): IArrow<*> = this

        override fun never(args: List<AnyType>): Never {
            TODO("Not yet implemented")
        }

        override fun substitute(substitution: Substitution): AnyType
            = SingletonConstructor(type.substitute(substitution))
    }

    data class Type(val name: String, val attributes: List<TypeAttribute> = emptyList(), private val explicitCardinality: ITypeCardinality = ITypeCardinality.Mono) : Entity<Type>, IConstructableType<Type> {
        companion object {
            val self = Type("__Self")
        }

        constructor(path: Path) : this(path.toString(OrbitMangler))

        override fun isSpecialised(): Boolean = false

        override val id: String = when (attributes.isEmpty()) {
            true -> name
            else -> name + attributes.joinToString("")
        }

        fun toStruct() : Struct = when (getCardinality()) {
            ITypeCardinality.Mono -> Struct(emptyList())
            else -> TODO("")
        }

        override fun getConstructors(): List<IConstructor<Type>>
            = listOf(SingletonConstructor(this) as IConstructor<Type>)

        override fun getCardinality(): ITypeCardinality = explicitCardinality
        override fun getCanonicalName(): String = name

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
            val simpleName = getPath().last()

            return "$indent${printer.apply(simpleName, PrintableKey.Bold)}"
        }

        override fun toString(): String = prettyPrint()

        fun getPath() : Path
            = OrbitMangler.unmangle(name)
    }

    data class Case(val condition: AnyType, val result: AnyType) : IArrow<Case> {
        override val id: String = "case (${condition.id}) -> ${result.id}"

        override fun getCardinality(): ITypeCardinality
            = condition.getCardinality()

        override fun getDomain(): List<AnyType> = listOf(condition)
        override fun getCodomain(): AnyType = result

        override fun never(args: List<AnyType>): Never {
            TODO("Not yet implemented")
        }

        override fun curry(): IArrow<*> = this
        override fun substitute(substitution: Substitution): Case
            = Case(condition.substitute(substitution), result.substitute(substitution))

        override fun equals(other: Any?): Boolean = when (other) {
            is Case -> other.condition == condition && other.result == result
            else -> false
        }

        override fun prettyPrint(depth: Int): String
            = "case ($condition) -> $result"

        override fun toString(): String = prettyPrint()
    }

    data class Property(val name: String, val type: AnyType) : AnyType, Trait.Member {
        override val id: String = "$name: $type"

        override fun getCardinality(): ITypeCardinality
            = type.getCardinality()

        override fun substitute(substitution: Substitution): Property =
            Property(name, type.substitute(substitution))

        override fun equals(other: Any?): Boolean = when (other) {
            is Property -> name == other.name && type == other.type
            else -> false
        }

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()
            val prettyName = printer.apply(name, PrintableKey.Italics)

            return "$prettyName: $type"
        }

        override fun toString(): String = prettyPrint()
    }

    sealed interface IConstructor<T : AnyType> : IArrow<IConstructor<T>> {
        val constructedType: T
    }

    data class TupleConstructor(val left: AnyType, val right: AnyType, override val constructedType: Tuple) : IConstructor<Tuple> {
        override fun getDomain(): List<AnyType>
            = listOf(left, right)

        override fun getCodomain(): AnyType = constructedType

        override fun curry(): IArrow<*> = this

        override fun never(args: List<AnyType>): Never {
            TODO("Not yet implemented")
        }

        override val id: String = "(${left.id} * ${left.id}) -> ${constructedType.id}"

        override fun substitute(substitution: Substitution): IConstructor<Tuple>
            = TupleConstructor(left.substitute(substitution), right.substitute(substitution), constructedType.substitute(substitution))

        override fun prettyPrint(depth: Int): String
            = Arrow2(left, right, constructedType).prettyPrint(depth)

        override fun toString(): String
            = prettyPrint()
    }

    sealed interface IConstructableType<Self: IConstructableType<Self>> : ISpecialisedType {
        fun getConstructor(given: List<AnyType>) : IConstructor<Self>? {
            val constructors = getConstructors()

            return constructors.firstOrNull { it.getDomain() == given } as? IConstructor<Self>
        }
    }

    sealed interface ICaseIterable<Self: ICaseIterable<Self>> : AnyType {
        fun getCases(result: AnyType) : List<Case>
    }

    sealed interface IIndexType<I, Self : IIndexType<I, Self>> : AnyType {
        fun getElement(at: I): AnyType
    }

    interface IAlgebraicType<Self : IAlgebraicType<Self>> : AnyType, IConstructableType<Self> {
        override fun getTypeCheckPosition(): TypeCheckPosition
            = TypeCheckPosition.AlwaysLeft
    }

    sealed interface IProductType<I, Self : IProductType<I, Self>> : IAlgebraicType<Self>, IIndexType<I, Self>
    sealed interface ISumType<Self : ISumType<Self>> : IAlgebraicType<Self>, IIndexType<AnyType, Self>

    data class Tuple(val left: AnyType, val right: AnyType) : IProductType<Int, Tuple>, ICaseIterable<Tuple> {
        override val id: String = "(${left.id} * ${right.id})"

        override fun getUnsolvedTypeVariables(): List<TypeVar>
            = left.getUnsolvedTypeVariables() + right.getUnsolvedTypeVariables()

        override fun isSpecialised(): Boolean = when (left) {
            is ISpecialisedType -> left.isSpecialised()
            else -> when (right) {
                is ISpecialisedType -> right.isSpecialised()
                else -> false
            }
        }

        override fun getCases(result: AnyType): List<Case> {
            val leftCases = when (left) {
                is ICaseIterable<*> -> left.getCases(result)
                else -> listOf(Case(left, result))
            }

            val rightCases = when (right) {
                is ICaseIterable<*> -> right.getCases(result)
                else -> listOf(Case(right, result))
            }

            val cases = mutableListOf<Case>()
            for (lCase in leftCases) {
                for (rCase in rightCases) {
                    val nCase = Case(Tuple(lCase.condition, rCase.condition), result)
                    val allCases = cases.map { it.id }

                    if (!allCases.contains(nCase.id)) cases.add(nCase)
                }
            }

            return cases
        }

        private fun getLeftConstructors() : List<IConstructor<*>> = when (left) {
            is IConstructableType<*> -> left.getConstructors()
            else -> emptyList()
        }

        private fun getRightConstructors() : List<IConstructor<*>> = when (right) {
            is IConstructableType<*> -> right.getConstructors()
            else -> emptyList()
        }

        override fun getConstructors(): List<IConstructor<Tuple>> {
            val constructors = mutableListOf<TupleConstructor>()
            for (lConstructor in getLeftConstructors()) {
                for (rConstructor in getRightConstructors()) {
                    var lDomain = lConstructor.getDomain()
                    var rDomain = rConstructor.getDomain()

                    if (lDomain.count() > 1 || rDomain.count() > 1) TODO("2+-ary Tuple Constructors")

                    if (lDomain.isEmpty() && lConstructor is SingletonConstructor) {
                        lDomain = listOf(lConstructor.getCodomain())
                    }

                    if (rDomain.isEmpty() && rConstructor is SingletonConstructor) {
                        rDomain = listOf(rConstructor.getCodomain())
                    }

                    val constructor = TupleConstructor(lDomain[0], rDomain[0], this)

                    if (constructors.none { it.id == constructor.id }) {
                        constructors.add(constructor)
                    }
                }
            }

            return constructors
        }

        override fun getCardinality(): ITypeCardinality
            = left.getCardinality() + right.getCardinality()

        override fun getElement(at: Int): AnyType = when (at) {
            0 -> left
            1 -> right
            else -> Never("Attempt to retrieve element from Tuple at index $at")
        }

        override fun substitute(substitution: Substitution): Tuple
            = Tuple(left.substitute(substitution), right.substitute(substitution))

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
            = Tuple(left.flatten(from, env), right.flatten(from, env))

        override fun prettyPrint(depth: Int): String
            = "($left, $right)"

        override fun toString(): String = prettyPrint()
    }

    sealed interface IAccessibleType<I> : IType {
        fun access(at: I) : AnyType
    }

    data class Struct(val members: List<Pair<String, AnyType>>) : IProductType<String, Struct>, IAlgebraicType<Struct>, IAccessibleType<String> {
        override val id: String = "{${members.joinToString("; ") { it.second.id }}}"

        fun getProperties() : List<Property>
            = members.map { Property(it.first, it.second) }

        override fun access(at: String): AnyType = when (val member = members.firstOrNull { it.first == at }) {
            null -> Never("Unknown Member `${at}` for Type $this")
            else -> member.second
        }

        override fun getUnsolvedTypeVariables(): List<TypeVar>
            = members.flatMap { it.second.getUnsolvedTypeVariables() }

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType {
            if (from is Alias) {
                val projections = env.getProjections(from)
                for (projection in projections) {
                    val nProjection = Projection(from, projection.component.target)

                    GlobalEnvironment.add(nProjection, from)
                }
            }

            return this
        }

        override fun isSpecialised(): Boolean
            = members.any { it.second is ISpecialisedType && (it.second as ISpecialisedType).isSpecialised() }

        override fun getElement(at: String): AnyType = members.first { it.first == at }.second
        override fun getConstructors(): List<IConstructor<Struct>> = listOf(StructConstructor(this, members.map { it.second }))

        override fun substitute(substitution: Substitution): Struct {
            val nStruct = Struct(members.map { Pair(it.first, it.second.substitute(substitution)) })

            val tags = GlobalEnvironment.getTags(this)

            tags.forEach { GlobalEnvironment.tag(nStruct, it) }

            return nStruct
        }

        override fun getCardinality(): ITypeCardinality = when (members.isEmpty()) {
            true -> ITypeCardinality.Mono
            else -> members.map { it.second.getCardinality() }.reduce(ITypeCardinality::plus)
        }

        override fun equals(other: Any?): Boolean = when (other) {
            is Struct -> other.members.count() == members.count() && other.members.zip(members).all { it.first == it.second }
            else -> false
        }

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()
            val prettyMembers = members.joinToString(", ") {
                val prettyName = printer.apply(it.first, PrintableKey.Italics)

                "$prettyName : ${it.second}"
            }

            return "$indent{ $prettyMembers }"
        }

        override fun toString(): String = prettyPrint()
    }

    data class StructConstructor(override val constructedType: Struct, val args: List<AnyType>) : IConstructor<Struct> {
        override val id: String = "(${args.joinToString(", ") { it.id }}) -> ${constructedType.id}"

        override fun getDomain(): List<AnyType> = args
        override fun getCodomain(): AnyType = constructedType
        override fun getCardinality(): ITypeCardinality
            = constructedType.getCardinality()

        override fun curry(): IArrow<*> = this

        override fun substitute(substitution: Substitution): IConstructor<Struct> =
            StructConstructor(constructedType.substitute(substitution), args.map { it.substitute(substitution) })

        override fun never(args: List<AnyType>): Never =
            Never("Cannot construct Type $constructedType with arguments (${args.joinToString("; ")})")

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

        override fun never(args: List<AnyType>): Never =
            Never("Union Type $constructedType cannot be constructed with argument $arg")

        override fun toString(): String = prettyPrint()
    }

    data class Union(val left: AnyType, val right: AnyType) : ISumType<Union>, IAlgebraicType<Union>, ICaseIterable<Union> {
        override val id: String = "(${left.id} | ${right.id})"

        override fun isSpecialised(): Boolean = when (left) {
            is ISpecialisedType -> left.isSpecialised()
            else -> when (right) {
                is ISpecialisedType -> right.isSpecialised()
                else -> false
            }
        }

        override fun getCardinality(): ITypeCardinality = when (left) {
            is Union -> left.left.getCardinality() + left.right.getCardinality() + right.getCardinality()
            else -> left.getCardinality() + right.getCardinality()
        }

        override fun getCases(result: AnyType): List<Case> {
            val lCases = when (left) {
                is ICaseIterable<*> -> left.getCases(result)
                else -> listOf(Case(left, result))
            }

            val rCases = when (right) {
                is ICaseIterable<*> -> right.getCases(result)
                else -> listOf(Case(right, result))
            }

            return lCases + rCases
        }

        override fun getElement(at: AnyType): AnyType = when (at) {
            left -> left
            right -> right
            else -> Never("Sum Type $this will never contain a value of Type $at")
        }

        override fun getConstructors(): List<IConstructor<Union>> {
            val lConstructors = when (left.flatten(this, GlobalEnvironment)) {
                is IConstructableType<*> -> left.getConstructors()
                else -> TODO("HERE!!!")
            }

            val rConstructors = when (right) {
                is IConstructableType<*> -> right.getConstructors()
                else -> TODO("HERE!!!")
            }

            return lConstructors.zip(rConstructors).map {
                // NOTE - Kotlin won't accept casting `it.first/second` directly to SingletonConstructor for some reason, so we need to erase the type
                val lConstructor = it.first
                val rConstructor = it.second

                val lDomain = when (lConstructor) {
                    is SingletonConstructor -> lConstructor.getCodomain()
                    else -> lConstructor.getDomain()[0]
                }

                val rDomain = when (rConstructor) {
                    is SingletonConstructor -> rConstructor.getCodomain()
                    else -> rConstructor.getDomain()[0]
                }

                UnionConstructor(Union(lDomain, rDomain), this)
            }
        }

        override fun substitute(substitution: Substitution): Union =
            Union(left.substitute(substitution), right.substitute(substitution))

        override fun equals(other: Any?): Boolean = when (other) {
            is Union -> left == other.left && right == other.right
            is AnyType -> left == other || right == other
            else -> false
        }

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
            = Union(left.flatten(from, env), right.flatten(from, env))

        override fun prettyPrint(depth: Int): String
            = "${"\t".repeat(depth)}($left | $right)"

        override fun toString(): String = prettyPrint()
    }

    sealed interface IOperatorArrow<A: IArrow<A>, Self: IOperatorArrow<A, Self>> : IArrow<Self> {
        val fixity: OperatorFixity
        val symbol: String
        val identifier: String
        val arrow: A

        override val id: String get() = "$identifier:${arrow.id}"

        override fun getDomain(): List<AnyType> = arrow.getDomain()
        override fun getCodomain(): AnyType = arrow.getCodomain()
        override fun curry(): IArrow<*> = arrow.curry()
        override fun never(args: List<AnyType>): Never = Never("")
        override fun getCardinality(): ITypeCardinality
            = arrow.getCodomain().getCardinality()

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()
            val prettyName = printer.apply("${fixity.name} $identifier `$symbol`", PrintableKey.Italics)

            return "$prettyName $arrow"
        }
    }

    data class PrefixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow1) :
        IOperatorArrow<Arrow1, PrefixOperator> {
        override val fixity: OperatorFixity = OperatorFixity.Prefix

        override fun substitute(substitution: Substitution): PrefixOperator
            = PrefixOperator(symbol, identifier, arrow.substitute(substitution))

        override fun toString(): String = prettyPrint()
    }

    data class PostfixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow1) :
        IOperatorArrow<Arrow1, PostfixOperator> {
        override val fixity: OperatorFixity = OperatorFixity.Postfix

        override fun substitute(substitution: Substitution): PostfixOperator
            = PostfixOperator(symbol, identifier, arrow.substitute(substitution))

        override fun toString(): String = prettyPrint()
    }

    data class InfixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow2) :
        IOperatorArrow<Arrow2, InfixOperator> {
        override val fixity: OperatorFixity = OperatorFixity.Infix

        override fun substitute(substitution: Substitution): InfixOperator
            = InfixOperator(symbol, identifier, arrow.substitute(substitution))

        override fun toString(): String = prettyPrint()
    }

    sealed interface IArrow<Self : IArrow<Self>> : AnyType {
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

        override fun prettyPrint(depth: Int): String {
            val domainString = getDomain().joinToString(", ")

            return "${"\t".repeat(depth)}($domainString) -> ${getCodomain()}"
        }
    }

    data class Arrow0(val gives: AnyType) : IArrow<Arrow0> {
        override val id: String = "() -> ${gives.id}"

        override fun getDomain(): List<AnyType> = emptyList()
        override fun getCodomain(): AnyType = gives

        override fun substitute(substitution: Substitution): Arrow0 = Arrow0(gives.substitute(substitution))
        override fun curry(): Arrow0 = this
        override fun never(args: List<AnyType>): Never = Never("Unreachable")

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
            = Arrow0(gives.flatten(from, env))

        override fun equals(other: Any?): Boolean = when (other) {
            is Arrow0 -> gives == other.gives
            else -> false
        }

        override fun toString(): String = prettyPrint()
    }

    data class Arrow1(val takes: AnyType, val gives: AnyType) : IArrow<Arrow1> {
        override val id: String = "(${takes.id}) -> ${gives.id}"

        override fun getDomain(): List<AnyType> = listOf(takes)
        override fun getCodomain(): AnyType = gives

        override fun substitute(substitution: Substitution): Arrow1 =
            Arrow1(takes.substitute(substitution), gives.substitute(substitution))

        override fun curry(): Arrow0 = Arrow0(Arrow1(takes, gives))

        override fun never(args: List<AnyType>): Never =
            Never("$id expects argument of Type $takes, found $args[0]")

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType {
            val domain = takes.flatten(from, env)

            if (domain is Never) return domain

            val codomain = gives.flatten(from, env)

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

        override fun toString(): String = prettyPrint()
    }

    data class Arrow2(val a: AnyType, val b: AnyType, val gives: AnyType) : IArrow<Arrow2> {
        override val id: String = "(${a.id}, ${b.id}) -> ${gives.id}"

        override fun getDomain(): List<AnyType> = listOf(a, b)
        override fun getCodomain(): AnyType = gives

        override fun substitute(substitution: Substitution): Arrow2 =
            Arrow2(a.substitute(substitution), b.substitute(substitution), gives.substitute(substitution))

        override fun curry(): Arrow1 = Arrow1(a, Arrow1(b, gives))

        override fun never(args: List<AnyType>): Never =
            Never("$this expects arguments of ($a, $b), found (${args.joinToString(", ")})")

        override fun toString(): String = prettyPrint()
    }

    data class Arrow3(val a: AnyType, val b: AnyType, val c: AnyType, val gives: AnyType) : IArrow<Arrow3> {
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
            Never("$id expects arguments of ($a, $b, $c), found (${args.joinToString(", ")})")

        override fun toString(): String = prettyPrint()
    }

    data class Signature(val receiver: AnyType, val name: String, val parameters: List<AnyType>, val returns: AnyType, val isInstanceSignature: Boolean) : IArrow<Signature>, Trait.Member {
        override val id: String get() {
            val pParams = parameters.joinToString(", ") { it.id }

            return "$receiver.$name($pParams)($returns)"
        }

        override fun getDomain(): List<AnyType> = toArrow().getDomain()
        override fun getCodomain(): AnyType = toArrow().getCodomain()

        override fun curry(): IArrow<*> = toArrow().curry()
        override fun never(args: List<AnyType>): Never = toArrow().never(args)

        override fun getCardinality(): ITypeCardinality
            = returns.getCardinality()

        private fun toInstanceArrow() = when (parameters.count()) {
            0 -> Arrow1(receiver, returns)
            1 -> Arrow2(receiver, parameters[0], returns)
            2 -> Arrow3(receiver, parameters[0], parameters[1], returns)
            else -> TODO("3+-ary instance Arrows")
        }

        fun toStaticArrow(): AnyArrow = parameters.arrowOf(returns)

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

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val prettyParams = parameters.joinToString(", ") { it.prettyPrint(0) }
            val printer = getKoinInstance<Printer>()
            val prettyName = printer.apply(name, PrintableKey.Italics)

            return "$indent(${receiver.prettyPrint(0)}) $prettyName ($prettyParams) (${returns.prettyPrint(0)})"
        }

        override fun toString(): String = prettyPrint()
    }

    data class TypeVar(val name: String, val constraints: List<ITypeConstraint> = emptyList()) : AnyType, IConstructableType<TypeVar>, ITypeConstraint {
        override val id: String = "?$name"
        override val type: AnyType = this

        override fun isSolvedBy(input: AnyType, env: ITypeEnvironment): Boolean
            = constraints.all { it.isSolvedBy(input, env) }

        override fun getUnsolvedTypeVariables(): List<TypeVar> = listOf(this)
        override fun isSpecialised(): Boolean = false
        override fun getConstructors(): List<IConstructor<TypeVar>> = listOf(SingletonConstructor(this) as IConstructor<TypeVar>)
        override fun getCanonicalName(): String = name
        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Infinite
        override fun substitute(substitution: Substitution): AnyType = when (substitution.old) {
            is TypeVar -> when (substitution.old.name == name) {
                true -> when (constraints.isEmpty()) {
                    true -> substitution.new
                    else -> {
                        val constraint = constraints.reduce(ITypeConstraint::plus)

                        when (constraint.isSolvedBy(substitution.new, GlobalEnvironment)) {
                            true -> substitution.new
                            else -> Never("")
                        }
                    }
                }
                else -> this
            }

            else -> this
        }

        override fun equals(other: Any?): Boolean = when (other) {
            is TypeVar -> name == other.name
            else -> false
        }

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()
            val path = OrbitMangler.unmangle(name)
            val simpleName = path.last()
            val prettyName = printer.apply("?$simpleName", PrintableKey.Bold, PrintableKey.Italics)

            return "$indent$prettyName"
        }

        override fun toString(): String = prettyPrint()
    }

    data class PatternBinding(val name: String, val type: AnyType) : IType {
        override val id: String = "$name => $type"

        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Zero
        override fun substitute(substitution: Substitution): AnyType
            = PatternBinding(name, type.substitute(substitution))
    }

    data class Trait(override val id: String, val properties: List<Property>, val signatures: List<Signature>) : Entity<Trait> {
        sealed interface Member

        override fun substitute(substitution: Substitution): Trait
            = Trait(id, properties.map { it.substitute(substitution) }, signatures.map { it.substitute(substitution) })

        override fun getCardinality(): ITypeCardinality
            = ITypeCardinality.Zero

        override fun equals(other: Any?): Boolean = when (other) {
            is Trait -> other.id == id
            else -> false
        }

        private fun isImplementedBy(struct: Struct, env: ITypeEnvironment) : Boolean {
            val projections = GlobalEnvironment.getProjectedTags(struct)

            return projections.any { it.component.target.id == id }
        }

        fun isImplementedBy(type: AnyType, env: ITypeEnvironment) : Boolean {
            val type = type.flatten(type, env)
            if (type is Trait) return type == this
            if (type is Struct) return isImplementedBy(type, env)

            val projections = env.getProjections(type) + when (env) {
                is ProjectionEnvironment -> listOf(ContextualDeclaration(env.getCurrentContext(), env.projection))
                else -> emptyList()
            }

            return projections.any { it.component.target.id == id }
        }

        operator fun plus(other: Trait) : Trait
            = Trait("$id*${other.id}", properties + other.properties, signatures + other.signatures)

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()
            val path = OrbitMangler.unmangle(id)
            val simpleName = path.last()

            return "$indent${printer.apply(simpleName, PrintableKey.Bold)}"
        }

        override fun toString(): String = prettyPrint()
    }

    val id: String
    val index: Int get() = TypeIndexer.next()

    fun getCanonicalName() : String = id
    fun flatten(from: AnyType, env: ITypeEnvironment) : AnyType = this
    fun getTypeCheckPosition() : TypeCheckPosition = TypeCheckPosition.Any
    fun getCardinality() : ITypeCardinality
    fun getConstructors() : List<IConstructor<*>> = emptyList()
    fun getUnsolvedTypeVariables() : List<IType.TypeVar> = emptyList()

    fun prettyPrint(depth: Int = 0) : String {
        val indent = "\t".repeat(depth)

        return "$indent$id"
    }
}

sealed interface IIntrinsicType : IType {
    sealed interface IIntegralType<Self: IIntegralType<Self>> : IType.IConstructableType<Self> {
        val maxValue: Int
        val minValue: Int
    }

    // 32-bit Signed Integer (Kotlin default)
    object RawInt : IIntrinsicType, IIntegralType<RawInt> {
        private object RawIntConstructor : IType.IConstructor<RawInt> {
            override val id: String = "(ℤ) -> ℤ"

            override val constructedType: RawInt = RawInt
            override fun getDomain(): List<AnyType> = listOf(RawInt)
            override fun getCodomain(): AnyType = RawInt
            override fun curry(): IType.IArrow<*> = this

            override fun never(args: List<AnyType>): IType.Never {
                TODO("Not yet implemented")
            }

            override fun substitute(substitution: Substitution): AnyType = this
            override fun equals(other: Any?): Boolean = other is RawIntConstructor
        }

        override val id: String = "ℤ"
        override val maxValue: Int = Int.MAX_VALUE
        override val minValue: Int = Int.MIN_VALUE

        override fun isSpecialised(): Boolean = false

        override fun equals(other: Any?): Boolean = when (other) {
            is RawInt -> true
            OrbCoreNumbers.intType -> true
            else -> false
        }

        override fun getConstructors(): List<IType.IConstructor<RawInt>> = listOf(RawIntConstructor)
        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Infinite

        override fun substitute(substitution: Substitution): AnyType = when (substitution.old) {
            this -> substitution.new
            else -> this
        }

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()

            return "$indent${printer.apply(id, PrintableKey.Bold)}"
        }

        override fun toString(): String = prettyPrint()
    }
}

sealed interface IValue<T: AnyType, V> : IType {
    val type: T
    val value: V

    override val id: String get() = "$value : $type"

    override fun substitute(substitution: Substitution): AnyType = type
    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "$indent$value"
    }
}

data class IntValue(override val value: Int) : IValue<IType.Type, Int> {
    override val type: IType.Type = OrbCoreNumbers.intType

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "$indent$value"
    }

    override fun toString(): String = prettyPrint()
}

object TrueValue : IValue<IType.Type, Boolean> {
    override val type: IType.Type = OrbCoreBooleans.trueType
    override val value: Boolean = true

    override fun toString(): String = prettyPrint()
}

object FalseValue : IValue<IType.Type, Boolean> {
    override val type: IType.Type = OrbCoreBooleans.falseType
    override val value: Boolean = false

    override fun toString(): String = prettyPrint()
}

data class InstanceValue(override val type: IType.IConstructableType<*>, override val value: Map<String, IValue<*, *>>) : IValue<IType.IConstructableType<*>, Map<String, IValue<*, *>>>, IType.IAccessibleType<String> {
    override fun access(at: String): AnyType = when (val member = value[at]) {
        null -> IType.Never("Unknown Member `$at` for compile-time instance of Structural Type $type")
        else -> member
    }

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = type.flatten(from, env)

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val pretty = value.map { "${it.key}: ${it.value}" }.joinToString(", ")

        return "$indent{$pretty}"
    }

    override fun toString(): String = prettyPrint()
}

typealias AnyType = IType
typealias AnyOperator = IType.IOperatorArrow<*, *>
typealias AnyMetaType = IType.IMetaType<*>
typealias AnyInt = IIntrinsicType.IIntegralType<*>

fun List<AnyType>.arrowOf(codomain: AnyType) : AnyArrow = when (count()) {
    0 -> IType.Arrow0(codomain)
    1 -> IType.Arrow1(this[0], codomain)
    2 -> IType.Arrow2(this[0], this[1], codomain)
    3 -> IType.Arrow3(this[0], this[1], this[2], codomain)
    else -> TODO("4+-ary Arrows")
}