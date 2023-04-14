package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeCheckPosition
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.IIntrinsicOperator

fun <M: IIntrinsicOperator> IIntrinsicOperator.Factory<M>.parse(symbol: String) : M? {
    for (modifier in all()) {
        if (modifier.symbol == symbol) return modifier
    }

    return null
}

private object TypeIndexer {
    private var index = 0

    fun next() : Int {
        index += 1
        return index
    }
}

interface IType : IContextualComponent, Substitutable<AnyType> {
    val id: String
    val index: Int get() = TypeIndexer.next()

    fun getCanonicalName() : String = id
    fun flatten(from: AnyType, env: ITypeEnvironment) : AnyType = this
    fun getTypeCheckPosition() : TypeCheckPosition = TypeCheckPosition.Any
    fun getCardinality() : ITypeCardinality
    fun getConstructors() : List<IConstructor<*>> = emptyList()
    fun getUnsolvedTypeVariables() : List<TypeVar> = emptyList()

    fun getPath() : Path
        = OrbitMangler.unmangle(getCanonicalName())

    fun prettyPrint(depth: Int = 0) : String {
        val indent = "\t".repeat(depth)

        return "$indent$id"
    }
}

typealias AnyType = IType
typealias AnyOperator = IOperatorArrow<*, *>
