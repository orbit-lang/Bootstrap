package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractConstructorUnit
import org.orbit.backend.codegen.common.AbstractExpressionUnit
import org.orbit.backend.phase.CodeWriter
import org.orbit.core.*
import org.orbit.core.nodes.ConstructorNode
import org.orbit.types.components.Entity
import org.orbit.types.components.Property
import org.orbit.types.components.Type
import org.orbit.util.Invocation
import org.orbit.util.partial

class ConstructorUnit(override val node: ConstructorNode, override val depth: Int) : AbstractConstructorUnit, KoinComponent {
    private val invocation: Invocation by inject()
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val targetType = node.typeExpressionNode.getType() as Type

        if (targetType !is Entity) {
            throw invocation.make<CodeWriter>("Only types may be initialised via a constructor call. Found $targetType", node.typeExpressionNode)
        }

        val parameters = when (node.parameterNodes.count() == targetType.properties.count()) {
            true -> node.parameterNodes
                .map { codeGenFactory.getExpressionUnit(it, depth) }
                .map(partial(AbstractExpressionUnit::generate, mangler))

            else -> {
                targetType.properties.mapIndexed { idx, item ->
                    when (val def = item.defaultValue) {
                        null -> codeGenFactory.getExpressionUnit(node.parameterNodes[idx], depth).generate(mangler)
                        else -> codeGenFactory.getExpressionUnit(def, depth).generate(mangler)
                    }
                }
            }
        }

        val parameterNames = targetType.properties.map(Property::name)

        val properties = parameterNames.zip(parameters).joinToString(", ") {
            "${it.first}: ${it.second}"
        }

        val targetTypeName = targetType.getFullyQualifiedPath().toString(mangler)

        return """ 
            |$targetTypeName($properties)
        """.trimMargin()
    }
}