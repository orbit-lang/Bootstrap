package org.orbit.core.nodes

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import kotlin.test.fail

sealed class MockAnnotationTag {
	object A : NodeAnnotationTag<Int>
	object B : NodeAnnotationTag<String>
}

private class MockNode : Node()

class NodeTests {
	@Test fun annotateNonUnique() {
		val node = MockNode()

		assert(node.annotations.isEmpty())

		assertDoesNotThrow {
			node.annotate(true, KeyedNodeAnnotationTag("Mock1"))
		}
		
		assert(node.annotations.size == 1)

		assertDoesNotThrow {
			node.annotate(false, KeyedNodeAnnotationTag("Mock2"))
		}

		assert(node.annotations.size == 2)
	}

	@Test fun annotateUnique() {
		val node = MockNode()
		
		assert(node.annotations.isEmpty())

		assertDoesNotThrow {
			node.annotate("B", MockAnnotationTag.B)
		}

		assert(node.annotations.size == 1)

		assertThrows<Exception> {
			node.annotate("B", MockAnnotationTag.B)
		}

		assert(node.annotations.size == 1)
	}

	@Test fun annotateUniqueKeyed() {
		val node = MockNode()

		assertDoesNotThrow {
			node.annotate(1, KeyedNodeAnnotationTag("1"))
		}

		assertThrows<Exception> {
			node.annotate(1, KeyedNodeAnnotationTag("1"))
		}

		assertEquals(1, node.annotations.size)
	}

	@Test fun getAnnotation() {
		val node = MockNode()

		node.annotate(99, MockAnnotationTag.A)

		val result = node.getAnnotation(MockAnnotationTag.A)

		assertNotNull(result)
		assertEquals(MockAnnotationTag.A, result!!.tag)
		assertEquals(99, result.value)
	}

	@Test fun getAnnotationWrongType() {
		val node = MockNode()
		
		node.annotate(99, MockAnnotationTag.A)
		
		assertThrows<Exception> {
			val result = node.getAnnotation(MockAnnotationTag.B)
			println(result?.value)
		}
	}
}