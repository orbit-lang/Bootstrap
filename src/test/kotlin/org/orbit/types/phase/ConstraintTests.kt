//package org.orbit.types.phase
//
//import junit.framework.TestCase
//import org.junit.jupiter.api.Test
//import org.orbit.types.components.*
//import org.orbit.types.util.*
//
//internal class ConstraintTests : TestCase() {
//    @Test
//    fun testEmptyPropertyConstraint() {
//        val x = Type("x")
//        val y = Type("y")
//
//        val p1 = Property("a", x)
//        val p2 = Property("b", y)
//
//        val sut = Type("sut")
//        val constraint1 = PropertyConstraint(p1)
//        val constraint2 = PropertyConstraint(p2)
//
//        val universe = object : ContextProtocol {
//            override val monomorphisedTypes: Map<String, Type> = emptyMap()
//            override val universe: List<TypeProtocol> = listOf(x, sut)
//        }
//
//        assertFalse(constraint1.checkConformance(universe, sut))
//        assertFalse(constraint2.checkConformance(universe, sut))
//    }
//
//    @Test
//    fun testPropertyConstraint() {
//        val x = Type("x")
//        val y = Type("y")
//
//        val p1 = Property("a", x)
//        val p2 = Property("b", y)
//
//        val sut = Type("sut", properties = listOf(p1))
//        val constraint1 = PropertyConstraint(p1)
//        val constraint2 = PropertyConstraint(p2)
//
//        val universe = object : ContextProtocol {
//            override val monomorphisedTypes: Map<String, Type> = emptyMap()
//            override val universe: List<TypeProtocol> = listOf(x, sut)
//        }
//
//        assertTrue(constraint1.checkConformance(universe, sut))
//        assertFalse(constraint2.checkConformance(universe, sut))
//    }
//
//    @Test
//    fun testTraitPropertyConstraint() {
//        val x = Type("x")
//        val y = Type("y")
//
//        val p1 = Property("a", x)
//
//        val trait1 = Trait("trait1", properties = listOf(p1))
//
//        val p2 = Property("b", trait1)
//
//        val sut = Type("sut", properties = listOf(p1))
//        val sut2 = Type("sut2", properties = listOf(Property("b", sut)))
//        val constraint1 = PropertyConstraint(p1)
//        val constraint2 = PropertyConstraint(p2)
//
//        val universe = object : ContextProtocol {
//            override val monomorphisedTypes: Map<String, Type> = emptyMap()
//            override val universe: List<TypeProtocol> = listOf(x, sut, sut2)
//        }
//
//        assertTrue(constraint1.checkConformance(universe, sut))
//        assertFalse(constraint2.checkConformance(universe, sut))
//        assertTrue(constraint2.checkConformance(universe, sut2))
//    }
//
//    @Test
//    fun testSignatureConstraint() {
//        val x = Type("x")
//        val y = Type("y")
//
//        val p1 = Parameter("a", x)
//        val p2 = Parameter("b", y)
//
//        val sut = Type("sut")
//
//        val universe = object : ContextProtocol {
//            override val monomorphisedTypes: Map<String, Type> = emptyMap()
//            override val universe: List<TypeProtocol> = listOf(x, sut, TypeSignature("id", sut, listOf(p1), x))
//        }
//
//        val sig1 = TypeSignature("id", sut, listOf(p1), x)
//        val sig2 = TypeSignature("id2", y, listOf(p2), y)
//        val constraint1 = SignatureConstraint(sig1)
//        val constraint2 = SignatureConstraint(sig2)
//
//        assertTrue(constraint1.checkConformance(universe, sut))
//        assertFalse(constraint2.checkConformance(universe, sut))
//    }
//}