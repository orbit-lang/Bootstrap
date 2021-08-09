package org.orbit.types.components

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ConformanceTests {
    @Test
    fun `True - Simple nominal equality`() {
        val context = Context()
        val typeA = Type("A")
        val typeA2 = Type("A")

        // Prove identity
        assertTrue(NominalEquality.isSatisfied(context, typeA, typeA))
        // Prove A == B because they have the same name
        assertTrue(NominalEquality.isSatisfied(context, typeA, typeA2))

        // Prove types use Nominal equality
        assertTrue(typeA.equalitySemantics is NominalEquality)
    }

    @Test
    fun `False - Simple nominal equality`() {
        val context = Context()
        val typeA = Type("A")
        val typeB = Type("B")

        assertFalse(NominalEquality.isSatisfied(context, typeA, typeB))
    }

    @Test
    fun `True - Simple structural equality`() {
        val context = Context()
        val typeA = Type("A")
        val typeB = Type("B")

        // Prove identity
        assertTrue(StructuralEquality.isSatisfied(context, typeA, typeB))
        // Prove A == B because they have the same structure (i.e. none)
        assertTrue(StructuralEquality.isSatisfied(context, typeA, typeB))
    }

    @Test
    fun `False - Complex structural equality where properties are mismatched`() {
        val context = Context()
        val foo = Type("Foo")
        val f = Property("f", foo)

        val typeA = Type("A", properties = listOf(f))
        val typeB = Type("B")

        // Prove identity
        assertTrue(StructuralEquality.isSatisfied(context, typeA, typeA))
        // Prove A != B because properties don't match
        assertFalse(StructuralEquality.isSatisfied(context, typeA, typeB))
    }

    @Test
    fun `True - Complex structural equality where properties match`() {
        val context = Context()
        val foo = Type("Foo")
        val f = Property("f", foo)

        val typeA = Type("A", properties = listOf(f))
        val typeB = Type("B", properties = listOf(f))

        // Prove A == B because properties match
        assertTrue(StructuralEquality.isSatisfied(context, typeA, typeB))
    }

    @Test
    fun `True - Simple Type Signature conformance`() {
        val context = Context()
        val typeA = Type("A")

        val sigA = TypeSignature("a", typeA, emptyList(), typeA)
        val sigB = TypeSignature("b", typeA, emptyList(), typeA)

        // Prove TypeSignatures use SignatureEquality
        assertTrue(sigA.equalitySemantics is SignatureEquality)
        // Prove identity
        assertTrue(SignatureEquality.isSatisfied(context, sigA as SignatureProtocol<TypeProtocol>, sigA as SignatureProtocol<TypeProtocol>))
        // Prove A == B because receiver, params & return types all match
        assertTrue((sigA.receiver.equalitySemantics as AnyEquality).isSatisfied(context, sigA.receiver, sigB.receiver))
        assertTrue((sigA.returnType.equalitySemantics as AnyEquality).isSatisfied(context, sigA.returnType, sigB.returnType))
        assertTrue(sigA.parameters.isEmpty() && sigB.parameters.isEmpty())
        assertTrue(SignatureEquality.isSatisfied(context, sigA as SignatureProtocol<TypeProtocol>, sigB as SignatureProtocol<TypeProtocol>))
    }

    @Test
    fun `False - Signature conformance with parameter mismatch`() {
        val context = Context()
        val typeA = Type("A")
        val paramA = Parameter("a", typeA)

        val sigA = TypeSignature("a", typeA, listOf(paramA), typeA)
        val sigB = TypeSignature("b", typeA, emptyList(), typeA)

        // Prove A == B because receiver, params & return types all match
        assertFalse(SignatureEquality.isSatisfied(context, sigA as SignatureProtocol<TypeProtocol>, sigB as SignatureProtocol<TypeProtocol>))
    }

    @Test
    fun `True - Simple trait conformance by structural equality`() {
        val context = Context()
        val trait = Trait("Trait")
        val type = Type("Type")

        // Prove traits resolve use StructuralEquality
        assertTrue(trait.equalitySemantics is StructuralEquality)
        // Prove identity
        assertTrue(StructuralEquality.isSatisfied(context, trait, trait))
        // Prove A == B because they have the same structure (i.e. none)
        assertTrue(StructuralEquality.isSatisfied(context, trait, type))
    }

    @Test
    fun `False - Simple trait conformance by structural equality where properties are mismatched`() {
        val context = Context()
        val foo = Type("Foo")
        val f = Property("foo", foo)
        val trait = Trait("Trait", properties = listOf(f))
        val type = Type("Type")

        // Prove A != B because their properties are mismatched
        assertFalse(StructuralEquality.isSatisfied(context, trait, type))
    }

    @Test
    fun `True - Simple trait conformance by structural equality where properties match`() {
        val context = Context()
        val foo = Type("Foo")
        val f = Property("foo", foo)
        val trait = Trait("Trait", properties = listOf(f))
        val type = Type("Type", properties = listOf(f))

        // Prove A == B because their properties match
        assertTrue(StructuralEquality.isSatisfied(context, trait, type))
    }

    @Test
    fun `False - Complex trait conformance by structural equality where properties match by name only`() {
        /*
            trait A
            type Foo1 : A
            type Foo2

            trait Trait(foo Foo1)
            type Type(foo Foo2)

            Trait != Type
         */
        val context = Context()
        val traitA = Trait("A")
        val foo1 = Type("Foo1", traitConformance = listOf(traitA))
        val foo2 = Type("Foo2")

        val f1 = Property("foo", foo1)
        val f2 = Property("foo", foo2)

        val trait = Trait("Trait", properties = listOf(f1))
        val type = Type("Type", properties = listOf(f2))

        // Prove A != B because their properties are mismatched by structure (although names are the same)
        assertFalse(StructuralEquality.isSatisfied(context, trait, type))
    }

    @Test
    fun `True - Complex trait conformance by structural equality where properties match by name and structure`() {
        /*
            trait A
            type Foo2 : A

            trait Trait(f A)
            type Type(f Foo2)

            Trait == Type
         */
        val context = Context()
        val traitA = Trait("A")
        val foo2 = Type("Foo2", traitConformance = listOf(traitA))

        val f1 = Property("foo", traitA)
        val f2 = Property("foo", foo2)

        val trait = Trait("Trait", properties = listOf(f1))
        val type = Type("Type", properties = listOf(f2))

        // Prove A == B because their properties match by name & structure
        assertTrue(StructuralEquality.isSatisfied(context, trait, type))
    }

    @Test
    fun `True - Any module conforms to an Api with no requirements`() {
        val context = Context()
        val api = Api("TestA")
        val mod = Module("TestM")

        assertTrue(mod.conforms(api, context))
    }

    @Test
    fun `True - Satisfying all type requirements satisfies conformance where required signatures is empty`() {
        val context = Context()
        val requiredTypeA = Type("a", isRequired = true)
        val api = Api("TestA", listOf(requiredTypeA))

        val typeA = Type("TypeA", isRequired = false)
        val typeAlias = TypeAlias("a", typeA)
        val mod = Module("ModuleA", listOf(typeAlias))

        assertTrue(mod.conforms(api, context))
    }

    @Test
    fun `Conformance is false where required types are satisfied but required signatures are not`() {
        /*
            api TestA {
                required type A
                (A) sigA () (A)
            }

            module ModuleA {
                type TypeA
                type A = TypeA

                (TypeA) sigB () (TypeA) {}
            }
         */
        val context = Context()
        val requiredTypeA = Type("A", isRequired = true)
        val typeA = Type("TypeA", isRequired = false)
        val typeAlias = TypeAlias("A", typeA)
        val signatureA = TypeSignature("sigA", requiredTypeA, emptyList(), requiredTypeA)
        val signatureB = TypeSignature("sigB", typeA, emptyList(), typeA)
        val api = Api("TestA", listOf(requiredTypeA), requiredSignatures = listOf(signatureA))
        val mod = Module("ModuleA", listOf(typeAlias), signatures = listOf(signatureB))

        assertFalse(mod.conforms(api, context))
    }

    @Test
    fun `True - Signature conformance is satisfied when type aliases declare trait conformance`() {
        /*
            api TestA {
                trait T
                required type A : T
                (A) sigA () (A)
            }

            module ModuleA {
                type TypeA : T
                type A = TypeA

                (TypeA) sigB () (TypeA) {}
            }
         */
        val context = Context()

        val traitT = Trait("T")
        val requiredTypeA = Type("A", traitConformance = listOf(traitT), isRequired = true)

        val typeA = Type("TypeA", traitConformance = listOf(traitT))
        val typeAliasA = TypeAlias("A", typeA)

        val sigA = TypeSignature("sigA", requiredTypeA, emptyList(), requiredTypeA)
        val sigB = TypeSignature("sigB", typeA, emptyList(), typeA)

        val api = Api("TestA", listOf(requiredTypeA), requiredSignatures = listOf(sigA))
        val mod = Module("ModuleA", listOf(typeAliasA), signatures = listOf(sigB))

        assertTrue(sigA.isReceiverSatisfied(sigB.receiver as Entity, context))

        assertTrue(mod.conforms(api, context))
    }

    @Test
    fun `True - Api monomorphisation maps required types to correct concrete types`() {
        val requiredType = Type("A", isRequired = true)
        val concreteType = Type("ConcreteType")
        val typeAlias = TypeAlias("A", concreteType)
        val virtualApi = Api("A", listOf(requiredType))
        val concreteApi = virtualApi.monomorphise { typeAlias }

        val result = concreteApi.getType(requiredType)

        assertFalse(result.isRequired)
        assertTrue(result.name == "ConcreteType")
    }
}