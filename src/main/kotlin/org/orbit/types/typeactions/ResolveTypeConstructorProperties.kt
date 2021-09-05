package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.injectResult
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.Context
import org.orbit.types.components.Property
import org.orbit.types.components.TypeConstructor
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

class ResolveTypeConstructorProperties(private val node: TypeConstructorNode) : TypeAction {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
    }

    private lateinit var typeConstructor: TypeConstructor

    override fun execute(context: Context) {
        typeConstructor = context.getTypeByPath(node.getPath()) as TypeConstructor

        val properties = node.properties.map {
            val path = it.typeExpressionNode.getPath()
            val pType = context.getTypeOrNull(path)
                ?: typeConstructor.getTypeParameterOrNull(path)
                ?: throw invocation.make<TypeSystem>("Could not find entity named '${path.toString(OrbitMangler)}' referenced by Type Constructor property '${it.identifierNode.identifier}'", it.typeExpressionNode)

            Property(it.identifierNode.identifier, pType)
        }

        typeConstructor = TypeConstructor(typeConstructor.name, typeConstructor.typeParameters, properties)

        node.annotate(typeConstructor, Annotations.Type, true)

        context.remove(typeConstructor.name)
        context.add(typeConstructor)
    }

    override fun describe(printer: Printer): String {
        return "Resolving properties for Type Constructor ${typeConstructor.toString(printer)}"
    }
}