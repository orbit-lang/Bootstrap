package org.orbit.types.typeactions

import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.Node
import org.orbit.types.components.Context
import org.orbit.types.components.TypeProtocol
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

interface CreateStub<N: Node, T: TypeProtocol> : TypeAction {
    val node: N
    val constructor: (N) -> T

    override fun execute(context: Context) {
        context.add(constructor(node))
    }

    override fun describe(printer: Printer): String
        = "Create stub ${printer.apply(node.getPath().toString(OrbitMangler), PrintableKey.Bold)}"
}