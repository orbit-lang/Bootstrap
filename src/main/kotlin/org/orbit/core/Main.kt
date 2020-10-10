package org.orbit.core

import org.orbit.analysis.Analysis
import org.orbit.core.nodes.MethodDefNode
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.Parser
import org.orbit.graph.CanonicalNameResolver
import org.orbit.graph.Environment
import org.orbit.serial.Serialiser
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

				val blocks = result.ast.search(MethodDefNode::class.java)

				val json = Serialiser.serialise(result.ast)
				println(json.toString(2))

				val environment = invocation.getResult<Environment>("CanonicalNameResolver")

				println(invocation.dumpWarnings())
				println(invocation.dumpErrors())

//				println(environment)

				val printer = Printer(Unix)
				val correctnessResults = invocation.getResult(correctness)

				Analysis.collate(correctnessResults, printer)
			} catch (ex: Exception) {
				println(ex.message)
//				throw ex
			}
        }
    }
}