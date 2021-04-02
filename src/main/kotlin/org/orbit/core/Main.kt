package org.orbit.core

import org.orbit.analysis.Analysis
import org.orbit.backend.*
import org.orbit.frontend.*
import org.orbit.frontend.rules.DefineRule
import org.orbit.frontend.rules.ProgramRule
import org.orbit.graph.CanonicalNameResolver
import org.orbit.graph.Environment
import org.orbit.serial.Serialiser
import org.orbit.types.Context
import org.orbit.types.Entity
import org.orbit.types.Lambda
import org.orbit.types.TypeChecker
import org.orbit.util.*

class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
        	try {
	            val orbit = Orbit()

	            orbit.main(args)

				// TODO - Platform should be derived from System.getProperty("os.name") or similar
				// TODO - Support non *nix platforms
				val invocation = Invocation(Unix)
				val sourceReader = FileSourceProvider(orbit.source)
				val dummyPhase = DummyPhase(invocation, sourceReader)

				invocation.storeResult(dummyPhase, sourceReader)

				val compilerGenerator = CompilerGenerator(invocation)

				compilerGenerator["__source__"] = dummyPhase
				compilerGenerator["CommentParser"] = CommentParser(invocation)
				compilerGenerator["Lexer"] = Lexer(invocation)
				compilerGenerator["Parser"] = Parser(invocation, ProgramRule)
				compilerGenerator["Observers"] = ObserverPhase(invocation)
				compilerGenerator["CanonicalNameResolver"] = CanonicalNameResolver(invocation)
				compilerGenerator["TypeChecker"] = TypeChecker(invocation)

				compilerGenerator.eventBus.events.registerObserver {
					val printer = Printer(invocation.platform.getPrintableFactory())
					val eventName = printer.apply(it.identifier, PrintableKey.Bold, PrintableKey.Underlined)

					println("Compiler event: $eventName")
				}

				compilerGenerator.run(CompilationScheme.canonicalScheme)

//				val frontend = Frontend(invocation)
//	            val semantics = Semantics(invocation)
//				val correctness = Correctness(invocation)
//
//				val phaseLinker = PhaseLinker(invocation,
//					initialPhase = frontend,
//					subsequentPhases = arrayOf(semantics, correctness),
//					finalPhase = UnitPhase(invocation))
//
//				val compiler = Compiler(invocation, phaseLinker)
//				compiler.execute(FileSourceProvider(orbit.source))
//
//				val result = invocation.getResult<Parser.Result>("Parser")
//				val nameResolver = CanonicalNameResolver(invocation)
//
//				val json = Serialiser.serialise(result.ast)
//				println(json.toString(2))
//
//				val environment = invocation.getResult<Environment>("CanonicalNameResolver")
//
////				println(invocation.dumpWarnings())
////				println(invocation.dumpErrors())
//
////				println(environment)
//
//				val printer = Printer(Unix)
//				val correctnessResults = invocation.getResult(correctness)
//
////				Analysis.collate(correctnessResults, printer)
//
//				val context = Context()
//
//				// TODO - Remove these bootstrap types
//				context.bind("Test::Int+Test::Int", Lambda(Entity("Test::Int"), Entity("Test::Int")))
//
//				val typeResolver = TypeChecker(invocation, context)
//
//				typeResolver.execute(environment)

//				println(Serialiser.serialise(context).toString(2))

//				val vm = VM(listOf("Some string"), listOf(
//					Push(OrbInt(1)),
//					Push(OrbInt(2)),
//					Mark("foo"),
//					Add<Int>()
//				))
//
//				vm.run()
//				println(vm.dump())
			} catch (ex: Exception) {
				println(ex.message)
//				throw ex
			}
        }
    }
}