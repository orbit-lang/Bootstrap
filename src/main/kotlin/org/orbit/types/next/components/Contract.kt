package org.orbit.types.next.components

import org.orbit.util.Printer
import org.orbit.util.Semigroup

sealed class ContractResult(open val type: IType) : Semigroup<ContractResult> {
    object None : ContractResult(Never)
    data class Success(override val type: IType, val contract: Contract<*>) : ContractResult(type)
    data class Failure(override val type: IType, val contract: Contract<*>) : ContractResult(type) {
        fun getErrorMessage(printer: Printer, type: IType) : String
            = contract.getErrorMessage(printer, type)
    }

    data class Group(override val type: IType, val results: List<ContractResult>) : ContractResult(type) {
        fun getErrorMessage(printer: Printer, type: IType) : String {
            val failures = results.filterIsInstance<Failure>()

            return failures.joinToString("\n") { it.contract.getErrorMessage(printer, type) }
        }
    }

    override fun plus(other: ContractResult): ContractResult = when (this) {
        is None -> other
        is Group -> when (other) {
            is Group -> Group(type, results + other.results)
            else -> Group(type, results + other)
        }

        else -> when (other) {
            is Group -> other + this
            else -> Group(type,  listOf(this, other))
        }
    }
}

interface Contract<T: IType> {
    val trait: Trait
    val input: T

    fun isImplemented(ctx: Ctx, by: IType) : ContractResult
    fun getErrorMessage(printer: Printer, type: IType) : String
}