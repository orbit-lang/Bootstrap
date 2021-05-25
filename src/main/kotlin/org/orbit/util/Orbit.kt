package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.ProgramUnitFactory
import org.orbit.backend.codegen.swift.units.ProgramUnit
import org.orbit.backend.phase.CodeWriter
import org.orbit.backend.phase.MainResolver
import org.orbit.core.*
import org.orbit.core.components.CompilationEventBus
import org.orbit.core.components.CompilationScheme
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.core.phase.CompilerGenerator
import org.orbit.frontend.MultiFileSourceProvider
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.ObserverPhase
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ProgramRule
import org.orbit.graph.pathresolvers.*
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.types.components.InstanceSignature
import org.orbit.types.components.Parameter
import org.orbit.types.components.TypeProtocol
import org.orbit.types.components.TypeSignature
import org.orbit.types.phase.TypeChecker
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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
		util.registerPathResolver(CallPathResolver(), CallNode::class.java)
		util.registerPathResolver(TypeIdentifierPathResolver(), TypeIdentifierNode::class.java)
		util.registerPathResolver(ExpressionPathResolver(), ExpressionNode::class.java)
		util.registerPathResolver(RValuePathResolver(), RValueNode::class.java)
		util.registerPathResolver(SymbolLiteralPathResolver, SymbolLiteralNode::class.java)
		util.registerPathResolver(IntLiteralPathResolver, IntLiteralNode::class.java)
		util.registerPathResolver(BinaryExpressionResolver(), BinaryExpressionNode::class.java)
		util.registerPathResolver(UnaryExpressionResolver(), UnaryExpressionNode::class.java)
		util.registerPathResolver(IdentifierExpressionPathResolver(), IdentifierNode::class.java)
		util.registerPathResolver(PrintPathResolver(), PrintNode::class.java)

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

			override fun mangle(signature: InstanceSignature): String {
				val mang = (OrbitMangler + this)
				val receiver = mang(signature.receiver.type.name)
				val params = signature.parameters.map(Parameter::type)
					.map(TypeProtocol::name).joinToString("_", transform = mang)

				val ret = mang(signature.returnType.name)

				return "${receiver}_${signature.name}_${params}_$ret"
			}

			override fun mangle(signature: TypeSignature): String {
				val mang = (OrbitMangler + this)
				val receiver = mang(signature.receiver.name)
				val ret = mang(signature.returnType.name)


				val params = when (signature.parameters.isEmpty()) {
					true -> ""
					else -> "_" + signature.parameters.map(Parameter::type)
						.map(TypeProtocol::name).joinToString("_", transform = mang)
				}


				return "${receiver}_${signature.name}${params}_$ret"
			}
		}
	}
}

class Build : CliktCommand(), KoinComponent {
	private val invocation: Invocation by inject()
	private val compilerGenerator: CompilerGenerator by inject()
	private val compilationEventBus: CompilationEventBus by inject()

	val sources by argument(help = "Orbit source file to compile")
		.file()
		.multiple(true)

	@ExperimentalTime
	override fun run() {
		println("Compilation completed in " + measureTime {
			try {
				startKoin { modules(mainModule) }

				// TODO - Platform should be derived from System.getProperty("os.name") or similar
				// TODO - Support non *nix platforms
				val sourceReader = MultiFileSourceProvider(sources)
				val dummyPhase = DummyPhase(invocation, sourceReader)

				// The first phase (CommentParser) needs an input, so we "cheat" here by inserting initial conditions
				invocation.storeResult("__source__", sourceReader)

				compilerGenerator["__source__"] = dummyPhase
				compilerGenerator[CompilationSchemeEntry.commentParser] = CommentParser(invocation)
				compilerGenerator[CompilationSchemeEntry.lexer] = Lexer(invocation)
				compilerGenerator[CompilationSchemeEntry.parser] = Parser(invocation, ProgramRule)
				compilerGenerator[CompilationSchemeEntry.observers] = ObserverPhase(invocation)
				compilerGenerator[CompilationSchemeEntry.canonicalNameResolver] = CanonicalNameResolver(invocation)
				compilerGenerator[CompilationSchemeEntry.typeChecker] = TypeChecker(invocation)
				compilerGenerator[CompilationSchemeEntry.mainResolver] = MainResolver

				compilationEventBus.events.registerObserver {
					val printer = Printer(invocation.platform.getPrintableFactory())
					val eventName = printer.apply(it.identifier, PrintableKey.Bold, PrintableKey.Underlined)

					//println("Compiler event: $eventName")
				}

				compilerGenerator.run(CompilationScheme)

				val parserResult = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser)
//				val html = (parserResult.ast as ProgramNode).write(HtmlNodeWriterFactory, 0)
//
//				val fileReader = FileReader("output.css")
//				val css = fileReader.readText()
//
//				fileReader.close()
//
//				val fileWriter = FileWriter("output.html")
//
//				fileWriter.write("<html><head>$css</head><body>${html}</body></html>")
//				fileWriter.close()

				println(CodeWriter.execute(parserResult.ast as ProgramNode))

				println(invocation.dumpWarnings())
			} catch (ex: Exception) {
				println(ex.message)
			}
		})
	}
}

class Orbit : CliktCommand() {
	override fun run() {}
}