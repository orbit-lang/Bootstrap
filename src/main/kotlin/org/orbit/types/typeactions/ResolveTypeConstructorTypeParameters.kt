package org.orbit.types.typeactions

import org.orbit.core.getPath
import org.orbit.core.nodes.EntityConstructorNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.types.components.*
import org.orbit.util.Printer

class ResolveEntityConstructorTypeParameters<EN: EntityConstructorNode, EC: EntityConstructor>(private val node: EN, private val generator: (String, List<TypeParameter>, List<PartiallyResolvedTraitConstructor>, List<TypeSignature>) -> EntityConstructor) : TypeAction {
    private lateinit var entityConstructor: EntityConstructor

    override fun execute(context: Context) {
        entityConstructor = context.getTypeByPath(node.getPath()) as EntityConstructor

        val typeParameters = node.typeParameterNodes
            .map(::TypeParameter)

        val signatures = when (entityConstructor) {
            is TraitConstructor -> (entityConstructor as TraitConstructor).signatures
            else -> emptyList()
        }

        entityConstructor = generator(entityConstructor.name, typeParameters, entityConstructor.partiallyResolvedTraitConstructors, signatures)

        context.remove(entityConstructor.name)
        context.add(entityConstructor)
    }

    override fun describe(printer: Printer): String {
        return "Resolve Type Parameters for Entity Constructor ${entityConstructor.toString(printer)}"
    }
}
