package org.orbit.fuzz

interface ISourceFuzzer {
    fun fuzz() : String
}

data class CompoundFuzzer(val a: ISourceFuzzer, val b: ISourceFuzzer, val separator: String) : ISourceFuzzer {
    override fun fuzz(): String
        = "${a.fuzz()}$separator${b.fuzz()}"
}

fun ISourceFuzzer.combine(other: ISourceFuzzer, separator: String) : ISourceFuzzer
    = CompoundFuzzer(this, other, separator)