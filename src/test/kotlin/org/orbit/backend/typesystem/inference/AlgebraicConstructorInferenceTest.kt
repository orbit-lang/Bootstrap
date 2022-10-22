package org.orbit.backend.typesystem.inference

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.orbit.backend.typesystem.components.GlobalEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.core.Path
import org.orbit.core.components.Token
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.NodeAnnotationMap
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.extensions.annotate
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.Unix

internal class AlgebraicConstructorInferenceTest {
    @BeforeEach
    fun setup() {
        startKoin { modules(module {
            single { Invocation(Unix) }
            single { Printer(Unix) }
            single { NodeAnnotationMap() }
        })}
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun infer() {
        val node = AlgebraicConstructorNode(Token.empty, Token.empty, TypeIdentifierNode(Token.empty, Token.empty, "A"), emptyList())

        node.annotate(Path("A"), Annotations.path)

        val result = AlgebraicConstructorInference.infer(node, GlobalEnvironment)

        assertTrue(result is IType.Type)
        assertEquals("A", result.id)
    }
}