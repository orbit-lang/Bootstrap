package org.orbit.types.typeactions

import org.orbit.core.getPath
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.components.Context
import org.orbit.types.components.Trait
import org.orbit.types.components.Type
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.util.Printer

class ResolveTraitConformance(private val node: TypeDefNode) : TypeAction {
    private lateinit var type: Type
    private lateinit var traits: List<Trait>

    override fun execute(context: Context) {
        type = context.getTypeByPath(node.getPath()) as Type
        traits = node.traitConformances.map {
            TypeInferenceUtil.infer(context, it) as Trait
        }

        val nType = Type(node.getPath(), type.typeParameters, type.properties, traits, type.equalitySemantics, false)

        context.remove(type.name)
        context.add(nType)
    }

    override fun describe(printer: Printer): String {
        return "Resolving trait conformance for type ${type.toString(printer)}\n\t\t(${traits.joinToString(", ", transform = { it.toString(printer) })})"
    }
}