package org.orbit.types

import junit.framework.TestCase
import org.junit.jupiter.api.Test
import org.orbit.core.Mangler
import org.orbit.core.Path

class Next {
    class Ctx {
        private val types = mutableListOf<Type>()
        private val traits = mutableListOf<Trait>()
        private val signatureMap = mutableMapOf<Type, List<Signature>>()
        private val conformanceMap = mutableMapOf<IType, List<Trait>>()

        fun getTypes() : List<Type> = types
        fun getTraits() : List<Trait> = traits
        fun getSignatureMap() : Map<Type, List<Signature>> = signatureMap
        fun getConformanceMap() : Map<IType, List<Trait>> = conformanceMap

        fun getSignatures(type: Type) : List<Signature>
            = signatureMap.filter { it.key == type }
                .values.firstOrNull() ?: emptyList()

        fun getConformance(type: IType) : List<Trait>
            = conformanceMap.filter { it.key == type }
                .values.firstOrNull() ?: emptyList()

        private fun extend(type: Type) {
            if (types.none { it.fullyQualifiedName == type.fullyQualifiedName }) {
                types.add(type)
            }
        }

        private fun extend(trait: Trait) {
            if (traits.none { it.fullyQualifiedName == trait.fullyQualifiedName }) {
                traits.add(trait)
            }
        }

        fun extend(type: IType) = when (type) {
            is Type -> extend(type)
            is Trait -> extend(type)
            else -> TODO("???")
        }

        fun map(key: Type, signature: Signature) = when (val sigs = signatureMap[key]) {
            null -> signatureMap[key] = listOf(signature)
            else -> signatureMap[key] = (sigs + signature).distinctBy { it.fullyQualifiedName }
        }

        fun map(key: IType, conformance: Trait) = when (val conf = conformanceMap[key]) {
            null -> conformanceMap[key] = listOf(conformance)
            else -> conformanceMap[key] = (conf + conformance).distinctBy { it.fullyQualifiedName }
        }
    }

    interface ITypeEq<A: IType, B: IType> {
        fun eq(ctx: Ctx, a: A, b: B) : Boolean
    }

    object NominalEq : ITypeEq<IType, IType> {
        override fun eq(ctx: Ctx, a: IType, b: IType): Boolean
            = a.fullyQualifiedName == b.fullyQualifiedName
    }

    object StructuralEq : ITypeEq<Trait, Type> {
        override fun eq(ctx: Ctx, a: Trait, b: Type): Boolean = when (a.contracts.isEmpty()) {
            true -> ctx.getConformance(b).contains(a)
            else -> a.contracts.all { it.isImplemented(ctx, b) }
        }
    }

    object AnyEq : ITypeEq<IType, IType> {
        override fun eq(ctx: Ctx, a: IType, b: IType): Boolean = when (a) {
            is Type -> NominalEq.eq(ctx, a, b)
            is Trait -> when (b) {
                is Type -> StructuralEq.eq(ctx, a, b)
                else -> NominalEq.eq(ctx, a, b)
            }
            else -> false
        }
    }

    sealed interface TypeRelation {
        data class Unrelated(val a: IType, val b: IType) : TypeRelation
        data class Related(val leastSpecific: Trait, val mostSpecific: IType) : TypeRelation
        data class Same(val a: IType, val b: IType) : TypeRelation
    }

    interface TypeExtreme {
        fun calculate(ctx: Ctx, a: IType, b: IType) : IType?
    }

    object TypeMinimum : TypeExtreme {
        override fun calculate(ctx: Ctx, a: IType, b: IType): IType? = when (val rel = a.compare(ctx, b)) {
            is TypeRelation.Same -> a
            is TypeRelation.Related -> rel.leastSpecific
            is TypeRelation.Unrelated -> null
        }
    }

    object TypeMaximum : TypeExtreme {
        override fun calculate(ctx: Ctx, a: IType, b: IType): IType? = when (val rel = a.compare(ctx, b)) {
            is TypeRelation.Same -> a
            is TypeRelation.Related -> rel.mostSpecific
            is TypeRelation.Unrelated -> null
        }
    }

    interface IType {
        val fullyQualifiedName: String
        val isSynthetic: Boolean

        fun compare(ctx: Ctx, other: IType) : TypeRelation
    }

    interface Synthesiser<T: IType, U: IType> {
        val identifier: String

        fun synthesise(ctx: Ctx, input: T) : U
    }

    object InterfaceSynthesiser : Synthesiser<Type, Trait> {
        override val identifier: String = "SyntheticInterface"

        override fun synthesise(ctx: Ctx, input: Type): Trait {
            val fieldContracts = input.fields.map(::FieldContract)
            val signatureContracts = ctx.getSignatures(input)
                .map(::SignatureContract)

            return Trait("${input.fullyQualifiedName}::$identifier", fieldContracts + signatureContracts, true)
        }
    }

    data class Field(override val fullyQualifiedName: String, val type: IType) : IType {
        override val isSynthetic: Boolean = type.isSynthetic

        override fun compare(ctx: Ctx, other: IType): TypeRelation = TypeRelation.Unrelated(this, other)
    }

    interface Contract {
        fun isImplemented(ctx: Ctx, by: IType) : Boolean
    }

    data class FieldContract(val field: Field) : Contract {
        override fun isImplemented(ctx: Ctx, by: IType): Boolean {
            if (by !is Type) return false
            if (by.fields.none { it.fullyQualifiedName == field.fullyQualifiedName }) return false

            return by.fields.any { AnyEq.eq(ctx, field.type, it.type) }
        }
    }

    data class SignatureContract(val signature: Signature) : Contract {
        private fun isMatchingSignature(ctx: Ctx, other: Signature) : Boolean {
            if (signature.relativeName != other.relativeName) return false

            val isReceiverEq = AnyEq.eq(ctx, signature.receiver, other.receiver)
            val areParametersEq = signature.parameters.count() == other.parameters.count()
                && signature.parameters.zip(other.parameters).allEq(ctx)
            val isReturnEq = AnyEq.eq(ctx, signature.returns, other.returns)

            return isReceiverEq && areParametersEq && isReturnEq
        }

        override fun isImplemented(ctx: Ctx, by: IType): Boolean {
            if (by !is Type) return false

            val allSignatures = ctx.getSignatures(by)

            return allSignatures.any { isMatchingSignature(ctx, it) }
        }
    }

    data class Trait(override val fullyQualifiedName: String, val contracts: List<Contract> = emptyList(), override val isSynthetic: Boolean = false) : IType, Contract {
        override fun isImplemented(ctx: Ctx, by: IType): Boolean = when (by) {
            is Type -> StructuralEq.eq(ctx, this, by)
            else -> false
        }

        inline fun <reified C: Contract> getTypedContracts() : List<C> = contracts.filterIsInstance<C>()

        fun merge(ctx: Ctx, other: Trait) : Trait? {
            val nFieldContracts = mutableListOf<FieldContract>()
            for (f1 in getTypedContracts<FieldContract>()) {
                for (f2 in other.getTypedContracts<FieldContract>()) {
                    if (f1.field.fullyQualifiedName == f2.field.fullyQualifiedName) {
                        if (AnyEq.eq(ctx, f1.field, f2.field)) {
                            // Name is the same, types are related
                            // We need the least specific of the two here
                            val min = TypeMinimum.calculate(ctx, f1.field, f2.field)
                                ?: return null

                            if (min === f1.field) {
                                nFieldContracts.add(f1)
                            } else {
                                nFieldContracts.add(f2)
                            }
                        } else {
                            // Conflict! Same name, unrelated types
                            return null
                        }
                    } else {
                        nFieldContracts.add(f1)
                        nFieldContracts.add(f2)
                    }
                }
            }

            return Trait(fullyQualifiedName + "_" + other.fullyQualifiedName, nFieldContracts, true)
        }

        override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
            is Trait -> when (NominalEq.eq(ctx, this, other)) {
                true -> TypeRelation.Same(this, other)
                else -> TypeRelation.Unrelated(this, other)
            }

            is Type -> when (StructuralEq.eq(ctx, this, other)) {
                true -> TypeRelation.Related(this, other)
                else -> TypeRelation.Unrelated(this, other)
            }

            else -> TypeRelation.Unrelated(this, other)
        }
    }

    data class Type(override val fullyQualifiedName: String, val fields: List<Field> = emptyList(), override val isSynthetic: Boolean = false) : IType {
        override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
            is Trait -> other.compare(ctx, this)

            is Type -> when (NominalEq.eq(ctx, this, other)) {
                true -> TypeRelation.Same(this, other)
                else -> TypeRelation.Unrelated(this, other)
            }

            else -> TypeRelation.Unrelated(this, other)
        }
    }

    data class Signature(override val fullyQualifiedName: String, val relativeName: String, val receiver: IType, val parameters: List<IType>, val returns: IType, override val isSynthetic: Boolean = false) : IType {
        override fun compare(ctx: Ctx, other: IType): TypeRelation = when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }
    }
}

fun Next.IType.getPath(mangler: Mangler) : Path
    = mangler.unmangle(fullyQualifiedName)

fun List<Next.IType>.anyEq(ctx: Next.Ctx, target: Next.IType) : Boolean
    = any { Next.AnyEq.eq(ctx, target, it) }

fun List<Next.IType>.filterEq(ctx: Next.Ctx, target: Next.IType) : List<Next.IType>
    = filter { Next.AnyEq.eq(ctx, target, it) }

fun List<Pair<Next.IType, Next.IType>>.allEq(ctx: Next.Ctx) : Boolean
    = all { Next.AnyEq.eq(ctx, it.first, it.second) }

class NextTypesTests : TestCase() {
    @Test
    fun testExtendContextWithType() {
        val ctx = Next.Ctx()

        assertTrue(ctx.getTypes().isEmpty())

        val type = Next.Type("Foo")

        ctx.extend(type)

        assertEquals(1, ctx.getTypes().count())

        ctx.extend(type)

        assertEquals(1, ctx.getTypes().count())
    }

    @Test
    fun testExtendContextWithTrait() {
        val ctx = Next.Ctx()

        assertTrue(ctx.getTraits().isEmpty())

        val trait = Next.Trait("Bar")

        ctx.extend(trait)

        assertEquals(1, ctx.getTraits().count())

        ctx.extend(trait)

        assertEquals(1, ctx.getTraits().count())
    }

    @Test
    fun testContextMapSignature() {
        val ctx = Next.Ctx()

        assertTrue(ctx.getSignatureMap().isEmpty())

        val type = Next.Type("Foo")
        val sig = Next.Signature("Sig", "sig", type, listOf(type), type)

        ctx.map(type, sig)

        assertEquals(1, ctx.getSignatureMap().count())
        assertEquals(1, ctx.getSignatures(type).count())

        ctx.map(type, sig)

        assertEquals(1, ctx.getSignatureMap().count())
        assertEquals(1, ctx.getSignatures(type).count())

        val sig2 = Next.Signature("Sig2", "sig2", type, listOf(type), type)

        ctx.map(type, sig2)

        assertEquals(1, ctx.getSignatureMap().count())
        assertEquals(2, ctx.getSignatures(type).count())
    }

    @Test
    fun testContextMapConformance() {
        val ctx = Next.Ctx()

        assertTrue(ctx.getConformanceMap().isEmpty())

        val type = Next.Type("Foo")
        val trait = Next.Trait("Bar")

        ctx.map(type, trait)

        assertEquals(1, ctx.getConformanceMap().count())

        ctx.map(type, trait)

        assertEquals(1, ctx.getConformanceMap().count())
    }

    @Test
    fun testGetSignatures() {
        val ctx = Next.Ctx()
        val foo = Next.Type("Foo")
        val sig = Next.Signature("Sig", "sig", foo, listOf(foo), foo)

        ctx.map(foo, sig)

        val result = ctx.getSignatures(foo)

        assertEquals(1, result.count())
    }

    @Test
    fun testNominalEq() {
        val ctx = Next.Ctx()
        val a = Next.Type("A")
        val a2 = Next.Type("A")
        val b = Next.Type("B")

        assertTrue(Next.NominalEq.eq(ctx, a, a))
        assertTrue(Next.NominalEq.eq(ctx, a, a2))
        assertTrue(Next.NominalEq.eq(ctx, a2, a))
        assertFalse(Next.NominalEq.eq(ctx, a, b))
        assertFalse(Next.NominalEq.eq(ctx, b, a))
    }

    @Test
    fun testStructuralEq() {
        val ctx = Next.Ctx()
        val t = Next.Type("T")
        val f = Next.Field("f", t)
        val t2 = Next.Type("T2", listOf(f))
        val tr = Next.Trait("Tr", listOf(Next.FieldContract(f)))
        val tr2 = Next.Trait("Tr2")

        assertFalse(Next.StructuralEq.eq(ctx, tr, t))
        assertTrue(Next.StructuralEq.eq(ctx, tr, t2))

        ctx.map(t, tr2)

        assertTrue(Next.StructuralEq.eq(ctx, tr2, t))
    }

    @Test
    fun testAnyEq() {
        val ctx = Next.Ctx()
        val t = Next.Type("T")
        val f = Next.Field("f", t)
        val t2 = Next.Type("T2", listOf(f))
        val tr1 = Next.Trait("Tr1", listOf(Next.FieldContract(f)))
        val tr2 = Next.Trait("Tr2", listOf(Next.FieldContract(f)))

        // Trait -> Trait equality is Nominal
        assertFalse(Next.AnyEq.eq(ctx, tr1, tr2))
        assertTrue(Next.AnyEq.eq(ctx, tr1, t2))
        assertFalse(Next.AnyEq.eq(ctx, tr1, t))
    }

    @Test
    fun testFieldIsImplemented() {
        val ctx = Next.Ctx()
        val t1 = Next.Type("T1")
        val field1 = Next.Field("f1", t1)

        val t2 = Next.Type("T2", listOf(field1))
        val sut = Next.FieldContract(field1)

        assertTrue(sut.isImplemented(ctx, t2))
        assertFalse(sut.isImplemented(ctx, t1))
    }

    @Test
    fun testSignatureIsImplemented() {
        val ctx = Next.Ctx()
        val t = Next.Type("T")
        val s = Next.Signature("S", "s", t, listOf(t), t, false)

        ctx.map(t, s)

        val sut = Next.SignatureContract(s)

        assertTrue(sut.isImplemented(ctx, t))
    }

    @Test
    fun testPropertyTrait() {
        val ctx = Next.Ctx()
        val t1 = Next.Type("T1")
        val field1 = Next.Field("f1", t1)
        val t2 = Next.Type("T2", listOf(field1))
        val contract1 = Next.FieldContract(field1)
        val sut = Next.Trait("Trait1", listOf(contract1))

        assertTrue(sut.isImplemented(ctx, t2))
    }

    @Test
    fun testTypeSynthesiseInterface() {
        val ctx = Next.Ctx()
        val t1 = Next.Type("T1")
        val field1 = Next.Field("f1", t1)
        val t2 = Next.Type("T2", listOf(field1))
        val s = Next.Signature("S", "s", t1, listOf(t1, t2), t1)

        ctx.map(t2, s)

        val syntheticInterface1 = Next.InterfaceSynthesiser.synthesise(ctx, t1)
        val syntheticInterface2 = Next.InterfaceSynthesiser.synthesise(ctx, t2)

        assertEquals("T1::SyntheticInterface", syntheticInterface1.fullyQualifiedName)
        assertEquals("T2::SyntheticInterface", syntheticInterface2.fullyQualifiedName)

        assertTrue(syntheticInterface1.isSynthetic)
        assertTrue(syntheticInterface2.isSynthetic)

        assertTrue(syntheticInterface1.contracts.isEmpty())
        assertEquals(2, syntheticInterface2.contracts.count())

        val fieldContracts = syntheticInterface2.getTypedContracts<Next.FieldContract>()
        val signatureContracts = syntheticInterface2.getTypedContracts<Next.SignatureContract>()

        assertEquals(1, fieldContracts.count())
        assertEquals(1, signatureContracts.count())
    }

    @Test
    fun testCompareTypes() {
        val ctx = Next.Ctx()
        val t1 = Next.Type("T1")
        val t2 = Next.Type("T2")
        val tr1 = Next.Trait("Tr1")
        val tr2 = Next.Trait("Tr2")

        assertEquals(Next.TypeRelation.Same(t1, t1), t1.compare(ctx, t1))
        assertEquals(Next.TypeRelation.Unrelated(t1, t2), t1.compare(ctx, t2))
        assertEquals(Next.TypeRelation.Unrelated(t2, t1), t2.compare(ctx, t1))

        assertEquals(Next.TypeRelation.Same(tr1, tr1), tr1.compare(ctx, tr1))
        assertEquals(Next.TypeRelation.Unrelated(tr1, tr2), tr1.compare(ctx, tr2))
        assertEquals(Next.TypeRelation.Unrelated(tr2, tr1), tr2.compare(ctx, tr1))

        ctx.map(t1, tr1)

        assertEquals(Next.TypeRelation.Related(tr1, t1), t1.compare(ctx, tr1))
        assertEquals(Next.TypeRelation.Related(tr1, t1), tr1.compare(ctx, t1))

        val f = Next.Field("f", t1)
        val t3 = Next.Type("T3", listOf(f))
        val tr3 = Next.Trait("Tr3", listOf(Next.FieldContract(f)))

        assertEquals(Next.TypeRelation.Related(tr3, t3), t3.compare(ctx, tr3))
        assertEquals(Next.TypeRelation.Related(tr3, t3), tr3.compare(ctx, t3))
    }
}