package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractAssignmentStatementUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.types.components.Context

class AssignmentStatementUnit(override val node: AssignmentStatementNode, override val depth: Int) : AbstractAssignmentStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<CHeader> by injectQualified(codeGeneratorQualifier)
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)

    override fun generate(mangler: Mangler): String {
        // TODO - Mutability?!
        val rhsPath = node.getPath()
        val rhsType = node.getType()
        val rhsTypeName = OrbitMangler.plus(mangler).invoke(rhsType.name) //(OrbitMangler + mangler)(rhsType.name)
        val rhs = codeGenFactory.getExpressionUnit(node.value, depth).generate(mangler)

        return "$rhsTypeName ${node.identifier.identifier} = $rhs;".prependIndent(indent())
    }
}