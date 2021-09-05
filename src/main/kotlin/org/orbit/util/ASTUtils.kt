package org.orbit.util

import org.koin.core.component.KoinComponent
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.injectResult
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Environment
import org.orbit.types.components.Context
import org.orbit.types.components.Type
import org.orbit.types.components.TypeProjection

class ASTUtil : KoinComponent {
    private val parseResult: Parser.Result by injectResult(CompilationSchemeEntry.parser)
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)

    fun <N: Node> search(clazz: Class<N>) : List<N>
        = parseResult.ast.search(clazz)

    fun getTypeProjections(forType: Type) : List<TypeProjection> {
        return search(TypeProjectionNode::class.java)
            .mapNotNull {
                context.getTypeByPath(it.getPath()) as? TypeProjection
            }
            .filter { it.type == forType }
    }
}