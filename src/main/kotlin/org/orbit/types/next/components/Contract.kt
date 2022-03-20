package org.orbit.types.next.components

import org.orbit.util.Printer
import org.orbit.util.Semigroup

sealed class ContractResult(open val type: TypeComponent) : Semigroup<ContractResult> {
    object None : ContractResult(Never)
    data class Success(override val type: TypeComponent, val contract: Contract<*>) : ContractResult(type)
    data class Failure(override val type: TypeComponent, val contract: Contract<*>) : ContractResult(type) {
        fun getErrorMessage(printer: Printer, type: TypeComponent) : String
            = contract.getErrorMessage(printer, type)
    }

    data class Group(override val type: TypeComponent, val results: List<ContractResult>) : ContractResult(type) {
        fun getErrorMessage(printer: Printer, type: TypeComponent) : String {
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

interface Contract<T: TypeComponent> {
    val trait: ITrait
    val input: T

    fun isImplemented(ctx: Ctx, by: TypeComponent) : ContractResult
    fun getErrorMessage(printer: Printer, type: TypeComponent) : String
}