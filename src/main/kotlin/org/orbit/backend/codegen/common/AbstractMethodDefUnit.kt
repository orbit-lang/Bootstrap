package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode

interface AbstractMethodSignatureUnit : CodeUnit<MethodSignatureNode>
interface AbstractMethodDefUnit : CodeUnit<MethodDefNode>

class MethodDefUnit(override val node: MethodDefNode, override val depth: Int) : AbstractMethodDefUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler) : String {
        val signatureUnit = codeGenFactory.getMethodSignatureUnit(node.signature, depth)
        val bodyUnit = codeGenFactory.getBlockUnit(node.body, depth + 1, stripBraces = false, isMethodBody = true)

        return """
            |${signatureUnit.generate(mangler)}
            |${bodyUnit.generate(mangler)}
        """.trimMargin()
    }
}