package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.UnitNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation
import kotlin.io.path.Path

object UnitPathResolver : IPathResolver<UnitNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: UnitNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result
        = IPathResolver.Result.Success(OrbitMangler.unmangle(IType.Unit.getCanonicalName()))
}