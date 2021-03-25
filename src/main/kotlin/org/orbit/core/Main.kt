package org.orbit.core

import org.orbit.analysis.Analysis
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.Parser
import org.orbit.graph.CanonicalNameResolver
import org.orbit.graph.Environment
import org.orbit.serial.Serialiser
import org.orbit.types.Context
import org.orbit.types.Entity
import org.orbit.types.Lambda
import org.orbit.types.TypeChecker
import org.orbit.util.Invocation
import org.orbit.util.Orbit
import org.orbit.util.Printer
import org.orbit.util.Unix

class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
        	try {
	            val orbit = Orbit()

	            orbit.main(args)

				// TODO - Platform should be derived from System.getProperty("os.name") or similar
				// TODO - Support non *nix platforms
				val invocation = Invocation(Unix)

				val frontend = Frontend(invocation)
	            val semantics = Semantics(invocation)
				val correctness = Correctness(invocation)

				val phaseLinker = PhaseLinker(invocation,
					initialPhase = frontend,
					subsequentPhases = arrayOf(semantics, correctness),
					finalPhase = UnitPhase(invocation))

				val compiler = Compiler(invocation, phaseLinker)
				compiler.execute(FileSourceProvider(orbit.source))

				val result = invocation.getResult<Parser.Result>("Parser")
				val nameResolver = CanonicalNameResolver(invocation)

//				val json = Serialiser.serialise(result.ast)
//				println(json.toString(2))

				val environment = invocation.getResult<Environment>("CanonicalNameResolver")

//				println(invocation.dumpWarnings())
//				println(invocation.dumpErrors())

//				println(environment)

				val printer = Printer(Unix)
				val correctnessResults = invocation.getResult(correctness)

//				Analysis.collate(correctnessResults, printer)

				val context = Context()

				// TODO - Remove these bootstrap types
				context.bind("Test::Int+Test::Int", Lambda(Entity("Test::Int"), Entity("Test::Int")))

				val typeResolver = TypeChecker(invocation, context)

				typeResolver.execute(environment)

//				println(Serialiser.serialise(context).toString(2))
			} catch (ex: Exception) {
				println(ex.message)
//				throw ex
			}
        }
    }
}