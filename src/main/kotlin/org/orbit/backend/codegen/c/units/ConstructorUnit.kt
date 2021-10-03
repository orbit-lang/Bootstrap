package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.common.AbstractConstructorUnit
import org.orbit.backend.phase.CodeWriter
import org.orbit.core.*
import org.orbit.core.nodes.ConstructorNode
import org.orbit.types.components.Entity
import org.orbit.types.components.Property
import org.orbit.util.Invocation

class ConstructorUnit(override val node: ConstructorNode, override val depth: Int) : AbstractConstructorUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGeneratorQualifier)
    private val invocation: Invocation by inject()

    override fun generate(mangler: Mangler): String {
        val targetType = node.typeExpressionNode.getType()

        if (targetType !is Entity) {
            throw invocation.make<CodeWriter>("Only types may be initialised via a constructor call. Found $targetType", node.typeExpressionNode)
        }

        val parameters = node.parameterNodes
            .map { codeGenFactory.getExpressionUnit(it, depth) }
            .map { it.generate(mangler) }

        val parameterNames = targetType.properties.map(Property::name)

        val properties = parameterNames.zip(parameters).joinToString(", ") {
            it.second
        }

        val targetTypeName = (OrbitMangler + mangler).invoke(targetType.name)

        return """ 
            |(($targetTypeName){$properties})
        """.trimMargin()
    }
}