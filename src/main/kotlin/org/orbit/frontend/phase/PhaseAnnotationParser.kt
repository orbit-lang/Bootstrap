package org.orbit.frontend.phase

import org.orbit.core.phase.ReifiedPhase
import org.orbit.graph.components.Environment
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