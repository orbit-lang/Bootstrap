package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.Unit
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.UnitNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

object UnitPathResolver : IPathResolver<UnitNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: UnitNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result
        = IPathResolver.Result.Success(OrbitMangler.unmangle(Unit.getCanonicalName()))
}