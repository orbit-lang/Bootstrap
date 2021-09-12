package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.phase.CodeWriter
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.ConstructorNode
import org.orbit.types.components.Context
import org.orbit.types.components.Entity
import org.orbit.types.components.Property
import org.orbit.util.Invocation
import org.orbit.util.partial

class ConstructorUnit(override val node: ConstructorNode, override val depth: Int) : CodeUnit<ConstructorNode>,
    KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    private val invocation: Invocation by inject()

    override fun generate(mangler: Mangler): String {
        val targetType = node.typeExpressionNode.getType()

        if (targetType !is Entity) {
            throw invocation.make<CodeWriter>("Only types may be initialised via a constructor call. Found $targetType", node.typeExpressionNode)
        }

        val parameters = node.parameterNodes
            .map(partial(::ExpressionUnit, depth))
            .map(partial(ExpressionUnit::generate, mangler))

        val parameterNames = targetType.properties.map(Property::name)

        val properties = parameterNames.zip(parameters).joinToString(", ") {
            "${it.first}: ${it.second}"
        }

        val targetTypeName = (OrbitMangler + mangler).invoke(targetType.name)

        return """ 
            |$targetTypeName($properties)
        """.trimMargin()
    }
}