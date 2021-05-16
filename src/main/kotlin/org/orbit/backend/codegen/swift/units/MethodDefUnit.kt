package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.nodes.MethodDefNode

class MethodDefUnit(override val node: MethodDefNode, override val depth: Int) : CodeUnit<MethodDefNode> {
    override fun generate(mangler: Mangler) : String {
        val signatureUnit = MethodSignatureUnit(node.signature, depth)
        val bodyUnit = BlockUnit(node.body, depth + 1)

        return """
            |${signatureUnit.generate(mangler)}
            |${bodyUnit.generate(mangler)}
        """.trimMargin()
    }
}