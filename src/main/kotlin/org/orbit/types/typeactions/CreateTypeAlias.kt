package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.orbit.core.Path
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.injectResult
import org.orbit.core.nodes.TypeAliasNode
import org.orbit.graph.components.Binding
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.Context
import org.orbit.types.components.Type
import org.orbit.types.components.TypeAlias
import org.orbit.types.typeresolvers.TypeExpressionTypeResolver
import org.orbit.util.Printer

class CreateTypeAlias(private val node: TypeAliasNode) : TypeAction {
    private companion object : KoinComponent {
        private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)
    }

    private lateinit var typeAlias: TypeAlias

    override fun execute(context: Context) {
        val typeExpressionResolver = TypeExpressionTypeResolver(node.targetTypeIdentifier, Binding(Binding.Kind.Type, "", Path()))
        val targetType = typeExpressionResolver.resolve(nameResolverResult.environment, context)
            .evaluate(context) as Type

        typeAlias = TypeAlias(node.getPath(), targetType)

        context.add(typeAlias)
    }

    override fun describe(printer: Printer): String {
        return "Create Type Alias ${typeAlias.toString(printer)}"
    }
}