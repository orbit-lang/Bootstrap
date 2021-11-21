package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractHeader
import org.orbit.backend.codegen.common.AbstractModuleUnit
import org.orbit.backend.codegen.common.AbstractProgramUnit
import org.orbit.backend.phase.Main
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.ProgramNode
import org.orbit.util.Build
import org.orbit.util.partial

class ProgramUnit(override val node: ProgramNode, override val depth: Int) : AbstractProgramUnit<CHeader>, KoinComponent {
    private val main: Main by injectResult(CompilationSchemeEntry.mainResolver)
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<CHeader> by injectQualified(codeGeneratorQualifier)
    private val buildConfig: Build.BuildConfig by inject()

    override val header = CHeader()

    private fun generateIncludes(mangler: Mangler) : String {
        return """
            |#include <stdio.h>
            |#include <Orb/OrbCore.h>
            |#include "${buildConfig.productName}.${codeGeneratorQualifier.headerExtension}"
        """.trimMargin()
    }

    override fun generate(mangler: Mangler): String {
        val modules = "${generateIncludes(mangler)}\n\n" + node.declarations
            .filterIsInstance<ModuleNode>()
            .map(partial(codeGenFactory::getModuleUnit, depth, header))
            .joinToString(newline(2), transform = partial(AbstractModuleUnit::generate, mangler))

        val mainCall = try {
            when (main.mainSignature) {
                null -> ""
                else -> {
                    val sig = main.mainSignature!!
                    val rec = (OrbitMangler + mangler)(sig.receiver.name)
                    val ret = (OrbitMangler + mangler)(sig.returnType.name)

                    """
                        int main() {
                            Orb_Core_Main_Main main = {0};
                            ${rec}_main_${rec}_$ret(main);
                        }
                    """.trimIndent()
                }
            }
        } catch (_: Exception) {
            ""
        }

        return "$modules\n\n$mainCall"
    }
}