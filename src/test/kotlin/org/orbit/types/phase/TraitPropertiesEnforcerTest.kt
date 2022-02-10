//package org.orbit.types.phase
//
//import junit.framework.TestCase
//import org.junit.jupiter.api.Test
//import org.orbit.types.components.*
//
//internal class TraitPropertiesEnforcerTest : TestCase() {
//    @Test
//    fun testEnforceSuccess() {
//        val typeX = Type("X")
//        val trait = Trait("T", properties = listOf(Property("x", typeX)))
//        val typeFoo = Type("Foo", properties = listOf(Property("x", typeX)))
//        val sut = TraitConstraintEnforcer(trait, typeFoo, trait::properties, trait::buildPropertyConstraints)
//
//        val universe = object : ContextProtocol {
//            override val monomorphisedTypes: Map<String, Type> = emptyMap()
//            override val universe: List<TypeProtocol> = listOf(typeX, trait, typeFoo)
//        }
//
//        val result = sut.enforce(universe)
//
//        assertTrue(result is TraitEnforcementResult.SuccessGroup)
//    }
//
//    @Test
//    fun testEnforceFailure() {
//        val typeX = Type("X")
//        val trait = Trait("T", properties = listOf(Property("x", typeX)))
//        val typeFoo = Type("Foo")
//        val sut = TraitConstraintEnforcer<Property>(trait, typeFoo, trait::properties, trait::buildPropertyConstraints)
//
//        val universe = object : ContextProtocol {
//            override val monomorphisedTypes: Map<String, Type> = emptyMap()
//            override val universe: List<TypeProtocol> = listOf(typeX, trait, typeFoo)
//        }
//
//        val result = sut.enforce(universe)
//
//        assertTrue(result is TraitEnforcementResult.FailureGroup)
//    }
//}