package org.orbit.util.next

import org.orbit.core.nodes.Node
import org.orbit.types.next.components.IType

interface NodeMap<N: Node, T> {
    fun set(node: N, value: T)
    fun get(node: N) : T?
}

interface ITypeMap : NodeMap<Node, IType>

class TypeMap : ITypeMap {
    private val map = mutableMapOf<String, IType>()

    override fun set(node: Node, value: IType) {
        map[node.id] = value
    }

    override fun get(node: Node): IType? = map[node.id]
}

interface IBindingScope {
    fun bind(name: String, type: IType)
    fun getType(name: String) : IType?
}

sealed class BindingScope : IBindingScope {
    object Root : BindingScope()
    class Leaf(val parent: IBindingScope) : BindingScope()

    private val bindings = mutableMapOf<String, IType>()

    override fun bind(name: String, type: IType) {
        bindings[name] = type
    }

    override fun getType(name: String) : IType? = bindings[name]
}