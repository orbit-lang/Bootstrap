package org.orbit.backend.codegen

import org.orbit.core.Mangler
import org.orbit.core.nodes.INode

interface CodeUnit<N: INode> {
    val node: N
    val depth: Int

    fun generate(mangler: Mangler) : String
    fun indent(count: Int = depth) : String = "\t".repeat(Integer.max(0, count))
    fun newline(count: Int = 1) : String = "${"\n".repeat(count)}${indent()}"
}