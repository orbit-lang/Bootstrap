package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.injectResult
import org.orbit.core.nodes.EntityConstructorNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

class ResolveEntityConstructorProperties<N: EntityConstructorNode, C: EntityConstructor>(private val node: N, private val generator: (String, List<TypeParameter>, List<Property>, List<PartiallyResolvedTraitConstructor>) -> C) : TypeAction {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
    }

    private lateinit var entityConstructor: EntityConstructor

    override fun execute(context: Context) {
        entityConstructor = context.getTypeByPath(node.getPath()) as EntityConstructor

        val properties = node.properties.map {
            val path = it.typeExpressionNode.getPath()
            val pType = context.getTypeOrNull(path)
                ?: entityConstructor.getTypeParameterOrNull(path)
                ?: throw invocation.make<TypeSystem>("Could not find entity named '${path.toString(OrbitMangler)}' referenced by Type Constructor property '${it.identifierNode.identifier}'", it.typeExpressionNode)

            Property(it.identifierNode.identifier, pType)
        }

        entityConstructor = generator(entityConstructor.name, entityConstructor.typeParameters, properties, entityConstructor.partiallyResolvedTraitConstructors)

        node.annotate(entityConstructor, Annotations.Type, true)

        context.remove(entityConstructor.name)
        context.add(entityConstructor)
    }

    override fun describe(printer: Printer): String {
        return "Resolving properties for Entity Constructor ${entityConstructor.toString(printer)}"
    }
}
