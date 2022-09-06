package org.orbit.core.nodes

interface ISugarNode<N: INode> : INode {
    fun desugar() : N
}