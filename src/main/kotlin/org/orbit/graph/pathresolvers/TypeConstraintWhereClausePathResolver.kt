package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.TraitConformanceTypeConstraintNode
import org.orbit.core.nodes.TypeConstraintWhereClauseNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

object TypeConstraintWhereClausePathResolver : PathResolver<TypeConstraintWhereClauseNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TypeConstraintWhereClauseNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result = when (input.statementNode) {
		is TraitConformanceTypeConstraintNode -> TypeConstraintPathResolver.resolve(
            input.statementNode,
            pass,
            environment,
            graph
        )
		else -> TODO("EntityConstructorWhereClausePathResolver")
	}
}