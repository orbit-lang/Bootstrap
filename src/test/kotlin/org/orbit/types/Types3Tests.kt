package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.util.assertIs
import kotlin.reflect.KProperty

class Types3Tests {
    sealed interface IType<T: IType<T>> : Substitutable<T> {
        interface Entity<E: Entity<E>> : IType<E>

        data class Never(val message: String, override val id: String = "!") : Entity<Never> {
            fun panic() : Nothing = throw RuntimeException(message)

            override fun substitute(substitution: Substitution): Never = this
            override fun equals(other: Any?): Boolean = TODO("")
        }

        data class Type(override val id: String) : Entity<Type> {
            override fun substitute(substitution: Substitution): Type = when (substitution.old) {
                this -> when (substitution.new) {
                    is Type -> substitution.new
                    else -> this
                }
                else -> this
            }

            override fun equals(other: Any?): Boolean = when (other) {
                is Type -> id == other.id
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

        data class Arrow(val takes: Entity<*>, val gives: Entity<*>) : IType<Arrow> {
            override val id: String = "${takes.id} -> ${gives.id}"

            override fun substitute(substitution: Substitution): Arrow {
                val nTakes = takes.substitute(substitution)
                val nGives = gives.substitute(substitution)

                return Arrow(nTakes, nGives)
            }

            override fun equals(other: Any?): Boolean = when (other) {
                is Arrow -> id == other.id
                else -> false
            }
        }

        data class TypeVar(val name: String) : Entity<TypeVar> {
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
        }

        val id: String
    }

    data class Substitution(val old: IType<*>, val new: IType<*>)
    interface Substitutable<Self: Substitutable<Self>> {
        fun substitute(substitution: Substitution) : Self
    }

    object TypeUtils {
        fun <E: Expr<E>, T: IType.Entity<T>> check(expression: E, type: T) : Boolean {
            val inferredType = expression.infer()

            return inferredType == type
        }

        fun <E: Expr<E>> infer(expression: E) : IType.Entity<*> = expression.infer()
    }

    interface ContextFunction {
        sealed interface Result {
            data class Success(val env: Env) : Result
            data class Failure(val reason: IType.Never) : Result
        }

        object Intrinsics {
            data class AssertEq<E: Expr<E>, T: IType.Entity<T>>(val expression: E, val type: T) : ContextFunction {
                override fun invoke(env: Env): Result = when (TypeUtils.check(expression, type)) {
                    true -> Result.Success(env)
                    else -> Result.Failure(IType.Never("Expression $expression expected to be of Type ${type.id}, found ${expression.infer().id}"))
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
                    else -> TODO()
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
        fun infer() : IType.Entity<*>
    }

    sealed interface Expr<Self: Expr<Self>> : Substitutable<Self>, Inf<Self> {
        data class Var(val name: String, val type: IType.Entity<*>) : Expr<Var> {
            override fun substitute(substitution: Substitution): Var
                = Var(name, type.substitute(substitution))

            override fun infer(): IType.Entity<*> = type

            override fun toString(): String {
                return "$name : ${type.id}"
            }
        }

        data class Invoke(val arrow: IType.Arrow, val arg: IType.Entity<*>) : Expr<Invoke> {
            override fun substitute(substitution: Substitution): Invoke
                = Invoke(arrow.substitute(substitution), arg.substitute(substitution))

            override fun infer(): IType.Entity<*> = arrow.gives
        }
    }

    sealed interface Decl {
        data class Clone(val cloneElements: Boolean = true, val cloneRefs: Boolean = true) : Decl {
            override fun exists(env: Env): Boolean = false
            override fun xtend(env: Env): Env = when (cloneElements) {
                true -> when (cloneRefs) {
                    true -> Env(env.elements, env.refs)
                    else -> Env(env.elements, emptyList())
                }

                else -> when (cloneRefs) {
                    true -> Env(emptyList(), env.refs)
                    else -> Env(emptyList(), emptyList())
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

                return Env(env.elements + type + nMembers, env.refs)
            }
        }

        data class Var(val name: String, val type: IType.Entity<*>) : Decl {
            override fun exists(env: Env): Boolean = env.refs.any { it.name == name }
            override fun xtend(env: Env): Env = Env(env.elements, env.refs + Ref(name, type))
        }

        data class Extension(val typeName: String, val members: Map<String, IType.Entity<*>>) : Decl {
            constructor(type: IType.Type, members: List<IType.Member>) : this(type.id, members.map { it.name to it.type }.toMap())

            override fun exists(env: Env): Boolean = env.elements.containsAll(members.values)
            override fun xtend(env: Env): Env {
                val type = env.getElementAs<IType.Type>(typeName) ?: return env
                val nMembers = members.map { IType.Member(it.key, it.value, type) }

                return Env(env.elements + nMembers, env.refs)
            }
        }

        data class Projection(val source: IType.Type, val target: IType.ITrait) : Decl {
            override fun exists(env: Env): Boolean = true

            override fun xtend(env: Env): Env
                = Env(env.elements, env.refs, env.contracts, env.projections + Types3Tests.Projection(source, target))
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

    data class Env(val elements: List<IType<*>> = emptyList(), val refs: List<Ref> = emptyList(), val contracts: List<Contract> = emptyList(), val projections: List<Projection> = emptyList()) {
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

        fun extend(decl: Decl) : Env = decl.extend(this)

        fun denyElement(id: String) : Env {
            val nElements = elements.map { when (it.id) {
                id -> IType.Never("$id is not defined in the current Context", id)
                else -> it
            }}

            return Env(nElements, refs)
        }

        fun denyRef(name: String) : Env {
            val nRefs = refs.map { when (it.name) {
                name -> Ref(name, IType.Never("$name is not bound in the current Context", name))
                else -> it
            }}

            return Env(elements, nRefs)
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
        val f = IType.Arrow(a, a)

        assertTrue(f == f)
    }

    @Test
    fun `Arrow == Arrow`() {
        val a = IType.Type("A")
        val f = IType.Arrow(a, a)
        val g = IType.Arrow(a, a)

        assertTrue(f == g)
    }

    @Test
    fun `Arrow != Arrow`() {
        val a = IType.Type("A")
        val b = IType.Type("B")
        val f = IType.Arrow(a, b)
        val g = IType.Arrow(b, a)

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
        val env = Env()
        val sut = Decl.Var("v", t)
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
}