package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.intrinsics.OrbCoreBooleans
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeCheckPosition
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.IIntrinsicOperator
import org.orbit.core.nodes.AttributeOperator
import org.orbit.core.nodes.INode
import org.orbit.core.nodes.OperatorFixity
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance
import org.w3c.dom.Attr
import java.util.Arrays

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

interface IType : IContextualComponent, Substitutable<AnyType> {
    sealed interface Entity<E : Entity<E>> : IType
    sealed interface IMetaType<M: IMetaType<M>> : Entity<M> {
        fun toBoolean() : Boolean = when (this) {
            is Always -> true
            is Never -> false
        }

        operator fun plus(other: IMetaType<*>) : IMetaType<*>
    }

    object Always : IMetaType<Always> {
        override val id: String = "Any"
        override fun substitute(substitution: Substitution): Always = this
        override fun plus(other: IMetaType<*>): IMetaType<*> = other
        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono
        override fun equals(other: Any?): Boolean = true
        override fun toString(): String = "Any"
    }

    data class Never(val message: String, override val id: String = "!") : IMetaType<Never>, IArrow<Never> {
        fun panic(node: INode? = null): Nothing = when (node) {
            null -> throw getKoinInstance<Invocation>().make<TypeSystem>(message)
            else -> throw getKoinInstance<Invocation>().make<TypeSystem>(message, node)
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

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()

            return printer.apply(message, PrintableKey.Error)
        }

        override fun toString(): String = prettyPrint()
    }

    object Unit : Entity<Unit> {
        override val id: String = "Unit"

        val path: Path = OrbitMangler.unmangle("Orb::Core::Types::Unit")

        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono
        override fun substitute(substitution: Substitution): Unit = this
        override fun equals(other: Any?): Boolean = when (other) {
            is Unit -> true
            else -> false
        }

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()

            return printer.apply(OrbCoreTypes.unitType.getCanonicalName(), PrintableKey.Bold)
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

    data class Lazy<T: AnyType>(val name: String, val type: () -> T) : IType {
        override val id: String = "⎡$name⎦"

        override fun getCardinality(): ITypeCardinality
            = type().getCardinality()

        override fun substitute(substitution: Substitution): AnyType
            = Lazy(name) { type().substitute(substitution) }

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()
            val pretty = printer.apply(name, PrintableKey.Bold)

            return "${"\t".repeat(depth)}⎡$pretty⎦"
        }

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType = type().flatten(from, env)

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
            val pretty = printer.apply(name, PrintableKey.Bold)

            return "$indent$pretty"
        }

        override fun toString(): String = prettyPrint()
    }

    data class Sum(val left: AnyType, val right: AnyType) : IConstructableType<Sum> {
        override val id: String = "(${left.id} | ${right.id})"

        override fun getConstructors(): List<IConstructor<*>>
            = left.getConstructors() + right.getConstructors()

        override fun getUnsolvedTypeVariables(): List<TypeVar>
            = left.getUnsolvedTypeVariables() + right.getUnsolvedTypeVariables()

        override fun isSpecialised(): Boolean = false

        override fun getCardinality(): ITypeCardinality
            = ITypeCardinality.Finite(2)

        override fun substitute(substitution: Substitution): AnyType
            = Sum(left.substitute(substitution), right.substitute(substitution))

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)

            return "$indent($left | $right)"
        }

        override fun toString(): String
            = prettyPrint()
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

    data class Forward(val name: String) : IType {
        override val id: String = name

        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Zero
        override fun substitute(substitution: Substitution): AnyType = this
        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
            = env.getTypeOrNull(name)
                ?.component
                ?.flatten(from, env)
                ?: throw Exception("HERE: $name")

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()
            val pretty = printer.apply(name, PrintableKey.Bold)

            return "$indent$pretty"
        }

        override fun toString(): String
            = prettyPrint()
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

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
            = Case(condition.flatten(from, env), result.flatten(from, env))

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

    data class Attribute(val name: String, val typeVariables: List<TypeVar>, val constraint: (IMutableTypeEnvironment) -> IMetaType<*>) : IType {
        sealed interface IAttributeApplication : IType {
            fun invoke(env: IMutableTypeEnvironment) : IMetaType<*>

            fun combine(op: AttributeOperator, other: IAttributeApplication) : IAttributeApplication
                = CompoundApplication(op, this, other)
        }

        data class CompoundApplication(val op: AttributeOperator, val left: IAttributeApplication, val right: IAttributeApplication) : IAttributeApplication {
            override val id: String = "${left.id} $op ${right.id}"

            override fun invoke(env: IMutableTypeEnvironment): IMetaType<*>
                = op.apply(left, right, env)

            override fun getCardinality(): ITypeCardinality
                = left.getCardinality() + right.getCardinality()

            override fun substitute(substitution: Substitution): AnyType
                = CompoundApplication(op, left.substitute(substitution) as IAttributeApplication, right.substitute(substitution) as IAttributeApplication)
        }

        data class Application(val attribute: Attribute, val args: List<AnyType>) : IAttributeApplication {
            override val id: String = attribute.id

            override fun invoke(env: IMutableTypeEnvironment) : IMetaType<*> {
                if (args.count() != attribute.typeVariables.count()) {
                    return Never("Attribute `${attribute.name}` expects ${attribute.typeVariables.count()} arguments, found ${args.count()}")
                }

                val nEnv = env.fork()
                attribute.typeVariables.zip(args).forEach {
                    nEnv.add(Alias(it.first.name, it.second))
                }

                return attribute.constraint(nEnv)
            }

            override fun getCardinality(): ITypeCardinality
                = attribute.getCardinality()

            // NOTE - We purposefully avoid substituting `attribute` here otherwise our abstract TypeVars are erased
            override fun substitute(substitution: Substitution): AnyType
                = Application(attribute, args.substitute(substitution))
        }

        override val id: String = "$name : (${typeVariables.joinToString(", ")}) => ?"

        operator fun plus(other: Attribute) : Attribute
            = Attribute("$name • ${other.name}", (typeVariables + other.typeVariables).distinct()) { constraint(it) + other.constraint(it) }

        override fun getCardinality(): ITypeCardinality
            = ITypeCardinality.Zero

        override fun substitute(substitution: Substitution): AnyType
            = Attribute(name, typeVariables.substitute(substitution) as List<TypeVar>, constraint)
    }

    sealed interface IArrayConstructor : IConstructor<Array> {
        data class Empty(override val constructedType: Array) : IArrayConstructor {
            override val id: String = "[]"

            override fun getDomain(): List<AnyType> = emptyList()
            override fun getCodomain(): AnyType = Array(constructedType.element, Array.Size.Fixed(0))
            override fun curry(): IArrow<*> = this

            override fun never(args: List<AnyType>): Never {
                TODO("Not yet implemented")
            }

            override fun substitute(substitution: Substitution): AnyType
                = Empty(constructedType.substitute(substitution) as Array)
        }

        data class Populated(override val constructedType: Array, private val dynamicSize: Int? = null) : IArrayConstructor {
            override val id: String = "[]"

            override fun getDomain(): List<AnyType> {
                val size = dynamicSize ?: when (constructedType.size) {
                    is Array.Size.Fixed -> constructedType.size.size
                    else -> TODO("WEIRD ARRAY ERROR")
                }

                return (0 until size).map { constructedType.element }
            }

            override fun getCodomain(): AnyType = constructedType
            override fun curry(): IArrow<*> = this

            override fun never(args: List<AnyType>): Never {
                TODO("Not yet implemented")
            }

            override fun substitute(substitution: Substitution): AnyType
                = Populated(constructedType.substitute(substitution) as Array)
        }
    }

    data class Array(val element: AnyType, val size: Size) : IType, ISpecialisedType, IConstructableType<Array> {
        sealed interface Size {
            data class Fixed(val size: Int) : Size {
                override fun equals(other: kotlin.Any?): Boolean = when (other) {
                    is Fixed -> other.size == size
                    is Any -> true
                    else -> false
                }

                override fun hashCode(): Int = size

                override fun toString(): String = "$size"
            }

            object Any : Size {
                override fun equals(other: kotlin.Any?): Boolean = when (other) {
                    is Size -> true
                    else -> false
                }

                override fun toString(): String = "∞"
            }
        }

        override val id: String = "[${element.id};$size]"

        override fun getConstructors(): List<IConstructor<*>> = when (size) {
            is Size.Fixed -> when (size.size) {
                0 -> listOf(IArrayConstructor.Empty(this))
                else -> listOf(IArrayConstructor.Empty(this), IArrayConstructor.Populated(this))
            }

            is Size.Any -> listOf(IArrayConstructor.Empty(this))
        }

        override fun getConstructor(given: List<AnyType>): IConstructor<Array>? = when (given.count()) {
            0 -> getConstructors()[0] as IConstructor<Array>
            else -> when (size) {
                is Size.Fixed -> getConstructors()[1] as IConstructor<Array>
                is Size.Any -> IArrayConstructor.Populated(this, given.count())
            }
        }

        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Infinite
        override fun substitute(substitution: Substitution): AnyType
            = Array(element.substitute(substitution), size)

        override fun getUnsolvedTypeVariables(): List<TypeVar>
            = element.getUnsolvedTypeVariables()

        override fun isSpecialised(): Boolean = when (element) {
            is ISpecialisedType -> element.isSpecialised()
            else -> false
        }

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)

            return "$indent[$element;$size]"
        }

        override fun toString(): String
            = prettyPrint()
    }

    interface IConstructor<T : AnyType> : IArrow<IConstructor<T>> {
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

            return constructors.firstOrNull {
                if (it.getDomain().count() != given.count()) return@firstOrNull false

                it.getDomain().zip(given).all { p -> TypeUtils.checkEq(GlobalEnvironment, p.first, p.second) }
            } as? IConstructor<Self>
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

    data class UnionConstructor(val name: String, override val constructedType: Lazy<Union>, val arg: AnyType) : IConstructor<Lazy<Union>>, ICaseIterable<Union> {
        data class ConcreteUnionConstructor(val lazyConstructor: UnionConstructor) : IConstructor<Union> {
            override val id: String = "(${lazyConstructor.arg}) -> $constructedType"
            val name: String = lazyConstructor.name

            override fun getCanonicalName(): String = name

            override val constructedType: Union get() = lazyConstructor.constructedType.type()
            override fun getDomain(): List<AnyType> = listOf(lazyConstructor.arg)
            override fun getCodomain(): AnyType = constructedType

            override fun curry(): IArrow<*> {
                TODO("Not yet implemented")
            }

            override fun never(args: List<AnyType>): Never {
                TODO("Not yet implemented")
            }

            override fun substitute(substitution: Substitution): AnyType
                = ConcreteUnionConstructor(lazyConstructor.constructedType.type().substitute(substitution) as UnionConstructor)

            override fun prettyPrint(depth: Int): String {
                val printer = getKoinInstance<Printer>()

                return printer.apply(name, PrintableKey.Bold)
            }

            override fun toString(): String = prettyPrint()
        }

        override val id: String = "$name :: (${arg.id}) -> $constructedType"

        override fun getCanonicalName(): String = name

        override fun getDomain(): List<AnyType> = listOf(arg)
        override fun getCodomain(): AnyType = constructedType
        override fun getCardinality(): ITypeCardinality
            = arg.getCardinality()

        override fun getCases(result: AnyType): List<Case> = when (arg) {
            is ICaseIterable<*> -> arg.getCases(result)
            else -> listOf(Case(arg, result))
        }

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
            = ConcreteUnionConstructor(this)

        override fun curry(): IArrow<*> = this

        override fun substitute(substitution: Substitution): IConstructor<Lazy<Union>> =
            UnionConstructor(name, constructedType.substitute(substitution) as Lazy<Union>, arg.substitute(substitution))

        override fun never(args: List<AnyType>): Never =
            Never("Union Type $constructedType cannot be constructed with argument $arg")

        override fun equals(other: Any?): Boolean = when (other) {
            is UnionConstructor -> other.name == name || other.constructedType == constructedType
            is ConcreteUnionConstructor -> other.lazyConstructor === this
            else -> false
        }

        override fun toString(): String = prettyPrint()
    }

    data class Union(val unionConstructors: List<UnionConstructor>) : ISumType<Union>, IAlgebraicType<Union>, ICaseIterable<Union> {
        constructor() : this(emptyList())

        override val id: String get() {
            val pretty = unionConstructors.joinToString(" | ")

            return "($pretty)"
        }

        override fun isSpecialised(): Boolean = false

        override fun getCardinality(): ITypeCardinality
            = unionConstructors.fold(ITypeCardinality.Zero as ITypeCardinality) { acc, next -> acc + next.getCardinality() }

        override fun getCases(result: AnyType): List<Case>
            = unionConstructors.fold(emptyList()) { acc, next -> acc + Case(next.arg, this) }

        override fun getElement(at: AnyType): AnyType {
            for (constructor in unionConstructors) {
                if (at == constructor) return constructor
            }

            return Never("Sum Type $this will never contain a value of Type $at")
        }

        override fun getConstructors(): List<IConstructor<Union>>
            = unionConstructors.map { UnionConstructor.ConcreteUnionConstructor(it) }

        override fun substitute(substitution: Substitution): Union =
            Union(unionConstructors.substitute(substitution) as List<UnionConstructor>)

        override fun equals(other: Any?): Boolean = when (other) {
            is Union -> other.unionConstructors == unionConstructors
            is AnyType -> other in unionConstructors
            else -> false
        }

        override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
            = Union(unionConstructors.map { when (val uc = it.flatten(from, env)) {
                is UnionConstructor -> uc
                is UnionConstructor.ConcreteUnionConstructor -> uc.lazyConstructor
                else -> TODO("Not a Union Constructor")
            } })

        override fun prettyPrint(depth: Int): String = when (val name = GlobalEnvironment.getUnionName(this)) {
            null -> {
                val printer = getKoinInstance<Printer>()
                val pretty = unionConstructors.joinToString(" | ") { printer.apply(it.name, PrintableKey.Bold) }

                "${"\t".repeat(depth)}($pretty)"
            }
            else -> {
                val printer = getKoinInstance<Printer>()
                val pretty = printer.apply(name, PrintableKey.Bold)

                "${"\t".repeat(depth)}$pretty"
            }
        }

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

    data class ConstrainedArrow(val arrow: AnyArrow, val constraints: List<Attribute.IAttributeApplication>) : IArrow<ConstrainedArrow>, IConstructableType<ConstrainedArrow> {
        override val id: String = "$arrow + ${constraints.joinToString(", ")}"

        override fun isSpecialised(): Boolean = false

        override fun getDomain(): List<AnyType>
            = arrow.getDomain()

        override fun getCodomain(): AnyType
            = arrow.getCodomain()

        override fun getCardinality(): ITypeCardinality
            = arrow.getCardinality()

        override fun curry(): IArrow<*>
            = arrow.curry()

        override fun never(args: List<AnyType>): Never
            = arrow.never(args)

        override fun substitute(substitution: Substitution): AnyType
            = ConstrainedArrow(arrow.substitute(substitution) as AnyArrow, constraints.substitute(substitution) as List<Attribute.Application>)

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val prettyDomain = arrow.getDomain().joinToString(", ")
            val prettyArrow = "($prettyDomain) => ${arrow.getCodomain()}"

            return when (constraints.isEmpty()) {
                true -> "$indent$prettyArrow"
                else -> {
                    val pretty = constraints.joinToString(" & ")

                    "$indent$prettyArrow where $pretty"
                }
            }
        }

        override fun toString(): String
            = prettyPrint()
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

        override fun curry(): Arrow0
            = Arrow0(Arrow1(takes, gives))

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

        override fun getUnsolvedTypeVariables(): List<TypeVar>
            = (receiver.getUnsolvedTypeVariables() + returns.getUnsolvedTypeVariables() + parameters.flatMap { it.getUnsolvedTypeVariables() })
                .distinct()

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
        constructor(pair: Pair<String, AnyType>) : this(pair.first, pair.second)

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
            = ITypeCardinality.Infinite

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
        val printer = getKoinInstance<Printer>()
        val indent = "\t".repeat(depth)
        val pretty = printer.apply("$value", PrintableKey.Bold)

        return "$indent$pretty"
    }

    override fun toString(): String = prettyPrint()
}

object TrueValue : IValue<IType.Type, Boolean> {
    override val type: IType.Type = OrbCoreBooleans.trueType.flatten(IType.Always, GlobalEnvironment) as IType.Type
    override val value: Boolean = true

    override fun toString(): String = prettyPrint()
}

object FalseValue : IValue<IType.Type, Boolean> {
    override val type: IType.Type = OrbCoreBooleans.falseType.flatten(IType.Always, GlobalEnvironment) as IType.Type
    override val value: Boolean = false

    override fun toString(): String = prettyPrint()
}

data class ArrayValue(override val type: IType.Array, override val value: List<AnyType>) : IValue<IType.Array, List<AnyType>> {
    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()
        val indent = "\t".repeat(depth)
        val pretty = value.joinToString(", ")
        val open = printer.apply("[", PrintableKey.Bold)
        val close = printer.apply("]", PrintableKey.Bold)

        return "$indent$open$pretty$close"
    }

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