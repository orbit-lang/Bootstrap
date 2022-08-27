package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.precess.backend.components.*
import org.orbit.precess.backend.utils.*
import org.orbit.util.assertIs

class Types3Tests {
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
        val sut = Decl.Assignment("v", IType.Type("T"))
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
        val sut = Expr.Invoke(a, Expr.AnyTypeLiteral(u))
        val res = sut.infer(env)

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Infer function call return type`() {
        val t = IType.Type("T")
        val u = IType.Type("U")
        val a = IType.Arrow1(t, u)
        val env = Env(listOf(t, u, a))
        val sut = Expr.Invoke(a, Expr.AnyTypeLiteral(t))
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
        val sut = Expr.EqPattern(Expr.AnyTypeLiteral(u))
        val res = sut.match(env, Expr.Var("x"))

        assertIs<Expr.MatchResult.UnreachablePattern>(res)
    }

    @Test
    fun `Pattern match to related type is reachable`() {
        val t = IType.Type("T")
        val x = Ref("x", t)
        val env = Env(listOf(t), listOf(x))
        val sut = Expr.EqPattern(Expr.AnyTypeLiteral(t))
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

        val sut1 = Expr.Invoke(cons[0], Expr.AnyTypeLiteral(b))
        val res1 = sut1.infer(env)

        assertIs<IType.Never>(res1)

        val sut2 = Expr.Invoke(cons[1], Expr.AnyTypeLiteral(a))
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

        val sut1 = Expr.Invoke(cons[0], Expr.AnyTypeLiteral(a))
        val res1 = sut1.infer(env)

        assertTrue(res1 === a)

        val sut2 = Expr.Invoke(cons[1], Expr.AnyTypeLiteral(b))
        val res2 = sut2.infer(env)

        assertTrue(res2 === b)
    }
}