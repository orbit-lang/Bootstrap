package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.ProgramNode
import org.orbit.types.IntrinsicTypes
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
        return """
            |/* Intrinsic Types */
            |${generateTypeAlias(mangler, SwiftAccessModifier.Internal, IntrinsicTypes.Unit)}
            |${generateTypeAlias(mangler, SwiftAccessModifier.Internal, IntrinsicTypes.Int)}
            |struct ${mangler.mangle(OrbitMangler.unmangle(IntrinsicTypes.Symbol.type.name))} {
            |   static let emptySymbol = Self(value: "")
            |   let value: String
            |}
        """.trimMargin()
    }
}

class ProgramUnit(override val node: ProgramNode, override val depth: Int = 0) : CodeUnit<ProgramNode> {
    override fun generate(mangler: Mangler): String {
        return "${ProgramUtilsUnit.generate(mangler)}\n\n" + node.declarations
            .filterIsInstance<ModuleNode>()
            .map(partial(::ModuleUnit, depth))
            .map(partial(ModuleUnit::generate, mangler))
            .joinToString(newline(2))
    }
}