package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.orbit.types.components.*
import org.orbit.types.components.StructuralEquality as StructuralEquality

internal class ClauseTests {
    @Test
    fun testEntityNominalIdentity() {
        // Proposition: A == A by name where A is an entity
        val entity = Type("A")
        val context = Context(entity)
        val sut = NominalEquality

        assertTrue(sut.isSatisfied(context, entity, entity))
    }

    @Test
    fun testEntityStructuralIdentity() {
        // Proposition: A == A by contract where A is an entity
        val entity = Type("A")
        val context = Context(entity)
        val sut = StructuralEquality

        assertTrue(sut.isSatisfied(context, entity, entity))
    }

    @Test
    fun testEntityNominalEqualityTrue() {
        // Proposition: A == A where A & A are distinct entities (referentially) with identical contracts.
        val entity1 = Type("A")
        val entity2 = Type("A")
        val context = Context(entity1, entity2)
        val sut = NominalEquality

        assertTrue(sut.isSatisfied(context, entity1, entity2))
    }

    @Test
    fun testEntityNominalEqualityFalse() {
        // Proposition: A != B by name where A & B are distinct entities with identical contracts.
        val entity1 = Type("A")
        val entity2 = Type("B")
        val context = Context(entity1, entity2)
        val sut = NominalEquality

        assertFalse(sut.isSatisfied(context, entity1, entity2))
    }

    @Test
    fun testEntityStructuralEqualityEmpty() {
        // Proposition: A == B contractually where A & B are distinct entities with identical contracts.
        val entity1 = Type("A")
        val entity2 = Type("B")
        val context = Context(entity1, entity2)
        val sut = StructuralEquality

        assertTrue(sut.isSatisfied(context, entity1, entity2))
    }

    @Test
    fun testEntityStructuralEqualitySingleMemberTrue() {
        // Proposition: A{x: X} == B{x: X}
        // where A & B & X are distinct entities AND A & B have identical contracts.
        val member = Property("x", Type("X"))
        val entity1 = Type("A", listOf(member))
        val entity2 = Type("B", listOf(member))
        val context = Context(entity1, entity2)
        val sut = StructuralEquality

        assertTrue(sut.isSatisfied(context, entity1, entity2))
    }

    @Test
    fun testEntityStructuralEqualitySingleMemberFalse() {
        // Proposition: A(x: X) != B(y: Y) contractually.
        val member1 = Property("x", Type("X"))
        val member2 = Property("y", Type("X"))
        val entity1 = Type("A", listOf(member1))
        val entity2 = Type("B", listOf(member2))
        val context = Context(entity1, entity2)
        val sut = StructuralEquality

        assertFalse(sut.isSatisfied(context, entity1, entity2))
    }

    @Test
    fun testEntityStructuralEqualityMultipleMembers() {
        // Proposition: A(x: T) != B(y: T) contractually.
        val member1 = Type("T") + "x"
        val member2 = Type("T") + "y"
        val entity1 = Type("A", listOf(member1, member2))
        val entity2 = Type("B", listOf(member2, member1))
        val context = Context(entity1, entity2)
        val sut = StructuralEquality

        assertTrue(sut.isSatisfied(context, entity1, entity2))
    }

//    @Test
//    fun testLambdaStructuralEqualityEntityToEntityTrue() {
//        // Proposition: (x -> x) == (y -> y) where x & y are Entities AND x == y contractually
//        val x = Type("x")
//        val y = Type("y")
//        val lambda1 = Lambda(x, x)
//        val lambda2 = Lambda(y, y)
//        val context = Context(lambda1, lambda2)
//        val sut = StructuralEquality
//
//        assertTrue(sut.isSatisfied(context, lambda1, lambda2))
//    }
//
//    @Test
//    fun testLambdaNominalEqualityIdentity() {
//        // Proposition: (x -> x) == (x -> x) by name.
//        val x = Type("x")
//        val lambda1 = Lambda(x, x)
//        val lambda2 = Lambda(x, x)
//        val context = Context(lambda1, lambda2)
//        val sut = NominalEquality
//
//        assertTrue(sut.isSatisfied(context, lambda1, lambda2))
//    }
//
//    @Test
//    fun testLambdaStructuralEqualityIdentity() {
//        // Proposition: (x -> x) == (x -> x) contractually.
//        val x = Type("x")
//        val lambda1 = Lambda(x, x)
//        val lambda2 = Lambda(x, x)
//        val context = Context(lambda1, lambda2)
//        val sut = StructuralEquality
//
//        assertTrue(sut.isSatisfied(context, lambda1, lambda2))
//    }
}