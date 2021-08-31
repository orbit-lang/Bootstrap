package org.orbit.types.typeactions

import org.orbit.core.nodes.ModuleNode
import org.orbit.types.components.Module

class CreateModuleStub(override val node: ModuleNode) : CreateStub<ModuleNode, Module> {
    override val constructor: (ModuleNode) -> Module = ::Module
}