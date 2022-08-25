package org.orbit.fuzz.orb

import org.orbit.fuzz.ISourceFuzzer
import kotlin.random.Random

object IntLiteralFuzzer : ISourceFuzzer {
    override fun fuzz(): String
        = Random(0).nextInt(Int.MAX_VALUE).toString()
}