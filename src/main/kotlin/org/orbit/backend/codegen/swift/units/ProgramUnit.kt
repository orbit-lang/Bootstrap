package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.phase.Main
import org.orbit.backend.codegen.CodeUnit
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
//        return """
//            |/* Intrinsic Types */
//            |${generateTypeAlias(mangler, SwiftAccessModifier.Internal, IntrinsicTypes.Unit)}
//            |${generateTypeAlias(mangler, SwiftAccessModifier.Internal, IntrinsicTypes.Int)}
//            |struct ${mangler.mangle(OrbitMangler.unmangle(IntrinsicTypes.Main.type.name))} {
//            |   let argc: Int
//            |}
//            |struct ${mangler.mangle(OrbitMangler.unmangle(IntrinsicTypes.Symbol.type.name))} {
//            |   static let emptySymbol = Self(value: "")
//            |   let value: String
//            |}
//        """.trimMargin()
    }
}

class ProgramUnit(override val node: ProgramNode, override val depth: Int = 0) : CodeUnit<ProgramNode>, KoinComponent {
    private val main: Main by injectResult(CompilationSchemeEntry.mainResolver)

    override fun generate(mangler: Mangler): String {
        val modules = "${ProgramUtilsUnit.generate(mangler)}\n\n" + node.declarations
            .filterIsInstance<ModuleNode>()
            .map(partial(::ModuleUnit, depth))
            .map(partial(ModuleUnit::generate, mangler))
            .joinToString(newline(2))

        val mainCall = try {
            when (main.mainSignature) {
                null -> ""
                else -> {
                    val sig = main.mainSignature!!
                    val rec = (OrbitMangler + mangler)(sig.receiver.name)
                    val ret = (OrbitMangler + mangler)(sig.returnType.name)

                    "${rec}_main_${rec}_$ret(self: ${rec}(argc: 0, argv: []))"
                }
            }
        } catch (_: Exception) {
            ""
        }



        return "$modules\n\n$mainCall"
    }
}