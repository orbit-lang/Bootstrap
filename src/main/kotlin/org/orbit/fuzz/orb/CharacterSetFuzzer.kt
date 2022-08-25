package org.orbit.fuzz.orb

import org.orbit.fuzz.ISourceFuzzer

data class CharacterSetFuzzer(val alphabet: String) : ISourceFuzzer {
    companion object {
        val lowerCase = CharacterSetFuzzer("abcdefghijklmnopqrstuvwxyz_")
        val upperCase = CharacterSetFuzzer("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        val digits = CharacterSetFuzzer("0123456789")
        val underscore = CharacterSetFuzzer("_")
        val anyCharacter = lowerCase + upperCase
        val anyCharacterOrDigit = anyCharacter + digits
        val any = anyCharacterOrDigit + underscore
    }

    override fun fuzz(): String
        = alphabet.random().toString()

    fun fuzz(n: Int) : String
        = IntRange(0, n).joinToString { fuzz() }

    operator fun plus(other: CharacterSetFuzzer) : CharacterSetFuzzer
        = CharacterSetFuzzer((alphabet + other).toCharArray().distinct().joinToString())
}