package org.orbit.frontend

import org.orbit.core.*
import org.orbit.graph.Environment
import org.orbit.util.Invocation

/**
 * "Phase time" is a compiler pass that
 */
class PhaseAnnotationParser(override val invocation: Invocation) : ReifiedPhase<Environment, Environment> {
    override val inputType: Class<Environment> = Environment::class.java
    override val outputType: Class<Environment> = Environment::class.java

    override fun execute(input: Environment): Environment {
        return input
    }
}