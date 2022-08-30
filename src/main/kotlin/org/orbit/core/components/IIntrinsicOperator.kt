package org.orbit.core.components

interface IIntrinsicOperator {
    interface Factory<M: IIntrinsicOperator> {
        fun all() : List<M>
    }

    val symbol: String
}