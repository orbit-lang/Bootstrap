package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

sealed interface IIntrinsicType : IType {
    sealed interface IIntegralType<Self: IIntegralType<Self>> : IConstructableType<Self> {
        val maxValue: Int
        val minValue: Int
    }

    // 32-bit Signed Integer (Kotlin default)
    object RawInt : IIntrinsicType, IIntegralType<RawInt> {
        private object RawIntConstructor : IConstructor<RawInt> {
            override val id: String = "(ℤ) -> ℤ"
            override val effects: List<Effect> = emptyList()

            override val constructedType: RawInt = RawInt
            override fun getDomain(): List<AnyType> = listOf(RawInt)
            override fun getCodomain(): AnyType = RawInt
            override fun curry(): IArrow<*> = this

            override fun never(args: List<AnyType>): Never {
                TODO("Not yet implemented")
            }

            override fun substitute(substitution: Substitution): AnyType = this
            override fun equals(other: Any?): Boolean = other is RawIntConstructor
        }

        override val id: String = "ℤ"
        override val maxValue: Int = Int.MAX_VALUE
        override val minValue: Int = Int.MIN_VALUE

        override fun isSpecialised(): Boolean = false

        override fun equals(other: Any?): Boolean = when (other) {
            is RawInt -> true
            OrbCoreNumbers.intType -> true
            else -> false
        }

        override fun getConstructors(): List<IConstructor<RawInt>> = listOf(RawIntConstructor)
        override fun getCardinality(): ITypeCardinality = ITypeCardinality.Infinite

        override fun substitute(substitution: Substitution): AnyType = when (substitution.old) {
            this -> substitution.new
            else -> this
        }

        override fun prettyPrint(depth: Int): String {
            val indent = "\t".repeat(depth)
            val printer = getKoinInstance<Printer>()

            return "$indent${printer.apply(id, PrintableKey.Bold)}"
        }

        override fun toString(): String = prettyPrint()
    }
}