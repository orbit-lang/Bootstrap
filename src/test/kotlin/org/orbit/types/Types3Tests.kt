package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.util.assertIs
import kotlin.reflect.KProperty

typealias AnyType = Types3Tests.IType<*>
typealias AnyEntity = Types3Tests.IType.Entity<*>
typealias AnyArrow = Types3Tests.IType.IArrow<*>
typealias AnyExpr = Types3Tests.Expr<*>

fun <S: Types3Tests.Substitutable<S>> List<S>.substituteAll(substitution: Types3Tests.Substitution) : List<S>
    = map { it.substitute(substitution) }

class Types3Tests {
    sealed interface IType<T: IType<T>> : Substitutable<T> {
        interface UnifiableType<Self: UnifiableType<Self>> : IType<Self> {
            fun unify(env: Env, other: UnifiableType<*>) : UnifiableType<*>
        }

        sealed interface Entity<E: Entity<E>> : UnifiableType<E>

        data class Never(val message: String, override val id: String = "!") : Entity<Never> {
            fun panic() : Nothing = throw RuntimeException(message)

            override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
                is Never -> Never("$message\n${other.message}", "$id:${other.id}")
                else -> this
            }

            override fun substitute(substitution: Substitution): Never = this
            override fun equals(other: Any?): Boolean = this === other
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

        data class Type(override val id: String) : Entity<Type> {
            companion object {
                val self = Type("__Self")
            }

            fun api(env: Env) : ITrait = ITrait.MembershipTrait("$id.__api", env.getMembers(this))

            override fun unify(env: Env, other: UnifiableType<*>) : UnifiableType<*> = when (other) {
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

            override fun substitute(substitution: Substitution): Member
                = Member(name, type.substitute(substitution), owner)

            override fun equals(other: Any?): Boolean = when (other) {
                is Member -> name == other.name && type == other.type
                else -> false
            }
        }

        sealed interface IConstructor<T: AnyType> : IArrow<IConstructor<T>> {
            val constructedType: T
        }

        sealed interface ICompositeType<Self: ICompositeType<Self>> : Entity<Self>

        sealed interface IIndexType<I, Self: IIndexType<I, Self>> : IType<Self> {
            fun getElement(at: I) : AnyType
        }

        interface IConstructableType<Self: IConstructableType<Self>> : IType<Self> {
            fun getConstructors() : List<IConstructor<Self>>
        }

        sealed interface IProductType<I, Self: IProductType<I, Self>> : ICompositeType<Self>, IIndexType<I, Self>
        sealed interface ISumType<Self: ISumType<Self>> : ICompositeType<Self>, IIndexType<AnyType, Self>

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

            override fun substitute(substitution: Substitution): Tuple
                = Tuple(elementTypes.substituteAll(substitution))

            override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
                is Tuple -> Tuple(this, other)
                else -> Never("Cannot unify Types $id & ${other.id}")
            }
        }

        data class Struct(val members: List<Member>) : IProductType<String, Struct>, IConstructableType<Struct> {
            override val id: String = "{${members.joinToString("; ") { it.id }}}"

            override fun getElement(at: String): AnyType
                = members.first { it.name == at }

            override fun getConstructors(): List<IConstructor<Struct>>
                = listOf(StructConstructor(this, members))

            override fun substitute(substitution: Substitution): Struct
                = Struct(members.substituteAll(substitution))

            override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
                is Struct -> Union(this, other)
                else -> Never("Cannot unify Types $id & ${other.id}")
            }
        }

        data class StructConstructor(override val constructedType: Struct, val args: List<Member>) : IConstructor<Struct> {
            override val id: String = "(${args.joinToString(", ") { it.id }}) -> ${constructedType.id}"

            override fun getDomain(): List<AnyType> = args.map { it.type }
            override fun getCodomain(): AnyType = constructedType

            override fun curry(): IArrow<*> = this

            override fun substitute(substitution: Substitution): IConstructor<Struct>
                = StructConstructor(constructedType.substitute(substitution), args.substituteAll(substitution))

            override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = Never("Cannot unify Types $id & ${other.id}")

            override fun never(args: List<AnyType>): Never
                = Never("Cannot construct Type ${constructedType.id} with arguments (${args.joinToString("; ") { it.id }})")
        }

        data class UnionConstructor(override val constructedType: Union, val arg: AnyType) : IConstructor<Union> {
            override val id: String = "(${arg.id}) -> ${constructedType.id}"

            override fun getDomain(): List<AnyType> = listOf(arg)
            override fun getCodomain(): AnyType = constructedType.getElement(arg)

            override fun curry(): IArrow<*> = this

            override fun substitute(substitution: Substitution): IConstructor<Union>
                = UnionConstructor(constructedType.substitute(substitution), arg.substitute(substitution))

            override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = Never("Cannot unify Types $id & ${other.id}")

            override fun never(args: List<AnyType>): Never
                = Never("Union Type ${constructedType.id} cannot be constructed with argument ${arg.id}")
        }

        data class Union(val left: AnyType, val right: AnyType) : ISumType<Union>, IConstructableType<Union> {
            override val id: String = "(${left.id} | ${right.id})"

            override fun getElement(at: AnyType): AnyType = when (at) {
                left -> left
                right -> right
                else -> Never("Sum Type $id will never contain a value of Type ${at.id}")
            }

            override fun getConstructors(): List<IConstructor<Union>>
                = listOf(left, right).map { UnionConstructor(this, it) }

            override fun substitute(substitution: Substitution): Union
                = Union(left.substitute(substitution), right.substitute(substitution))

            override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*> = when (other) {
                is Union -> Union(this, other)
                else -> Never("Cannot unify Types $id & ${other.id}")
            }
        }

        sealed interface IArrow<Self: IArrow<Self>> : UnifiableType<Self> {
            fun getDomain() : List<AnyType>
            fun getCodomain() : AnyType

            fun curry() : IArrow<*>
            fun never(args: List<AnyType>) : IType.Never

            fun maxCurry() : Arrow0 = when (this) {
                is Arrow0 -> this
                is Arrow1 -> curry()
                is Arrow2 -> curry().curry()
                is Arrow3 -> curry().curry().curry()
                else -> Arrow0(this)
            }

            override fun unify(env: Env, other: UnifiableType<*>): UnifiableType<*>
                = Never("Cannot unify Arrow Type $id with ${other.id}")
        }

        data class Arrow0(val gives: IType<*>) : IArrow<Arrow0> {
            override val id: String = "() -> ${gives.id}"

            override fun getDomain(): List<AnyType> = emptyList()
            override fun getCodomain(): AnyType = gives

            override fun substitute(substitution: Substitution): Arrow0
                = Arrow0(gives.substitute(substitution))

            override fun curry(): Arrow0
                = Arrow0(Arrow0(gives))

            override fun never(args: List<AnyType>): Never = Never("Unreachable")
        }

        data class Arrow1(val takes: IType<*>, val gives: IType<*>) : IArrow<Arrow1> {
            override val id: String = "(${takes.id}) -> ${gives.id}"

            override fun getDomain(): List<AnyType> = listOf(takes)
            override fun getCodomain(): AnyType = gives

            override fun substitute(substitution: Substitution): Arrow1
                = Arrow1(takes.substitute(substitution), gives.substitute(substitution))

            override fun curry(): Arrow0
                = Arrow0(Arrow1(takes, gives))

            override fun never(args: List<AnyType>): Never = Never("$id expects argument of Type ${takes.id}, found ${args[0].id}")
        }

        data class Arrow2(val a: IType<*>, val b: IType<*>, val gives: IType<*>) : IArrow<Arrow2> {
            override val id: String = "(${a.id}, ${b.id}) -> ${gives.id}"

            override fun getDomain(): List<AnyType> = listOf(a, b)
            override fun getCodomain(): AnyType = gives

            override fun substitute(substitution: Substitution): Arrow2
                = Arrow2(a.substitute(substitution), b.substitute(substitution), gives.substitute(substitution))

            override fun curry(): Arrow1
                = Arrow1(a, Arrow1(b, gives))

            override fun never(args: List<AnyType>): Never
                = Never("$id expects arguments of (${a.id}, ${b.id}), found (${args.joinToString(", ") { it.id }})")
        }

        data class Arrow3(val a: IType<*>, val b: IType<*>, val c: IType<*>, val gives: IType<*>) : IArrow<Arrow3> {
            override val id: String = "(${a.id}, ${b.id}, ${c.id}) -> ${gives.id}"

            override fun getDomain(): List<AnyType> = listOf(a, b, c)
            override fun getCodomain(): AnyType = gives

            override fun substitute(substitution: Substitution): Arrow3
                = Arrow3(a.substitute(substitution), b.substitute(substitution), c.substitute(substitution), gives.substitute(substitution))

            override fun curry(): Arrow2
                = Arrow2(a, b, Arrow1(c, gives))

            override fun never(args: List<AnyType>): Never
                = Never("$id expects arguments of (${a.id}, ${b.id}, ${c.id}), found (${args.joinToString(", ") { it.id }})")
        }

        data class Signature(val receiver: IType<*>, val name: String, val parameters: List<IType<*>>, val returns: IType<*>) : IType<Signature> {
            override val id: String get() {
                val pParams = parameters.joinToString(", ") { it.id }

                return "${receiver.id}.$name($pParams)(${returns.id})"
            }

            fun toArrow() : AnyArrow {
                val takes = listOf(receiver) + parameters

                return when (takes.count()) {
                    1 -> Arrow1(takes[0], returns)
                    2 -> Arrow2(takes[0], takes[1], returns)
                    3 -> Arrow3(takes[0], takes[1], takes[2], returns)
                    else -> TODO("4+-ary Arrows")
                }
            }

            override fun substitute(substitution: Substitution): Signature
                = Signature(receiver.substitute(substitution), name, parameters.map { it.substitute(substitution) }, returns.substitute(substitution))

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
                    is MembershipTrait -> MembershipTrait("($id & ${other.id})", requiredMembers + other.requiredMembers)
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

            operator fun plus(other: ITrait) : ITrait
        }

        val id: String
    }

    data class Substitution(val old: IType<*>, val new: IType<*>)
    interface Substitutable<Self: Substitutable<Self>> {
        fun substitute(substitution: Substitution) : Self
    }

    object TypeUtils {
        fun check(env: Env, expression: AnyExpr, type: AnyType) : Boolean {
            val inferredType = when (val cached = env.expressionCache[expression.toString()]) {
                null -> infer(env, expression)
                else -> cached
            }

            return inferredType == type
        }

        fun unify(env: Env, typeA: IType.UnifiableType<*>, typeB: IType.UnifiableType<*>) : IType.UnifiableType<*>
            = typeA.unify(env, typeB)

        fun infer(env: Env, expression: Expr<*>) : IType<*>
            = env.expressionCache[expression.toString()]
                ?: expression.infer(env)
    }

    interface ContextFunction {
        sealed interface Result {
            data class Success(val env: Env) : Result
            data class Failure(val reason: IType.Never) : Result
        }

        object Intrinsics {
            data class AssertEq<E: Expr<E>, T: IType.Entity<T>>(val expression: E, val type: T) : ContextFunction {
                override fun invoke(env: Env): Result = when (TypeUtils.check(env, expression, type)) {
                    true -> Result.Success(env)
                    else -> Result.Failure(IType.Never("Expression $expression expected to be of Type ${type.id}, found ${expression.infer(env).id}"))
                }
            }
        }

        sealed interface Predicate : Clause {
            data class Exists(val element: IType<*>) : Predicate {
                override fun weaken(env: Env): Env = when (env.elements.contains(element)) {
                    true -> env
                    else -> env.extend(Decl.DenyElement(element.id))
                }
            }

            data class Conforms(val type: IType.Type, val trait: IType.ITrait) : Clause {
                private fun getContract() : Contract.Implements<*> = when (trait) {
                    is IType.ITrait.MembershipTrait -> Contract.Implements.Membership(type, trait)
                }

                override fun weaken(env: Env): Env {
                    val contract = getContract()

                    return when (contract.verify(env)) {
                        is Contract.ContractResult.Verified -> env.extend(Decl.Projection(type, trait))
                        is Contract.ContractResult.Violated -> IType.Never("Type ${type.id} does not conform to Trait ${trait.id}").panic()
                    }
                }
            }

            data class Used(val name: String, val useCount: Int) : Predicate {
                override fun weaken(env: Env): Env {
                    val ref = env.refs.firstOrNull { it.name == name } ?: return env
                    val uses = ref.getHistory().filterIsInstance<RefEntry.Use>()

                    return when (uses.count()) {
                        useCount -> env
                        else -> env.denyRef(name)
                    }
                }
            }
        }

        fun invoke(env: Env) : Result
    }

    sealed interface Clause {
        data class ContextFunctionCall(val contextFunction: ContextFunction) : Clause {
            override fun weaken(env: Env): Env = when (val result = contextFunction.invoke(env)) {
                is ContextFunction.Result.Success -> result.env
                is ContextFunction.Result.Failure -> env // NOTE - throws a Never
            }
        }

        fun weaken(env: Env) : Env
    }

    class Context(private val initialEnv: Env, val typeVariables: List<IType.TypeVar>, val clauses: List<Clause>) {
        fun initialise() : Env = clauses.fold(initialEnv) { acc, next -> next.weaken(acc) }
    }

    interface Inf<E: Expr<E>> {
        fun infer(env: Env) : IType<*>
    }

    sealed interface Expr<Self: Expr<Self>> : Substitutable<Self>, Inf<Self> {
        data class Var(val name: String) : Expr<Var> {
            override fun substitute(substitution: Substitution): Var
                = Var(name)

            override fun infer(env: Env): IType<*> = env.getRef(name)?.type ?: IType.Never("$name is undefined in the current context")
            override fun toString(): String = name
        }

        data class TypeLiteral(val name: String) : Expr<TypeLiteral> {
            override fun substitute(substitution: Substitution): TypeLiteral = this
            override fun infer(env: Env): IType<*> = env.getElement(name)!!
            override fun toString(): String = "`Type<$name>`"
        }

        data class Block(val body: List<AnyExpr>) : Expr<Block> {
            override fun substitute(substitution: Substitution): Block
                = Block(body.map { it.substitute(substitution) })

            override fun infer(env: Env): IType<*> = when (body.isEmpty()) {
                true -> IType.Unit
                else -> body.last().infer(env)
            }

            override fun toString(): String = """
                `{
                    ${body.joinToString("\n\t") { it.toString() }}
                }`
            """.trimIndent()
        }

        data class Return(val expr: AnyExpr) : Expr<Return> {
            override fun substitute(substitution: Substitution): Return
                = Return(expr.substitute(substitution))

            override fun toString(): String = "`return $expr`"

            override fun infer(env: Env): IType<*>
                = expr.infer(env)
        }

        sealed interface MatchResult {
            data class ReachablePattern(val env: Env) : MatchResult
            data class UnreachablePattern(val reason: IType.Never) : MatchResult
        }

        sealed interface IPattern : Expr<IPattern> {
            fun match(env: Env, target: AnyExpr) : MatchResult
        }

        object ElsePattern : IPattern {
            override fun substitute(substitution: Substitution): IPattern = this
            override fun infer(env: Env): IType<*> = IType.Unit
            override fun match(env: Env, target: AnyExpr): MatchResult = MatchResult.ReachablePattern(env)

            override fun toString(): String = "`else`"
        }

        data class EqPattern(val expr: AnyExpr) : IPattern {
            override fun substitute(substitution: Substitution): IPattern = this
            override fun infer(env: Env): AnyType = expr.infer(env)
            override fun match(env: Env, target: AnyExpr): MatchResult = when (TypeUtils.check(env, expr, target.infer(env))) {
                true -> MatchResult.ReachablePattern(env)
                else -> MatchResult.UnreachablePattern(IType.Never("$expr will never match against $target"))
            }

            override fun toString(): String = "`case ? == $expr`"
        }

//        data class ConstructorPattern(val type: IType.Type, val) {
//        }

        data class Case(val pattern: IPattern, val block: Block) : Expr<Case> {
            override fun substitute(substitution: Substitution): Case
                = Case(pattern.substitute(substitution), block.substitute(substitution))

            override fun infer(env: Env): IType<*> = IType.Arrow1(pattern.infer(env), block.infer(env))
        }

        data class Select(val target: AnyExpr, val cases: List<Case>) : Expr<Select> {
            override fun substitute(substitution: Substitution): Select
                = Select(target.substitute(substitution), cases.map { it.substitute(substitution) })

            override fun infer(env: Env): IType<*> = cases.map { it.block.infer(env) as IType.UnifiableType<*> }
                .reduce { acc, next -> TypeUtils.unify(env, acc, next) }

            fun verify(env: Env) : Select {
                val unreachable = cases.map { it.pattern.match(env, target) }
                    .filterIsInstance<MatchResult.UnreachablePattern>()

                if (unreachable.isEmpty()) return this
                
                unreachable.fold(IType.Never("The following errors were found while verifying Select expression:")) { acc, next ->
                    acc.unify(env, next.reason) as IType.Never
                }.panic()
            }
        }

        data class Invoke(val arrow: AnyArrow, val args: List<AnyExpr>) : Expr<Invoke> {
            constructor(arrow: AnyArrow, arg: AnyExpr) : this(arrow, listOf(arg))

            override fun substitute(substitution: Substitution): Invoke
                = Invoke(arrow.substitute(substitution), args.map { it.substitute(substitution) })

            override fun toString(): String = "${arrow.id}(${args.joinToString(", ") { it.toString() }})"

            override fun infer(env: Env): IType<*> {
                val exit = { arrow.never(args.map { it.infer(env) }) }
                val domain = arrow.getDomain()

                if (args.count() != domain.count()) return exit()

                val checked = args.zip(domain).fold(true) { acc, next ->
                    if (!acc) return exit()

                    acc && TypeUtils.check(env, next.first, next.second)
                }

                if (!checked) return exit()

                return arrow.getCodomain()
            }
        }
    }

    sealed interface Decl {
        data class Clone(val cloneElements: Boolean = true, val cloneRefs: Boolean = true) : Decl {
            override fun exists(env: Env): Boolean = false
            override fun xtend(env: Env): Env = when (cloneElements) {
                true -> when (cloneRefs) {
                    true -> Env(env.elements, env.refs, env.contracts, env.projections, env.expressionCache)
                    else -> Env(env.elements, emptyList(), env.contracts, env.projections, env.expressionCache)
                }

                else -> when (cloneRefs) {
                    true -> Env(emptyList(), env.refs, env.contracts, env.projections, env.expressionCache)
                    else -> Env(emptyList(), emptyList(), env.contracts, env.projections, env.expressionCache)
                }
            }
        }

        data class DenyElement(private val id: String) : Decl {
            override fun exists(env: Env): Boolean = true
            override fun xtend(env: Env): Env = env.denyElement(id)
        }

        data class DenyRef(private val name: String) : Decl {
            override fun exists(env: Env): Boolean = true
            override fun xtend(env: Env): Env = env.denyRef(name)
        }

        data class Type(val type: IType.Type, val members: Map<String, IType.Entity<*>>) : Decl {
            override fun exists(env: Env): Boolean = env.elements.any { it.id == type.id }
            override fun xtend(env: Env): Env {
                val nMembers = members.map { IType.Member(it.key, it.value, type) }

                return Env(env.elements + type + nMembers, env.refs, env.contracts, env.projections, env.expressionCache)
            }
        }

        data class Assignment(val name: String, val expr: Expr<*>) : Decl {
            override fun exists(env: Env): Boolean = env.refs.any { it.name == name }
            override fun xtend(env: Env): Env {
                val type = expr.infer(env) as? IType.Entity<*> ?: TODO()

                return Env(env.elements, env.refs + Ref(name, type), env.contracts, env.projections, env.expressionCache)
            }
        }

        data class Extension(val typeName: String, val members: Map<String, IType.Entity<*>>) : Decl {
            constructor(type: IType.Type, members: List<IType.Member>) : this(type.id, members.map { it.name to it.type }.toMap())

            override fun exists(env: Env): Boolean = env.elements.containsAll(members.values)
            override fun xtend(env: Env): Env {
                val type = env.getElementAs<IType.Type>(typeName) ?: return env
                val nMembers = members.map { IType.Member(it.key, it.value, type) }

                return Env(env.elements + nMembers, env.refs, env.contracts, env.projections, env.expressionCache)
            }
        }

        data class Projection(val source: IType.Type, val target: IType.ITrait) : Decl {
            override fun exists(env: Env): Boolean = true

            override fun xtend(env: Env): Env
                = Env(env.elements, env.refs, env.contracts, env.projections + Types3Tests.Projection(source, target), env.expressionCache)
        }

        data class Cache(val expr: AnyExpr, val type: AnyType) : Decl {
            override fun exists(env: Env): Boolean = true
            override fun xtend(env: Env): Env
                = Env(env.elements, env.refs, env.contracts, env.projections, env.expressionCache + (expr.toString() to type))
        }

        fun exists(env: Env) : Boolean
        fun xtend(env: Env) : Env

        fun extend(env: Env) : Env = Env.capture { xtend(env) }
    }

    sealed interface Contract {
        sealed interface ContractResult {
            data class Verified(val env: Env) : ContractResult
            data class Violated(val reason: IType.Never) : ContractResult
        }

        sealed interface Invariant : Contract {
            object Intrinsics {
                data class MaxUse(private val ref: Ref, private val limit: Int) : Invariant {
                    override fun verify(env: Env): ContractResult = when (val count = ref.getHistoryInstances<RefEntry.Use>().count()) {
                        limit + 1 -> ContractResult.Violated(IType.Never("Contract Violation: ${ref.name} is constrained by a MaxUse invariant in the current context where limit = $limit, uses = $count"))
                        else -> ContractResult.Verified(env)
                    }
                }
            }
        }

        sealed interface Implements<T: IType.ITrait> : Contract {
            data class Membership(val type: IType.Type, val trait: IType.ITrait.MembershipTrait) : Implements<IType.ITrait.MembershipTrait> {
                override fun verify(env: Env): ContractResult {
                    val members = env.getDeclaredMembers(type) + env.getProjectedMembers(type)

                    if (members.count() < trait.requiredMembers.count()) {
                        return ContractResult.Violated(IType.Never("Type ${type.id} does not conform to Trait ${trait.id}"))
                    }

                    for (requiredMember in trait.requiredMembers) {
                        if (!members.contains(requiredMember)) {
                            return ContractResult.Violated(IType.Never("Type ${type.id} does not conform to Trait ${trait.id}. Missing member: ${requiredMember.id}"))
                        }
                    }

                    return ContractResult.Verified(env.extend(Decl.Projection(type, trait)))
                }
            }
        }

        fun verify(env: Env) : ContractResult
    }

    sealed interface RefEntry {
        data class Use(val ref: Ref) : RefEntry
    }

    class Ref(val name: String, val type: IType.Entity<*>) {
        private val history = mutableListOf<RefEntry>()

        val uniqueId: String = "$name:${type.id}"

        fun getHistory() : List<RefEntry> = history
        inline fun <reified E: RefEntry> getHistoryInstances() : List<E>
            = getHistory().filterIsInstance<E>()

        fun consume() : Ref = apply {
            history.add(RefEntry.Use(this))
        }
    }

    data class Projection(val source: IType.Entity<*>, val target: IType.Entity<*>)

    data class Env(
        val elements: List<IType<*>> = emptyList(),
        val refs: List<Ref> = emptyList(),
        val contracts: List<Contract> = emptyList(),
        val projections: List<Projection> = emptyList(),
        val expressionCache: Map<String, AnyType> = emptyMap()) {
        companion object {
            private var current: Env = Env()

            fun capture(fn: () -> Env) : Env {
                current = fn()

                return current
            }

            operator fun getValue(obj: Any, property: KProperty<*>): Env = current
        }

        private sealed interface Protector<T> {
            object RefProtector : Protector<Ref?> {
                override fun protect(block: () -> Ref?): Ref? {
                    val result = block() ?: return null

                    return when (val t = result.type) {
                        is IType.Never -> panic(t)
                        else -> result
                    }
                }
            }

            object TypeProtector : Protector<IType<*>?> {
                override fun protect(block: () -> IType<*>?): IType<*>? {
                    val result = block() ?: return null

                    return when (result) {
                        is IType.Never -> panic(result)
                        is IType.Member -> when (result.type) {
                            is IType.Never -> panic(result.type)
                            else -> result
                        }

                        else -> result
                    }
                }
            }

            fun protect(block: () -> T) : T
            fun <T> panic(never: IType.Never) : T = never.panic()
        }

        internal constructor(type: IType<*>, ref: Ref) : this(listOf(type), listOf(ref))

        private fun <T> protect(protector: Protector<T>, block: () -> T) : T
            = protector.protect(block)

        fun getRef(of: String) : Ref? = protect(Protector.RefProtector) {
            refs.firstOrNull { it.name == of }
                // NOTE - A lookup for a defined Ref `r` bumps its use count
                ?.consume()
        }

        fun getElement(id: String) : IType<*>? = protect(Protector.TypeProtector) {
            elements.firstOrNull { it.id == id }
        }

        inline fun <reified T: IType<T>> getElementAs(id: String) : T? = getElement(id) as? T
        inline fun <reified T: IType<T>> getRefAs(of: String) : T? = getRef(of) as? T

        fun getProjections(of: IType.Entity<*>) : List<Projection>
            = projections.filter { it.source == of }

        fun getProjectedMembers(of: IType.Entity<*>) : List<IType.Member> {
            val projections = getProjections(of)
                .map { it.target }
                .filterIsInstance<IType.ITrait.MembershipTrait>()

            return projections.flatMap { it.requiredMembers }
        }

        fun projects(source: IType.Entity<*>, target: IType.Entity<*>) : Boolean
            = projections.any { it.source == source && it.target == target }

        fun getDeclaredMembers(of: IType.Type) : List<IType.Member>
            = elements.filterIsInstance<IType.Member>()
                .filter { it.owner == of }

        fun getMembers(of: IType.Type) : List<IType.Member>
            = getDeclaredMembers(of) + getProjectedMembers(of)

        fun extend(decl: Decl) : Env = decl.extend(this)

        fun denyElement(id: String) : Env {
            val nElements = elements.map { when (it.id) {
                id -> IType.Never("$id is not defined in the current Context", id)
                else -> it
            }}

            return Env(nElements, refs, contracts, projections, expressionCache)
        }

        fun denyRef(name: String) : Env {
            val nRefs = refs.map { when (it.name) {
                name -> Ref(name, IType.Never("$name is not bound in the current Context", name))
                else -> it
            }}

            return Env(elements, nRefs, contracts, projections, expressionCache)
        }

        fun accept(contract: Contract) : Env = Env(elements, refs, contracts + contract)
        fun verifyContracts() = contracts.forEach { when (val result = it.verify(this)) {
            is Contract.ContractResult.Verified -> {}
            is Contract.ContractResult.Violated -> result.reason.panic()
        }}
    }

    @Test
    fun `Type == Type - Identity`() {
        val t = IType.Type("t")

        assertTrue(t == t)
    }

    @Test
    fun `Type == Type - name == name`() {
        val a = IType.Type("t")
        val b = IType.Type("t")

        assertTrue(a == b)
    }

    @Test
    fun `Type != Type - name != name`() {
        val a = IType.Type("a")
        val b = IType.Type("b")

        assertFalse(a == b)
    }

    @Test
    fun `Member == Member - Identity`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val m = IType.Member( "a", a, b)

        assertTrue(m == m)
    }

    @Test
    fun `Member == Member`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val m = IType.Member( "a", a, b)
        val n = IType.Member( "a", a, b)

        assertTrue(m == n)
    }

    @Test
    fun `Member != Member`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val m = IType.Member( "a", a, b)
        val n = IType.Member( "b", a, b)

        assertFalse(m == n)
    }

    @Test
    fun `Arrow == Arrow - Identity`() {
        val a = IType.Type("A")
        val f = IType.Arrow1(a, a)

        assertTrue(f == f)
    }

    @Test
    fun `Arrow == Arrow`() {
        val a = IType.Type("A")
        val f = IType.Arrow1(a, a)
        val g = IType.Arrow1(a, a)

        assertTrue(f == g)
    }

    @Test
    fun `Arrow != Arrow`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val f = IType.Arrow1(a, b)
        val g = IType.Arrow1(b, a)

        assertFalse(f == g)
    }

    @Test
    fun `Bump ref use count 0`() {
        val a = IType.Type("A")
        val env = Env(listOf(a), listOf(Ref("a", a)))
        val sut = ContextFunction.Predicate.Used("a", 0)
        val res = sut.weaken(env)

        assertTrue(res === env)
    }

    @Test
    fun `Bump ref use count 1`() {
        val a = IType.Type("A")
        val env = Env(listOf(a), listOf(Ref("a", a)))
        val sut = ContextFunction.Predicate.Used("a", 1)

        env.getRef("a")

        val res = sut.weaken(env)

        assertTrue(res === env)
    }

    @Test
    fun `Bump ref use count 2`() {
        val a = IType.Type("A")
        val env = Env(listOf(a), listOf(Ref("a", a)))
        val sut = ContextFunction.Predicate.Used("a", 2)

        env.getRef("a")
        env.getRef("a")

        val res = sut.weaken(env)

        assertTrue(res === env)
    }

    @Test
    fun `Clone Decl extends Env where cloneElements == true && cloneRefs == true`() {
        val t = IType.Type("T")
        val r = Ref("r", t)
        val env = Env(listOf(t), listOf(r))
        val sut = Decl.Clone()
        val res = env.extend(sut)

        // Expectation: a cloned Env is referentially distinct from its progenitor
        assertTrue(res !== env)
        assertEquals(1, res.elements.count())
        assertEquals(1, res.refs.count())
    }

    @Test
    fun `Clone Decl is empty where cloneElements == false && cloneRefs == false`() {
        val t = IType.Type("T")
        val r = Ref("r", t)
        val env = Env(listOf(t), listOf(r))
        val sut = Decl.Clone(cloneElements = false, cloneRefs = false)
        val res = env.extend(sut)

        // Expectation: a cloned Env is referentially distinct from its progenitor
        assertTrue(res !== env)
        assertTrue(res.elements.isEmpty())
        assertTrue(res.refs.isEmpty())
    }

    @Test
    fun `Type Decl extends Env with new element`() {
        val t = IType.Type("T")
        val env = Env()
        val sut = Decl.Type(t, mapOf("t" to t))
        val res = env.extend(sut)

        // Expectation: an extended Env is referentially distinct from its progenitor
        assertTrue(res !== env)
        assertEquals(2, res.elements.count())

        val resT = res.getElementAs<IType.Type>("T")!!
        val resR = res.getElementAs<IType.Member>("T.t")!!

        assertTrue(resT === t)
        assertTrue(resR.name == "t")
        assertTrue(resR.type === t)
    }

    @Test
    fun `Var Decl extends Env with new ref`() {
        val t = IType.Type("T")
        val env = Env(listOf(t))
        val sut = Decl.Assignment("v", Expr.TypeLiteral("T"))
        val res = env.extend(sut)

        // Expectation: an extended Env is referentially distinct from its progenitor
        assertTrue(res !== env)
        assertEquals(1, res.refs.count())
    }

    @Test
    fun `DenyElement Decl prohibits access to named Type`() {
        val t = IType.Type("T")
        val env = Env(listOf(t))
        val sut = Decl.DenyElement("T")
        val res = env.extend(sut)

        // Expectation: an extended Env is referentially distinct from its progenitor
        assertTrue(res !== env)
        assertEquals(1, res.elements.count())
        assertIs<IType.Never>(res.elements[0])

        assertThrows<Exception> { res.getElement("T") }
    }

    @Test
    fun `DenyRef Decl prohibits access to ref`() {
        val t = IType.Type("T")
        val r = Ref("r", t)
        val env = Env(listOf(t), listOf(r))
        val sut = Decl.DenyRef("r")
        val res = env.extend(sut)

        // Expectation: an extended Env is referentially distinct from its progenitor
        assertTrue(res !== env)
        assertEquals(1, res.refs.count())
        assertIs<IType.Never>(res.refs[0].type)
    }

    @Test
    fun `UseCount Invariant panics when violated`() {
        val t = IType.Type("T")
        val r = Ref("r", t)
        val env = Env(t, r)
        val sut = Contract.Invariant.Intrinsics.MaxUse(r, 1)
        val res = env.accept(sut)

        assertTrue(res !== env)
        assertEquals(1, res.refs.count())
        assertTrue(res.refs[0].getHistoryInstances<RefEntry.Use>().isEmpty())

        assertDoesNotThrow { res.verifyContracts() }

        res.getRef("r")

        assertEquals(1, res.refs.count())
        assertEquals(1, res.refs[0].getHistoryInstances<RefEntry.Use>().count())

        assertDoesNotThrow { res.verifyContracts() }

        res.getRef("r")

        assertEquals(1, res.refs.count())
        assertEquals(2, res.refs[0].getHistoryInstances<RefEntry.Use>().count())

        assertThrows<Exception> { res.verifyContracts() }
    }

    @Test
    fun `Env returns extended members for Type`() {
        val t = IType.Type("T")
        val u = IType.Type("U")
        val sut = Env(listOf(t, u))
        val initialConditions = sut.getDeclaredMembers(t)

        assertTrue(initialConditions.isEmpty())

        val m = IType.Member("m", u, t)
        val x = Decl.Extension(t, listOf(m))
        val res = sut.extend(x)

        assertEquals(1, res.getDeclaredMembers(t).count())
        assertEquals(m, res.getDeclaredMembers(t)[0])
    }

    @Test
    fun `Type does not conform to MembershipTrait`() {
        val t = IType.Type("T")
        val u = IType.Type("U")
        val m = IType.Member("m", u, t)
        val mt = IType.ITrait.MembershipTrait("hasU", listOf(m))
        val env = Env(listOf(t, u))
        val sut = Contract.Implements.Membership(t, mt)
        val res = sut.verify(env)

        assertIs<Contract.ContractResult.Violated>(res)
    }

    @Test
    fun `Type conforms to MembershipTrait via declared members`() {
        val t = IType.Type("T")
        val u = IType.Type("U")
        val m = IType.Member("m", u, t)
        val mt = IType.ITrait.MembershipTrait("hasU", listOf(m))
        val env = Env(listOf(t, u, m))
        val sut = Contract.Implements.Membership(t, mt)
        val res = sut.verify(env)

        assertIs<Contract.ContractResult.Verified>(res)
    }

    @Test
    fun `Type conforms to MembershipTrait via Projection`() {
        val t = IType.Type("T")
        val u = IType.Type("U")
        val m = IType.Member("m", u, t)
        val mt = IType.ITrait.MembershipTrait("hasU", listOf(m))
        val p = Projection(t, mt)
        val env = Env(listOf(t, u), projections = listOf(p))
        val sut = Contract.Implements.Membership(t, mt)
        val res = sut.verify(env)

        assertIs<Contract.ContractResult.Verified>(res)
    }

    @Test
    fun `Function call inference fails where arg type is wrong`() {
        val t = IType.Type("T")
        val u = IType.Type("U")
        val a = IType.Arrow1(t, u)
        val env = Env(listOf(t, u, a))
        val sut = Expr.Invoke(a, Expr.TypeLiteral("U"))
        val res = sut.infer(env)

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Infer function call return type`() {
        val t = IType.Type("T")
        val u = IType.Type("U")
        val a = IType.Arrow1(t, u)
        val env = Env(listOf(t, u, a))
        val sut = Expr.Invoke(a, Expr.TypeLiteral("T"))
        val res = sut.infer(env)

        assertEquals(u, res)
    }

    @Test
    fun `Nullary Arrow curries to Nullary Arrow`() {
        val a = IType.Type("A")
        val sut = IType.Arrow0(a)
        val res = sut.curry()

        assertIs<IType.Arrow0>(res)
        assertEquals("() -> A", sut.id)
        assertEquals("() -> () -> A", res.id)
    }

    @Test
    fun `Arrow1 curries to Arrow0`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val sut = IType.Arrow1(a, b)
        val res = sut.curry()

        assertIs<IType.Arrow0>(res)
        assertEquals("(A) -> B", sut.id)
        assertEquals("() -> (A) -> B", res.id)
    }

    @Test
    fun `Arrow2 curries to Arrow1`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val c = IType.Type("C")
        val sut = IType.Arrow2(a, b, c)
        val res = sut.curry()

        assertIs<IType.Arrow1>(res)
        assertEquals("(A, B) -> C", sut.id)
        assertEquals("(A) -> (B) -> C", res.id)
    }

    @Test
    fun `Arrow3 curries to Arrow2`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val c = IType.Type("C")
        val d = IType.Type("D")
        val sut = IType.Arrow3(a, b, c, d)
        val res = sut.curry()

        assertIs<IType.Arrow2>(res)
        assertEquals("(A, B, C) -> D", sut.id)
        assertEquals("(A, B) -> (C) -> D", res.id)
    }

    @Test
    fun `Arrow3 max curries to Arrow0`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val c = IType.Type("C")
        val d = IType.Type("D")
        val sut = IType.Arrow3(a, b, c, d)
        val res = sut.maxCurry()

        assertIs<IType.Arrow0>(res)
        assertEquals("(A, B, C) -> D", sut.id)
        assertEquals("() -> (A) -> (B) -> (C) -> D", res.id)
    }

    @Test
    fun `Unify(Unit, Unit) = Unit`() {
        val env = Env()
        val res = TypeUtils.unify(env, IType.Unit, IType.Unit)

        assertIs<IType.Unit>(res)
    }

    @Test
    fun `Unify(Never, Never) = Never`() {
        val n1 = IType.Never("Never 1")
        val n2 = IType.Never("Never 2")
        val res = TypeUtils.unify(Env(), n1, n2)

        assertIs<IType.Never>(res)
        assertEquals("!:!", (res as IType.Never).id)
        assertEquals("Never 1\nNever 2", res.message)
    }

    @Test
    fun `Unify(Unit, Never) = Never`() {
        val res = TypeUtils.unify(Env(), IType.Unit, IType.Never("Nope"))

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Unify(Never, Unit) = Never`() {
        val res = TypeUtils.unify(Env(), IType.Never("Nope"), IType.Unit)

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Unify(Type, Never) = Never`() {
        val t = IType.Type("T")
        val res = TypeUtils.unify(Env(), t, IType.Never("Nope"))

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Unify(Never, Type) = Never`() {
        val t = IType.Type("T")
        val res = TypeUtils.unify(Env(), IType.Never("Nope"), t)

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Unify(Type, Unit) = Type`() {
        val t = IType.Type("T")
        val res = TypeUtils.unify(Env(), t, IType.Unit)

        assertIs<IType.Type>(res)
    }

    @Test
    fun `Unify(Unit, Type) = Type`() {
        val t = IType.Type("T")
        val res = TypeUtils.unify(Env(), IType.Unit, t)

        assertIs<IType.Type>(res)
    }

    @Test
    fun `Unify(Trait, Unit) = Trait`() {
        val t = IType.ITrait.MembershipTrait("T", emptyList())
        val res = TypeUtils.unify(Env(), t, IType.Unit)

        assertIs<IType.ITrait.MembershipTrait>(res)
    }

    @Test
    fun `Unify(Unit, Trait) = Trait`() {
        val t = IType.ITrait.MembershipTrait("T", emptyList())
        val res = TypeUtils.unify(Env(), IType.Unit, t)

        assertIs<IType.ITrait.MembershipTrait>(res)
    }

    @Test
    fun `Unify(Trait, Never) = Never`() {
        val t = IType.ITrait.MembershipTrait("T", emptyList())
        val res = TypeUtils.unify(Env(), t, IType.Never("Nope"))

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Unify(Never, Trait) = Never`() {
        val t = IType.ITrait.MembershipTrait("T", emptyList())
        val res = TypeUtils.unify(Env(), IType.Never("Nope"), t)

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Unify(Type, Type) = Trait`() {
        val t = IType.Type("T")
        val a = IType.Type("A")
        val b = IType.Type("B")
        val ma = IType.Member("a", t, a)
        val mb = IType.Member("b", t, b)
        val env = Env(listOf(t, a ,b, ma, mb))
        val res = TypeUtils.unify(env, a, b)

        assertIs<IType.ITrait.MembershipTrait>(res)

        val trait = res as IType.ITrait.MembershipTrait

        assertEquals(2, trait.requiredMembers.count())
        assertEquals("(A.__api & B.__api)", trait.id)
    }

    @Test
    fun `Unify(Trait, Trait) = Trait`() {
        val t = IType.Type("T")
        val ma = IType.Member("ma", t, IType.Type.self)
        val mb = IType.Member("mb", t, IType.Type.self)
        val a = IType.ITrait.MembershipTrait("A", listOf(ma))
        val b = IType.ITrait.MembershipTrait("B", listOf(mb))
        val env = Env(listOf(t, a ,b, ma, mb))
        val res = TypeUtils.unify(env, a, b)

        assertIs<IType.ITrait.MembershipTrait>(res)

        val trait = res as IType.ITrait.MembershipTrait

        assertEquals(2, trait.requiredMembers.count())
        assertEquals("(A & B)", trait.id)
    }

    @Test
    fun `Unify(Trait, Type) = Trait`() {
        val t = IType.Type("T")
        val ma = IType.Member("ma", t, IType.Type.self)
        val a = IType.ITrait.MembershipTrait("A", listOf(ma))
        val b = IType.Type("B")
        val mb = IType.Member("mb", t, b)
        val env = Env(listOf(t, a ,b, ma, mb))
        val res = TypeUtils.unify(env, a, b)

        assertIs<IType.ITrait.MembershipTrait>(res)

        val trait = res as IType.ITrait.MembershipTrait

        assertEquals(2, trait.requiredMembers.count())
        assertEquals("(B.__api & A)", trait.id)
    }

    @Test
    fun `Unify(Type, Trait) = Trait`() {
        val t = IType.Type("T")
        val a = IType.Type("A")
        val ma = IType.Member("ma", t, a)
        val mb = IType.Member("mb", t, IType.Type.self)
        val b = IType.ITrait.MembershipTrait("B", listOf(mb))
        val env = Env(listOf(t, a ,b, ma, mb))
        val res = TypeUtils.unify(env, a, b)

        assertIs<IType.ITrait.MembershipTrait>(res)

        val trait = res as IType.ITrait.MembershipTrait

        assertEquals(2, trait.requiredMembers.count())
        assertEquals("(A.__api & B)", trait.id)
    }

    @Test
    fun `Pattern match to unrelated type is unreachable`() {
        val t = IType.Type("T")
        val u = IType.Type("U")
        val x = Ref("x", t)
        val env = Env(listOf(t, u), listOf(x))
        val sut = Expr.EqPattern(Expr.TypeLiteral("U"))
        val res = sut.match(env, Expr.Var("x"))

        assertIs<Expr.MatchResult.UnreachablePattern>(res)
    }

    @Test
    fun `Pattern match to related type is reachable`() {
        val t = IType.Type("T")
        val x = Ref("x", t)
        val env = Env(listOf(t), listOf(x))
        val sut = Expr.EqPattern(Expr.TypeLiteral("T"))
        val res = sut.match(env, Expr.Var("x"))

        assertIs<Expr.MatchResult.ReachablePattern>(res)
    }

    @Test
    fun `Union Constructors fail when invoked with mismatched args`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val u = IType.Union(a, b)
        val env = Env(listOf(a, b, u))
        val cons = u.getConstructors()

        assertEquals(2, cons.count())

        val sut1 = Expr.Invoke(cons[0], Expr.TypeLiteral("B"))
        val res1 = sut1.infer(env)

        assertIs<IType.Never>(res1)

        val sut2 = Expr.Invoke(cons[1], Expr.TypeLiteral("A"))
        val res2 = sut2.infer(env)

        assertIs<IType.Never>(res2)
    }

    @Test
    fun `Union Constructors succeed when invoked with expected args`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val u = IType.Union(a, b)
        val env = Env(listOf(a, b, u))
        val cons = u.getConstructors()

        assertEquals(2, cons.count())

        val sut1 = Expr.Invoke(cons[0], Expr.TypeLiteral("A"))
        val res1 = sut1.infer(env)

        assertTrue(res1 === a)

        val sut2 = Expr.Invoke(cons[1], Expr.TypeLiteral("B"))
        val res2 = sut2.infer(env)

        assertTrue(res2 === b)
    }
}