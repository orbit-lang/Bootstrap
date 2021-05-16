package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.orbit.types.components.Entity
import org.orbit.types.components.Lambda
import org.orbit.types.components.Property

internal class ClauseTests {
    @Test
    fun testEntityNominalIdentity() {
        // Proposition: A == A by name where A is an entity
        val entity = Entity("A")
        val sut = NominalEquality(entity, entity)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testEntityStructuralIdentity() {
        // Proposition: A == A by contract where A is an entity
        val entity = Entity("A")
        val sut = StructuralEquality(entity, entity)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testEntityNominalEqualityTrue() {
        // Proposition: A == A where A & A are distinct entities (referentially) with identical contracts.
        val entity1 = Entity("A")
        val entity2 = Entity("A")
        val sut = NominalEquality(entity1, entity2)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testEntityNominalEqualityFalse() {
        // Proposition: A != B by name where A & B are distinct entities with identical contracts.
        val entity1 = Entity("A")
        val entity2 = Entity("B")
        val sut = NominalEquality(entity1, entity2)

        assertFalse(sut.satisfied())
    }

    @Test
    fun testEntityStructuralEqualityEmpty() {
        // Proposition: A == B contractually where A & B are distinct entities with identical contracts.
        val entity1 = Entity("A")
        val entity2 = Entity("B")
        val sut = StructuralEquality(entity1, entity2)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testEntityStructuralEqualitySingleMemberTrue() {
        // Proposition: A{x: X} == B{x: X}
        // where A & B & X are distinct entities AND A & B have identical contracts.
        val member = Property("x", Entity("X"))
        val entity1 = Entity("A", listOf(member))
        val entity2 = Entity("B", listOf(member))
        val sut = StructuralEquality(entity1, entity2)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testEntityStructuralEqualitySingleMemberFalse() {
        // Proposition: A(x: X) != B(y: Y) contractually.
        val member1 = Property("x", Entity("X"))
        val member2 = Property("y", Entity("X"))
        val entity1 = Entity("A", listOf(member1))
        val entity2 = Entity("B", listOf(member2))
        val sut = StructuralEquality(entity1, entity2)

        assertFalse(sut.satisfied())
    }

    @Test
    fun testEntityStructuralEqualityMultipleMembers() {
        // Proposition: A(x: T) != B(y: T) contractually.
        val member1 = Entity("T") + "x"
        val member2 = Entity("T") + "y"
        val entity1 = Entity("A", listOf(member1, member2))
        val entity2 = Entity("B", listOf(member2, member1))
        val sut = StructuralEquality(entity1, entity2)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testLambdaStructuralEqualityEntityToEntityTrue() {
        // Proposition: (x -> x) == (y -> y) where x & y are Entities AND x == y contractually
        val x = Entity("x")
        val y = Entity("y")
        val lambda1 = Lambda(x, x)
        val lambda2 = Lambda(y, y)
        val sut = StructuralEquality(lambda1, lambda2)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testLambdaNominalEqualityIdentity() {
        // Proposition: (x -> x) == (x -> x) by name.
        val x = Entity("x")
        val lambda1 = Lambda(x, x)
        val lambda2 = Lambda(x, x)
        val sut = NominalEquality(lambda1, lambda2)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testLambdaStructuralEqualityIdentity() {
        // Proposition: (x -> x) == (x -> x) contractually.
        val x = Entity("x")
        val lambda1 = Lambda(x, x)
        val lambda2 = Lambda(x, x)
        val sut = StructuralEquality(lambda1, lambda2)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testApplicativeEqualityIdenticalEntityTrue() {
        // Proposition: (x -> x) accepts an "x" contractually.
        val x = Entity("x")
        // x -> x
        val lambda = Lambda(x, x)
        // x(x)
        val application = Application(lambda, x)
        val sut = ApplicativeEquality(application)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testApplicativeEqualityImplicitlyBivariantEntitiesTrue() {
        // Proposition: (x -> x) accepts a "y" where y satisfies the contact of x.
        /**
         * x & y are bivariant here b/c each satisfies the other's contract[1][2].
         * Application is valid by default if y satisfies x's contract.
         *
         * 1. Implicitly due to x & y being structurally identical.
         * 2. Variance can be implied even though x & y do not declare conformance to each other.
         */
        val x = Entity("x")
        val y = Entity("y")
        // x -> x
        val lambda = Lambda(x, x)
        // x(y)
        val application = Application(lambda, y)
        val sut = ApplicativeEquality(application)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testApplicativeEqualityImplicitlyCovariantEntitiesTrue() {
        // Proposition: (x -> x) accepts a "y" where y satisfies AND extends the contract of x.
        /**
         * x & y have an implicit covariance here: x < y.
         * A y can be used in place of (upcast) an x because y satisfies x's structural contract.
         * NOTE - x cannot be downcast to y as its contract does not specify a member "a" of type x.
         */
        val x = Entity("x")
        val y = Entity("y", listOf(Property("a", x)))
        // x -> x
        val lambda = Lambda(x, x)
        // x(y)
        val application = Application(lambda, y)
        val sut = ApplicativeEquality(application)

        assertTrue(sut.satisfied())
    }

    @Test
    fun testApplicativeEqualityImplicitlyCovariantEntitiesFalse() {
        // Proposition: (x -> x) rejects a "y" where y does not satisfy the contract of x.
        /**
         * The implied relation x < y is not satisfied by y b/c it
         * does not declare a member "a" of type "Any" in its contract.
         * Therefore, y cannot be upcast to x in this context.
         */
        val x = Entity("x", listOf(Property("a", Entity("Any"))))
        val y = Entity("y")
        // x -> x
        val lambda = Lambda(x, x)
        // x(y)
        val application = Application(lambda, y)
        val sut = ApplicativeEquality(application)

        assertFalse(sut.satisfied())
    }
}