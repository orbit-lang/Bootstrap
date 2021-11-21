package org.orbit.core.nodes

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.serial.Serial
import java.io.Serializable

private data class SerialWrapper<T>(val value: T) : Serial, Serializable {
	override fun describe(json: JSONObject) {
		json.put("value", value.toString())
	}
}

sealed class MockAnnotationTag {
	object A : NodeAnnotationTag<SerialWrapper<Int>>
	object B : NodeAnnotationTag<SerialWrapper<String>>
}

private object MockTokenType : TokenType("mock", "", true, false, TokenType.Family.Id)

class MockNode : Node(
	Token(MockTokenType, "mock", SourcePosition(0, 0)),
	Token(MockTokenType, "mock", SourcePosition(0, 0))
) {
	override fun getChildren(): List<Node> = emptyList()
}

class VertexTests {
	@Test fun annotateNonUnique() {
		val node = MockNode()

		assert(node.annotations.isEmpty())

		assertDoesNotThrow {
			node.annotate(SerialWrapper(true), KeyedNodeAnnotationTag("Mock1"))
		}
		
		assert(node.annotations.size == 1)

		assertDoesNotThrow {
			node.annotate(SerialWrapper(false), KeyedNodeAnnotationTag("Mock2"))
		}

		assert(node.annotations.size == 2)
	}

	@Test fun annotateUnique() {
		val node = MockNode()
		
		assert(node.annotations.isEmpty())

		assertDoesNotThrow {
			node.annotate(SerialWrapper("B"), MockAnnotationTag.B)
		}

		assert(node.annotations.size == 1)

		assertThrows<Exception> {
			node.annotate(SerialWrapper("B"), MockAnnotationTag.B)
		}

		assert(node.annotations.size == 1)
	}

	@Test fun annotateUniqueKeyed() {
		val node = MockNode()

		assertDoesNotThrow {
			node.annotate(SerialWrapper(1), KeyedNodeAnnotationTag("1"))
		}

		assertThrows<Exception> {
			node.annotate(SerialWrapper(1), KeyedNodeAnnotationTag("1"))
		}

		assertEquals(1, node.annotations.size)
	}

	@Test fun getAnnotation() {
		val node = MockNode()

		node.annotate(SerialWrapper(99), MockAnnotationTag.A)

		val result = node.getAnnotation(MockAnnotationTag.A)

		assertNotNull(result)
		assertEquals(MockAnnotationTag.A, result!!.tag)
		assertEquals(99, result.value)
	}

	@Test fun getAnnotationWrongType() {
		val node = MockNode()
		
		node.annotate(SerialWrapper(99), MockAnnotationTag.A)
		
		assertThrows<Exception> {
			val result = node.getAnnotation(MockAnnotationTag.B)
			println(result?.value)
		}
	}
}