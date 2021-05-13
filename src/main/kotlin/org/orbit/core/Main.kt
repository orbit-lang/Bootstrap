package org.orbit.core

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.definition.BeanDefinition
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.CodeWriter
import org.orbit.backend.codegen.ProgramUnitFactory
import org.orbit.backend.codegen.swift.units.ProgramUnit
import org.orbit.core.nodes.*
import org.orbit.frontend.*
import org.orbit.frontend.rules.ProgramRule
import org.orbit.graph.*
import org.orbit.types.Context
import org.orbit.types.TypeChecker
import org.orbit.util.*
import org.orbit.util.nodewriters.html.HtmlNodeWriterFactory
import org.orbit.util.nodewriters.write
import java.io.FileReader
import java.io.FileWriter

interface Qualified {
	fun toQualifier() : Qualifier
}

interface QualifiedEnum : Qualified {
	val name: String
	override fun toQualifier(): Qualifier = StringQualifier(name)
}

enum class CodeGeneratorQualifier : QualifiedEnum {
	Swift;
}

inline fun <reified T> Module.single(
	qualified: Qualified,
	noinline definition: Definition<T>
): BeanDefinition<T> {
	return single(qualified.toQualifier(), createdAtStart = false, false, definition)
}

inline fun <reified T : Any> KoinComponent.injectQualified(
	qualified: Qualified,
	mode: LazyThreadSafetyMode = KoinPlatformTools.defaultLazyMode(),
	noinline parameters: ParametersDefinition? = null
): Lazy<T> =
	lazy(mode) { get<T>(qualified.toQualifier(), parameters) }

inline fun <reified T> KoinComponent.injectResult(
	entry: CompilationSchemeEntry,
	mode: LazyThreadSafetyMode = KoinPlatformTools.defaultLazyMode()
) : Lazy<T> = lazy(mode) {
	val invocation = getKoin().get<Invocation>()

	return@lazy invocation.getResult(entry)
}

private val mainModule = module {
	single { Invocation(Unix) }
	single { CompilerGenerator(get()) }
	single { CompilationEventBus() }
	single { Printer(get<Invocation>().platform.getPrintableFactory()) }
	single {
		val util = PathResolverUtil()

		util.registerPathResolver(ContainerResolver(), ModuleNode::class.java)
		util.registerPathResolver(AssignmentPathResolver(), AssignmentStatementNode::class.java)
		util.registerPathResolver(MethodDefPathResolver(), MethodDefNode::class.java)
		util.registerPathResolver(MethodSignaturePathResolver(), MethodSignatureNode::class.java)
		util.registerPathResolver(BlockPathResolver(), BlockNode::class.java)
		util.registerPathResolver(PropertyPairPathResolver(), PairNode::class.java)
		util.registerPathResolver(ConstructorPathResolver(), ConstructorNode::class.java)
		util.registerPathResolver(InstanceMethodCallPathResolver(), InstanceMethodCallNode::class.java)
		util.registerPathResolver(TypeIdentifierPathResolver(), TypeIdentifierNode::class.java)
		util.registerPathResolver(ExpressionPathResolver(), ExpressionNode::class.java)
		util.registerPathResolver(RValuePathResolver(), RValueNode::class.java)
		util.registerPathResolver(SymbolLiteralPathResolver, SymbolLiteralNode::class.java)
		util.registerPathResolver(IntLiteralPathResolver, IntLiteralNode::class.java)
		util.registerPathResolver(BinaryExpressionResolver(), BinaryExpressionNode::class.java)
		util.registerPathResolver(IdentifierExpressionPathResolver(), IdentifierNode::class.java)

		util
	}

	single<ProgramUnitFactory>(CodeGeneratorQualifier.Swift) {
		object : ProgramUnitFactory {
			override fun getProgramUnit(input: ProgramNode): CodeUnit<ProgramNode> {
				return ProgramUnit(input)
			}
		}
	}

	single<Mangler>(CodeGeneratorQualifier.Swift) {
		object : Mangler {
			override fun mangle(path: Path): String {
				return path.relativeNames.joinToString("_")
			}

			override fun unmangle(name: String): Path {
				return Path(name.split("_"))
			}
		}
	}
}

class Main {
    companion object : KoinComponent {
		private val invocation: Invocation by inject()
		private val compilerGenerator: CompilerGenerator by inject()
		private val compilationEventBus: CompilationEventBus by inject()

        @JvmStatic fun main(args: Array<String>) {
        	try {
				startKoin { modules(mainModule) }

	            val orbit = Orbit()

	            orbit.main(args)

				// TODO - Platform should be derived from System.getProperty("os.name") or similar
				// TODO - Support non *nix platforms
				val sourceReader = MultiFileSourceProvider(orbit.source)
				val dummyPhase = DummyPhase(invocation, sourceReader)

				invocation.storeResult("__source__", sourceReader)

				compilerGenerator["__source__"] = dummyPhase
				compilerGenerator[CompilationSchemeEntry.commentParser] = CommentParser(invocation)
				compilerGenerator[CompilationSchemeEntry.lexer] = Lexer(invocation)
				compilerGenerator[CompilationSchemeEntry.parser] = Parser(invocation, ProgramRule)
				compilerGenerator[CompilationSchemeEntry.observers] = ObserverPhase(invocation)
				compilerGenerator[CompilationSchemeEntry.canonicalNameResolver] = CanonicalNameResolver(invocation)
				compilerGenerator[CompilationSchemeEntry.typeChecker] = TypeChecker(invocation)

				compilationEventBus.events.registerObserver {
					val printer = Printer(invocation.platform.getPrintableFactory())
					val eventName = printer.apply(it.identifier, PrintableKey.Bold, PrintableKey.Underlined)

					//println("Compiler event: $eventName")
				}

				compilerGenerator.run(CompilationScheme.Intrinsics)

				val contextResult = invocation.getResult<Context>(CompilationSchemeEntry.typeChecker)

				val parserResult = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser)
				val html = (parserResult.ast as ProgramNode).write(HtmlNodeWriterFactory, 0)

				val fileReader = FileReader("output.css")
				val css = fileReader.readText()

				fileReader.close()

				val fileWriter = FileWriter("output.html")

				fileWriter.write("<html><head>$css</head><body>${html}</body></html>")
				fileWriter.close()

				println(CodeWriter.execute(parserResult.ast))

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