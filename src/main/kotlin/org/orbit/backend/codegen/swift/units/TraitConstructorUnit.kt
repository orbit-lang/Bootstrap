package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.TraitConstructorNode
import org.orbit.types.components.Context
import org.orbit.types.components.TraitConstructor
import org.orbit.types.components.TypeParameter
import org.orbit.util.partial

// NOTE - Because generic protocols are dogshit in Swift, its easier to just export as a base class
//class TraitConstructorUnit(override val node: TraitConstructorNode, override val depth: Int) : CodeUnit<TraitConstructorNode>,
//    KoinComponent {
//    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
//
//    override fun generate(mangler: Mangler): String {
//        val traitConstructor = node.getType() as TraitConstructor
//        val traitPath = node.getPath()
//        val typeParametersOrbit = traitConstructor.typeParameters.joinToString(", ", transform = TypeParameter::name)
//        val typeParametersSwift = node.typeParameterNodes
//            .map(partial(::TypeParameterUnit, depth + 1))
//            .map(partial(TypeParameterUnit::generate, mangler))
//            .joinToString(", ")
//
//        val header = "/* trait ${traitPath.toString(OrbitMangler)}(${typeParametersOrbit}) */"
//
//        return """
//            |$header
//            |class ${traitPath.toString(mangler)} <${typeParametersSwift}> {}
//        """.trimMargin().prependIndent(indent())
//    }
//}