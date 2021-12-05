package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

class ResolveTypeConstructorTraitConformance(private val node: TypeConstructorNode) : TypeAction, KoinComponent {
    private lateinit var typeConstructor: TypeConstructor
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun execute(context: Context) {
        typeConstructor = context.getTypeByPath(node.getPath()) as TypeConstructor

        val partiallyResolvedTraitConstructors = mutableListOf<PartiallyResolvedTraitConstructor>()
        for (traitNode in node.traitConformance as List<MetaTypeNode>) {
            val traitPath = traitNode.getPath()
            val traitConstructor = context.getTypeByPath(traitPath) as TraitConstructor

            val abstractParameters = traitConstructor.typeParameters
            val concreteParameters = typeConstructor.typeParameters
            val specifiedParameterNodes = traitNode.typeParameters

            val aPCount = abstractParameters.count()
            val sPCount = specifiedParameterNodes.count()

            if (sPCount != aPCount) {
                throw invocation.make<TypeSystem>("Incorrect number of type parameters passed to Trait Constructor ${traitConstructor.toString(printer)}. Expected $, found $sPCount", traitNode)
            }

            val typeParameterMap = mutableMapOf<TypeParameter, TypeParameter>()
            val specifiedParameters = specifiedParameterNodes
                .map { concreteParameters.find { tp -> tp.name == it.getPath().toString(OrbitMangler) }!! }

            abstractParameters.zip(specifiedParameters)
                .forEach { typeParameterMap[it.first] = it.second }

            partiallyResolvedTraitConstructors.add(PartiallyResolvedTraitConstructor(traitConstructor, typeParameterMap))
        }

        typeConstructor = TypeConstructor(typeConstructor, partiallyResolvedTraitConstructors)

        context.replace(typeConstructor)
    }

    override fun describe(printer: Printer): String {
        return "Resolving Trait conformance for Type Constructor ${typeConstructor.toString(printer)}"
    }
}