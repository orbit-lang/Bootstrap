@file:Suppress("UNCHECKED_CAST")

package org.orbit.types

import junit.framework.TestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.orbit.types.next.components.*
import org.orbit.types.next.constraints.EqualityConstraint
import org.orbit.types.next.constraints.EqualityConstraintApplication
import org.orbit.types.next.inference.*
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.Unix
import org.orbit.util.assertIs
import org.orbit.util.next.BindingScope
import org.orbit.util.next.TypeMap

class NextTypesTests : TestCase() {
    private val testModule = module {
        single { Invocation(Unix) }
        single { Printer(Unix.getPrintableFactory()) }
    }

    @BeforeEach
    override fun setUp() {
        startKoin { modules(testModule) }
    }

    @AfterEach
    override fun tearDown() {
        stopKoin()
    }

    @Test
    fun testExtendContextWithType() {
        val ctx = Ctx()

        assertTrue(ctx.getTypes().isEmpty())

        val type = Type("Foo")

        ctx.extend(type)

        assertEquals(1, ctx.getTypes().count())

        ctx.extend(type)

        assertEquals(1, ctx.getTypes().count())
    }

    @Test
    fun testContextMapConformance() {
        val ctx = Ctx()

        assertTrue(ctx.getConformanceMap().isEmpty())

        val type = Type("Foo")
        val trait = Trait("Bar")

        ctx.map(type, trait)

        assertEquals(1, ctx.getConformanceMap().count())

        ctx.map(type, trait)

        assertEquals(1, ctx.getConformanceMap().count())
    }

    @Test
    fun testNominalEq() {
        val ctx = Ctx()
        val a = Type("A")
        val a2 = Type("A")
        val b = Type("B")

        assertTrue(NominalEq.eq(ctx, a, a))
        assertTrue(NominalEq.eq(ctx, a, a2))
        assertTrue(NominalEq.eq(ctx, a2, a))
        assertFalse(NominalEq.eq(ctx, a, b))
        assertFalse(NominalEq.eq(ctx, b, a))
    }

    @Test
    fun testStructuralEq() {
        val ctx = Ctx()
        val t = Type("T")
        val f = Field("f", t)
        val t2 = Type("T2", listOf(f))
        val tr = Trait("Tr", listOf(FieldContract(TypeReference("Tr"), f)))
        val tr2 = Trait("Tr2")

        assertFalse(StructuralEq.eq(ctx, tr, t))
        assertTrue(StructuralEq.eq(ctx, tr, t2))

        ctx.map(t, tr2)

        assertTrue(StructuralEq.eq(ctx, tr2, t))
    }

    @Test
    fun testAnyEq() {
        val ctx = Ctx()
        val t = Type("T")
        val f = Field("f", t)
        val t2 = Type("T2", listOf(f))
        val tr1 = Trait("Tr1", listOf(FieldContract(TypeReference("Tr1"), f)))
        val tr2 = Trait("Tr2", listOf(FieldContract(TypeReference("Tr2"), f)))

        // Trait -> Trait equality is Nominal
        assertFalse(AnyEq.eq(ctx, tr1, tr2))
        assertTrue(AnyEq.eq(ctx, tr1, t2))
        assertFalse(AnyEq.eq(ctx, tr1, t))
    }

    @Test
    fun testFieldIsImplemented() {
        val ctx = Ctx()
        val t1 = Type("T1")
        val field1 = Field("f1", t1)

        val t2 = Type("T2", listOf(field1))
        val sut = FieldContract(TypeReference, field1)

        assertTrue(sut.isImplemented(ctx, t2) is ContractResult.Success)
        assertFalse(sut.isImplemented(ctx, t1) is ContractResult.Success)
    }

    @Test
    fun testPropertyTrait() {
        val ctx = Ctx()
        val t1 = Type("T1")
        val field1 = Field("f1", t1)
        val t2 = Type("T2", listOf(field1))
        val contract1 = FieldContract(TypeReference("Trait1"), field1)
        val sut = Trait("Trait1", listOf(contract1))

        assertTrue(sut.isImplemented(ctx, t2) is ContractResult.Success)
    }

//    @Test
//    fun testTypeSynthesiseInterface() {
//        val ctx = Ctx()
//        val t1 = Type("T1")
//        val field1 = Field("f1", t1)
//        val t2 = Type("T2", listOf(field1))
//        val s = Signature("s", t1, listOf(t1, t2), t1)
//
//        ctx.map(t2, s.derive())
//
//        val syntheticInterface1 = InterfaceSynthesiser.synthesise(ctx, t1)
//        val syntheticInterface2 = InterfaceSynthesiser.synthesise(ctx, t2)
//
//        assertEquals("T1::SyntheticInterface", syntheticInterface1.fullyQualifiedName)
//        assertEquals("T2::SyntheticInterface", syntheticInterface2.fullyQualifiedName)
//
//        assertTrue(syntheticInterface1.isSynthetic)
//        assertTrue(syntheticInterface2.isSynthetic)
//
//        assertTrue(syntheticInterface1.contracts.isEmpty())
//        assertEquals(2, syntheticInterface2.contracts.count())
//
//        val fieldContracts = syntheticInterface2.getTypedContracts<FieldContract>()
//        val signatureContracts = syntheticInterface2.getTypedContracts<SignatureContract>()
//
//        assertEquals(1, fieldContracts.count())
//        assertEquals(1, signatureContracts.count())
//    }

    @Test
    fun testCompareTypes() {
        val ctx = Ctx()
        val t1 = Type("T1")
        val t2 = Type("T2")
        val tr1 = Trait("Tr1")
        val tr2 = Trait("Tr2")

        assertEquals(TypeRelation.Same(t1, t1), t1.compare(ctx, t1))
        assertEquals(TypeRelation.Unrelated(t1, t2), t1.compare(ctx, t2))
        assertEquals(TypeRelation.Unrelated(t2, t1), t2.compare(ctx, t1))

        assertEquals(TypeRelation.Same(tr1, tr1), tr1.compare(ctx, tr1))
        assertEquals(TypeRelation.Unrelated(tr1, tr2), tr1.compare(ctx, tr2))
        assertEquals(TypeRelation.Unrelated(tr2, tr1), tr2.compare(ctx, tr1))

        ctx.map(t1, tr1)

        assertEquals(TypeRelation.Related(tr1, t1), t1.compare(ctx, tr1))
        assertEquals(TypeRelation.Related(tr1, t1), tr1.compare(ctx, t1))

        val f = Field("f", t1)
        val t3 = Type("T3", listOf(f))
        val tr3 = Trait("Tr3", listOf(FieldContract(TypeReference("Tr3"), f)))

        assertEquals(TypeRelation.Related(tr3, t3), t3.compare(ctx, tr3))
        assertEquals(TypeRelation.Related(tr3, t3), tr3.compare(ctx, t3))
    }

    @Test
    fun testMonomorphiseField() {
        val ctx = Ctx()
        val t = Type("T")
        val p = AbstractTypeParameter("P")
        val f = Field("F", p)

        val result = FieldMonomorphiser.monomorphise(ctx, f, t, MonomorphisationContext.Any)
            as MonomorphisationResult.Total<Field, Field>

        assertNotNull(result)
        assertTrue(result.result.type is Type)
        assertEquals("T", result.result.type.fullyQualifiedName)
    }

//    @Test
//    fun testMonomorphiseType() {
//        val ctx = Ctx()
//        val t2 = Type("T2")
//        val t3 = Type("T3")
//        val p1 = Parameter("P1")
//        val p2 = Parameter("P2")
//
//        val f1 = Field("f1", p1)
//        val f2 = Field("f2", p2)
//
//        val t = Type("T", listOf(f1, f2))
//
//        val poly = PolymorphicType(t, listOf(p1, p2))
//        val failure = TypeMonomorphiser.monomorphise(ctx, poly, listOf(0 + t2, 1 + t2, 2 + t3))
//
//        assertTrue(failure is MonomorphisationResult.Failure)
//
//        val partial = TypeMonomorphiser.monomorphise(ctx, poly, listOf(0 + t2))
//
//        assertTrue(partial is MonomorphisationResult.Partial)
//
//        val result = TypeMonomorphiser.monomorphise(ctx, poly, listOf(0 + t2, 1 + t3))
//            as MonomorphisationResult.Total<Type, MonomorphicType<Type>>
//
//        assertNotNull(result)
//        assertEquals("T::T2::T3", result.result.fullyQualifiedName)
//        assertEquals(t2, result.result.specialisedType.fields[0].type)
//        assertEquals(t3, result.result.specialisedType.fields[1].type)
//    }

    @Test
    fun testMonomorphiseSignatureSelf() {
        val ctx = Ctx()
        val t = Type("T")
        val s = Signature("s", Self, listOf(Self, t), Self)
        val result = SignatureSelfMonomorphiser.monomorphise(ctx, s, t, MonomorphisationContext.Any)

        assertTrue(result is MonomorphisationResult.Total<*, *>)

        val total = result as MonomorphisationResult.Total<Signature, Signature>

        val resultReceiver = total.result.receiver
        val resultParams = total.result.parameters
        val resultReturns = total.result.returns

        assertEquals(t, resultReceiver)
        assertEquals(t, resultReturns)
        assertEquals(t, resultParams[0])
        assertEquals(t, resultParams[1])
    }

    @Test
    fun testMonomorphiseSignature() {
        val ctx = Ctx()
        val t = Type("T")
        val p1 = AbstractTypeParameter("P1")
        val p2 = AbstractTypeParameter("P2")
        val s = Signature("s", Self, listOf(Self, t), p2)
        val poly = PolymorphicType(s, listOf(p1, p2), partialFields = emptyList())

        val failure = SignatureMonomorphiser.monomorphise(ctx, poly, emptyList(), MonomorphisationContext.Any)

        assertIs<MonomorphisationResult.Failure<*>>(failure)

        val result = SignatureMonomorphiser.monomorphise(ctx, poly, listOf(t, t), MonomorphisationContext.Any)

        assertIs<MonomorphisationResult.Total<*, *>>(result)

        val total = result as MonomorphisationResult.Total<Signature, MonomorphicType<Signature>>

        val resultReceiver = total.result.specialisedType.receiver
        val resultParams = total.result.specialisedType.parameters
        val resultReturns = total.result.specialisedType.returns

        assertEquals(Self, resultReceiver)
        assertEquals(t, resultReturns)
        assertEquals(Self, resultParams[0])
        assertEquals(t, resultParams[1])
    }

//    @Test
//    fun testSelfIndex() {
//        val ctx = Ctx()
//        val t1 = Type("T1")
//        val t2 = Type("T2")
//        val p1 = Parameter("P1")
//        val poly = PolymorphicType(t1, listOf(p1))
//        val idx = SelfIndex(p1)
//        val mono = TypeMonomorphiser.monomorphise(ctx, poly, listOf(0 + t2)) as MonomorphisationResult.Total<Type, MonomorphicType<Type>>
//        val result = idx.apply(mono.result)
//
//        assertNotNull(result)
//
//        val idx2 = SelfIndex(Parameter("P2"))
//        val result2 = idx2.apply(mono.result)
//
//        assertNull(result2)
//    }
//
//    @Test
//    fun testApplyEqualityConstraint() {
//        val ctx = Ctx()
//        val t1 = Type("T1")
//        val t2 = Type("T2")
//        val p1 = AbstractTypeParameter("P1")
//        val p2 = AbstractTypeParameter("P2")
//        val poly = PolymorphicType(t1, listOf(p1, p2), partialFields = emptyList())
//        val selfIndex = SelfIndex(p1)
//        val sut = EqualityConstraint<Type>(selfIndex, t2)
//        val result = sut.refine(ctx, poly) as EqualityConstraintApplication.Partial
//
//        assertEquals(1, result.result.parameters.count())
//        assertEquals("P2", result.result.parameters[0].fullyQualifiedName)
//
//        val selfIndex2 = SelfIndex(p2)
//        val sut2 = EqualityConstraint<Type>(selfIndex2, t2)
//        val result2 = sut2.refine(ctx, result.result) as EqualityConstraintApplication.Total
//
//        assertIs<MonomorphicType<Type>>(result2.result)
//    }

//    @Test
//    fun testMergeCtx() {
//        val ctx1 = Ctx()
//        val ctx2 = Ctx()
//
//        val t1 = Type("T1")
//        val t2 = Type("T2")
//        val t3 = Type("T3")
//
//        val s1 = Signature("s1", t1, listOf(t1), t1)
//        val s2 = Signature("s2", t2, listOf(t2), t2)
//
//        ctx1.extend(t1)
//        ctx1.extend(t3)
//
//        ctx2.extend(t2)
//        ctx2.extend(t3)
//
//        ctx1.map(t1, s1)
//        ctx1.map(t2, s2)
//        ctx1.map(t3, s1)
//        ctx2.map(t1, s2)
//        ctx2.map(t1, s1)
//        ctx2.map(t2, s2)
//        ctx2.map(t3, s1)
//
//        val result = ctx1.merge(ctx2)
//
//        assertEquals(3, result.getTypes().count())
//        assertEquals(3, result.getSignatureMap().count())
//
//        assertEquals(2, result.getSignatures(t1).count())
//        assertEquals(1, result.getSignatures(t2).count())
//        assertEquals(1, result.getSignatures(t3).count())
//    }

    @Test
    fun testCurrySimpleFunc() {
        val t1 = Type("T1")
        val t2 = Type("T2")
        val f = Func(ListType(listOf(t1, t2)), t1)
        val l = f.curry()
        val result = l.fullyQualifiedName

        assertEquals("(T1) -> (T2) -> T1", result)
    }

    @Test
    fun testCurryComplexFunc() {
        val t1 = Type("T1")
        val t2 = Type("T2")
        val f1 = Func(ListType(listOf(t1, t2)), t1)
        val f2 = Func(ListType(listOf(t1, t2, f1)), f1)
        val l = f2.curry()
        val result = l.fullyQualifiedName

        assertEquals("(T1) -> (T2) -> ((T1) -> (T2) -> T1) -> (T1) -> (T2) -> T1", result)
    }

    @Test
    fun testFuncPartial() {
        val t1 = Type("T1")
        val t2 = Type("T2")
        val f1 = Func(ListType(listOf(t1, t2, t1)), t1)
        val result = f1.partial(listOf(Pair(0, t1)))

        assertEquals("(T2, T1) -> T1", result.fullyQualifiedName)
    }

    @Test
    fun testFamilyRelations() {
        val ctx = Ctx()
        val yes = Type("Yes")
        val no = Type("No")
        val family = TypeFamily("Choice", yes, no)
        val thing = Type("T")

        assertTrue(family.compare(ctx, yes) is TypeRelation.Member<*>)
        assertTrue(family.compare(ctx, no) is TypeRelation.Member<*>)
        assertTrue(family.compare(ctx, family) is TypeRelation.Same)

        assertTrue(family.compare(ctx, thing) is TypeRelation.Unrelated)
    }

    @Test
    fun testSameConstraintSubFail() {
        val v = TypeVariable("v")
        val w = TypeVariable("w")
        val a = Type("a")
        val b = Type("b")
        val ctx = Ctx()
        val sut = SameConstraint(v, b)
        val result = sut.substitute(w, a) as SameConstraint

        assertFalse(AnyEq.eq(ctx, a, result.source))
        assertTrue(result.source is TypeVariable)
    }

    @Test
    fun testSameConstraintSubPass() {
        val v = TypeVariable("v")
        val a = Type("a")
        val b = Type("b")
        val ctx = Ctx()
        val sut = SameConstraint(v, b)
        val result = sut.substitute(v, a) as SameConstraint

        assertTrue(AnyEq.eq(ctx, a, result.source))
    }

    @Test
    fun testSameConstraintSelf() {
        val a = Type("a")
        val ctx = Ctx()
        val sut = SameConstraint(a, a)

        assertTrue(sut.check(ctx))
    }

    @Test
    fun testSameConstraintOther() {
        val a = Type("a")
        val b = Type("b")
        val ctx = Ctx()
        val sut = SameConstraint(a, b)

        assertFalse(sut.check(ctx))
    }

    @Test
    fun testLikeConstraintFail() {
        val t = Trait("t")
        val a = Type("a")
        val inferenceUtil = InferenceUtil(TypeMap(), BindingScope.Root, null)

        inferenceUtil.declare(t)
        inferenceUtil.declare(a)

        val ctx = inferenceUtil.toCtx()
        val sut = LikeConstraint(a, t)

        assertFalse(sut.check(ctx))
    }

    @Test
    fun testLikeConstraintPass() {
        val t = Trait("t")
        val a = Type("a")
        val inferenceUtil = InferenceUtil(TypeMap(), BindingScope.Root, null)

        inferenceUtil.declare(t)
        inferenceUtil.declare(a)
        inferenceUtil.addConformance(a, t)

        val ctx = inferenceUtil.toCtx()
        val sut = LikeConstraint(a, t)

        assertTrue(sut.check(ctx))
    }

//    @Test
//    fun testMemberConstraintFail() {
//        val f = TypeFamily<Type>("f")
//        val t = Type("t")
//        val inferenceUtil = InferenceUtil(TypeMap(), BindingScope.Root, null)
//
//        inferenceUtil.declare(f)
//        inferenceUtil.declare(t)
//
//        val ctx = inferenceUtil.toCtx()
//
//        assertFalse(MemberConstraint.check(ctx, f, t))
//    }
//
//    @Test
//    fun testMemberConstraintPass() {
//        val t = Type("f")
//        val f = TypeFamily<Type>("f", t)
//        val inferenceUtil = InferenceUtil(TypeMap(), BindingScope.Root, null)
//
//        inferenceUtil.declare(f)
//        inferenceUtil.declare(t)
//
//        val ctx = inferenceUtil.toCtx()
//
//        assertTrue(MemberConstraint.check(ctx, f, t))
//    }

    @Test
    fun testContextIncomplete() {
        val v = TypeVariable("v")
        val sut = Context("C", listOf(v), emptyList())
        val inferenceUtil = InferenceUtil(TypeMap(), BindingScope.Root)

        assertThrows<Exception> { sut.check(inferenceUtil, emptyList()) }
    }

    @Test
    fun testContextCompleteEmpty() {
        val v = TypeVariable("v")
        val sut = Context("C", listOf(v), emptyList())
        val t = Type("t")
        val inferenceUtil = InferenceUtil(TypeMap(), BindingScope.Root)

        assertTrue(sut.check(inferenceUtil, listOf(t)))
    }

    @Test
    fun testContextCheckSame() {
        val v = TypeVariable("v")
        val t = Type("t")
        val c = SameConstraint(v, t)
        val sut = Context("C", listOf(v), listOf(c))
        val inferenceUtil = InferenceUtil(TypeMap(), BindingScope.Root)

        assertTrue(sut.check(inferenceUtil, listOf(t)))
    }

    @Test
    fun testEqualityRefinement() {
        val inferenceUtil = InferenceUtil.getRoot()
        val t = Type("t")
        val v = TypeVariable("v")
        val sut = EqualityRefinement(v, t)

        inferenceUtil.declare(v)
        inferenceUtil.declare(t)

        assertTrue(inferenceUtil.find(v.fullyQualifiedName)!! is TypeVariable)

        sut.refine(inferenceUtil)

        assertTrue(inferenceUtil.find(v.fullyQualifiedName)!! is Type)
        assertEquals("t", (inferenceUtil.find(v.fullyQualifiedName)!!).fullyQualifiedName)
    }

    @Test
    fun testEqualityRefinementDouble() {
        val inferenceUtil = InferenceUtil.getRoot()
        val t = Type("t")
        val u = Type("u")
        val v = TypeVariable("v")
        val sut = EqualityRefinement(v, t)
        val sut2 = EqualityRefinement(v, u)

        inferenceUtil.declare(v)
        inferenceUtil.declare(t)
        inferenceUtil.declare(u)

        sut.refine(inferenceUtil)

        assertThrows<Exception> { sut2.refine(inferenceUtil) }
    }

    @Test
    fun testEqualityRefinementBothTypeVariables() {
        val inferenceUtil = InferenceUtil.getRoot()
        val a = TypeVariable("a")
        val b = TypeVariable("b")
        val t = Type("t")
        val sut = EqualityRefinement(a, b)
        val sut2 = EqualityRefinement(b, t)

        inferenceUtil.declare(a)
        inferenceUtil.declare(b)
        inferenceUtil.declare(t)

        sut.refine(inferenceUtil)
        sut2.refine(inferenceUtil)

        val result = inferenceUtil.find("a")
            ?: return fail()

        assertTrue(result is Type)
        assertEquals("t", result.fullyQualifiedName)
    }

    @Test
    fun testEqualityRefinementCyclic() {
        val inferenceUtil = InferenceUtil.getRoot()
        val a = TypeVariable("a")
        val b = TypeVariable("b")
        val t = Type("t")
        val sut = EqualityRefinement(a, b)
        val sut2 = EqualityRefinement(b, a)

        inferenceUtil.declare(a)
        inferenceUtil.declare(b)
        inferenceUtil.declare(t)

        sut.refine(inferenceUtil)

        assertThrows<Exception> { sut2.refine(inferenceUtil) }
    }

    @Test
    fun testConformanceRefinement() {
        val inferenceUtil = InferenceUtil.getRoot()
        val t = Trait("t")
        val v = TypeVariable("v")
        val sut = ConformanceRefinement(v, t)

        inferenceUtil.declare(v)
        inferenceUtil.declare(t)

        assertTrue(inferenceUtil.getConformance(v).isEmpty())

        sut.refine(inferenceUtil)

        val result = inferenceUtil.getConformance(v)

        assertEquals(1, result.count())
    }
}

