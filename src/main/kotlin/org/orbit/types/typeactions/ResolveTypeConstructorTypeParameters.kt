package org.orbit.types.typeactions

import org.orbit.core.getPath
import org.orbit.core.nodes.EntityConstructorNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.types.components.Context
import org.orbit.types.components.EntityConstructor
import org.orbit.types.components.TypeConstructor
import org.orbit.types.components.TypeParameter
import org.orbit.util.Printer

class ResolveEntityConstructorTypeParameters<EN: EntityConstructorNode, EC: EntityConstructor>(private val node: EN, private val generator: (String, List<TypeParameter>) -> EntityConstructor) : TypeAction {
    private lateinit var entityConstructor: EntityConstructor

    override fun execute(context: Context) {
        entityConstructor = context.getTypeByPath(node.getPath()) as EntityConstructor

        val typeParameters = node.typeParameterNodes
            .map(::TypeParameter)

        entityConstructor = generator(entityConstructor.name, typeParameters)

        context.remove(entityConstructor.name)
        context.add(entityConstructor)
    }

    override fun describe(printer: Printer): String {
        return "Resolve Type Parameters for Entity Constructor ${entityConstructor.toString(printer)}"
    }
}
