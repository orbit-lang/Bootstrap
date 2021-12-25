package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractModuleUnit
import org.orbit.backend.codegen.common.AbstractProgramUnit
import org.orbit.backend.phase.Main
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.ProgramNode
import org.orbit.types.components.IntrinsicTypes
import org.orbit.util.partial

enum class SwiftAccessModifier {
    Private, Fileprivate, Public, Internal;

    override fun toString() : String = name.toLowerCase()
}

fun generateTypeAlias(mangler: Mangler, access: SwiftAccessModifier = SwiftAccessModifier.Public, orbitType: Path, swiftType: String) : String {
    return "$access typealias ${orbitType.toString(mangler)} = $swiftType"
}

fun generateTypeAlias(mangler: Mangler, access: SwiftAccessModifier = SwiftAccessModifier.Public, intrinsicType: IntrinsicTypes) : String {
    val orbitPath = OrbitMangler.unmangle(intrinsicType.type.name)

    return when (intrinsicType) {
        IntrinsicTypes.Unit -> generateTypeAlias(mangler, access, orbitPath, "()")
        IntrinsicTypes.Int -> generateTypeAlias(mangler, access, orbitPath, "Int")
        else -> ""
    }
}

private object ProgramUtilsUnit {
    fun generate(mangler: Mangler) : String {
        return "import OrbCore"
    }
}

class ProgramUnit(override val node: ProgramNode, override val depth: Int = 0) : AbstractProgramUnit<SwiftHeader>, KoinComponent {
    private val main: Main by injectResult(CompilationSchemeEntry.mainResolver)
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override val header = SwiftHeader

    override fun generate(mangler: Mangler): String {
        val modules = ProgramUtilsUnit.generate(mangler) + node.declarations
            .filterIsInstance<ModuleNode>()
            .map(partial(codeGenFactory::getModuleUnit, depth, SwiftHeader))
            .joinToString(newline(), transform = partial(AbstractModuleUnit::generate, mangler))

        val mainCall = try {
            when (main.mainSignature) {
                null -> ""
                else -> {
                    val sig = main.mainSignature!!
                    val recPath = sig.receiver.getFullyQualifiedPath()
                    val recName = mangler.mangle(recPath)
                    val retPath = sig.returnType.getFullyQualifiedPath()
                    val retName = mangler.mangle(retPath)

                    "${recName}_main_${recName}_$retName(self: ${recName}(argc: 0, argv: []))"
                }
            }
        } catch (_: Exception) {
            ""
        }

        return "$modules\n\n$mainCall"
    }
}