package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.types.components.*

//internal class InferenceTests {
//    @Test
//    fun testVariableInferenceTrue() {
//        val a = Type("A")
//        val context = Context(a)
//
//        context.bind("a", a)
//
//        val variable = Variable("a")
//        val result = TypeInferenceUtil.infer(context, variable)
//
//        assertTrue(result is Entity)
//        assertEquals("A", result.name)
//    }
//
//    @Test
//    fun testVariableInferenceFalse() {
//        val a = Entity("A")
//        val context = Context(a)
//        val variable = Variable("a")
//
//        assertThrows<Exception> {
//            TypeInferenceUtil.infer(context, variable)
//        }
//    }
//
//    @Test
//    fun testBinaryInference() {
//        val x = Entity("x")
//        val context = Context(x)
//        val lambda = Lambda(x, x)
//
//        context.bind("x+x", lambda)
//
//        val binary = Binary("+", x, x)
//        val result = TypeInferenceUtil.infer(context, binary)
//
//        assertTrue(result is Lambda)
//        assertEquals("x -> x", result.name)
//    }
//
//    @Test
//    fun testAssignmentInference() {
//        val x = Entity("x")
//        val context = Context(x)
//
//        context.bind("a", x)
//
//        val variable = Variable("a")
//        val assignment = Assignment("y", variable)
//        val result = TypeInferenceUtil.infer(context, assignment)
//
//        assertTrue(result is Entity)
//        assertEquals("x", result.name)
//    }
//
//    @Test
//    fun testBlockInference() {
//        val x = Entity("x")
//        val y = Entity("y")
//        val context = Context(x, y)
//
//        context.bind("a", x)
//        context.bind("b", y)
//
//        val varA = Variable("a")
//        val varB = Variable("b")
//        val assignmentC = Assignment("c", varA)
//        val assignmentD = Assignment("d", varB)
//        val block = Block(listOf(assignmentC, assignmentD))
//        val result = TypeInferenceUtil.infer(context, block)
//
//        assertTrue(result is Entity)
//        assertEquals("y", result.name)
//    }
//}