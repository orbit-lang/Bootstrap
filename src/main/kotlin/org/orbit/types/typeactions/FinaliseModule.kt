package org.orbit.types.typeactions

import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ModuleNode
import org.orbit.types.components.Context
import org.orbit.types.components.Entity
import org.orbit.types.components.Module
import org.orbit.util.Printer

class FinaliseModule(private val node: ModuleNode) : TypeAction {
    private var ownedTypes = emptyList<Entity>()
    private var result: Module? = null

    override fun execute(context: Context) {
        val module = context.getTypeByPath(node.getPath()) as Module

        ownedTypes = context.types
            .filterIsInstance<Entity>()
            .map { OrbitMangler.unmangle(it.name) }
            .filter { node.getPath().isAncestor(it) }
            .map { context.getTypeByPath(it) as Entity }

        result = Module(node.getPath(), entities = ownedTypes, signatures = module.signatures)

        context.remove(module.name)
        context.add(result!!)
    }

    override fun describe(printer: Printer): String {
        return """
            |Finalise module type ${node.getPath().toString(printer)}
            |${result!!.toString(printer)}
        """.trimMargin()
    }
}