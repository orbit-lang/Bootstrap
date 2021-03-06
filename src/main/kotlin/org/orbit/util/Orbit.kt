package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
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
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.components.Scope
import org.orbit.graph.components.ScopeIdentifier
import org.orbit.graph.pathresolvers.*
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import java.io.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

data class OrbitLibrary(val scopes: List<Scope>, val context: Context, val graph: Graph) : Serializable {
	companion object : FilenameFilter {
		fun fromInvocation(invocation: Invocation) : OrbitLibrary {
			val names = invocation.getResult<NameResolverResult>(CompilationSchemeEntry.canonicalNameResolver)
			val context = invocation.getResult<Context>(CompilationSchemeEntry.typeChecker)

			return OrbitLibrary(names.environment.scopes, context, names.graph)
		}

		fun fromPath(path: File) : OrbitLibrary {
			val fis = FileInputStream(path)
			val ois = ObjectInputStream(fis)

			return ois.use { ois ->
				ois.readObject() as OrbitLibrary
			}
		}

		override fun accept(dir: File?, name: String?): Boolean {
			return name?.endsWith(".orbl") ?: false
		}
	}

	fun write(path: File) {
		val fos = FileOutputStream(path)
		val oos = ObjectOutputStream(fos)

		oos.writeObject(this)

		oos.close()
	}
}

class ImportManager(private val libraries: List<OrbitLibrary>) {
	val allScopes = libraries.flatMap { it.scopes }
	val allGraphs = libraries.map { it.graph }
	val allBindings = libraries
		.flatMap { it.scopes }
		.flatMap { it.bindings }

	fun findSymbol(symbol: String) : Scope.BindingSearchResult {
		val matches = allBindings.filter { it.simpleName == symbol || it.path.toString(OrbitMangler) == symbol }

		return when (matches.count()) {
			0 -> Scope.BindingSearchResult.None(symbol)
			1 -> Scope.BindingSearchResult.Success(matches[0])
			else -> Scope.BindingSearchResult.Multiple(matches)
		}
	}

	fun findEnclosingScope(symbol: String) : ScopeIdentifier? {
		for (scope in allScopes) {
			val binding = scope.bindings.find { it.simpleName == symbol || it.path.toString(OrbitMangler) == symbol }
				?: continue

			return scope.identifier
		}

		return null
	}
}

private val mainModule = module {
	single { Invocation(Unix) }
	single { CompilerGenerator(get()) }
	single { CompilationEventBus() }
	single { Printer(get<Invocation>().platform.getPrintableFactory()) }
	single {
		val util = PathResolverUtil()

		util.registerPathResolver(ContainerResolver(), ModuleNode::class.java)
		util.registerPathResolver(ContainerResolver(), ApiDefNode::class.java)
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
		util.registerPathResolver(MetaTypePathResolver, MetaTypeNode::class.java)

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

class Symbols : CliktCommand(), KoinComponent {
	private val invocation: Invocation by inject()
	private val source by argument( help = "Path to .orbl library file")
		.file()

	override fun run() {
		if (!source.exists() || !source.isFile) {
			throw invocation.make("Path must be a .orbl file")
		}

		val library = OrbitLibrary.fromPath(source)
		val allBindings = library.scopes.flatMap(Scope::bindings)
			.joinToString("\n\t") { "${it.kind::class.java.simpleName} : ${it.path.toString(OrbitMangler)}" }

		val allTypes = library.context.types.joinToString("\n\t") { "${it::class.java.simpleName} : ${it.name}" }

		println("Library @ ${source.absolutePath} contains the following symbols:")

		println("Named bindings:\n\t${allBindings}")

		println("")
		println("Types:\n\t${allTypes}")
	}
}

class Build : CliktCommand(), KoinComponent {
	companion object {
		const val COMMAND_OPTION_LONG_MAX_CYCLES = "--max-cycles"
		const val COMMAND_OPTION_LONG_OUTPUT = "--output"
		const val COMMAND_OPTION_LONG_OUTPUT_PATH = "--output-path"
		const val COMMAND_OPTION_LONG_LIBRARY = "--library"
		const val COMMAND_OPTION_LONG_VERBOSE = "--verbose"

		const val COMMAND_OPTION_DEFAULT_MAX_CYCLES = 25
	}

	private val invocation: Invocation by inject()
	private val compilerGenerator: CompilerGenerator by inject()
	private val compilationEventBus: CompilationEventBus by inject()

	data class BuildConfig(val maxDepth: Int)

	private val sources by argument(help = "Orbit source file to compile")
		.file()
		.multiple(true)

	private val output by option("-o", COMMAND_OPTION_LONG_OUTPUT, help = "Custom name for library product (default value = MyLibrary). Must start with a capital letter.")
		.default("MyLibrary")

	private val outputPath by option("-p", COMMAND_OPTION_LONG_OUTPUT_PATH, help = "Path where library product will be written to. Defaults to current directory.")
		.file()
		.default(File("."))

	private val libraryPaths by option("-l", COMMAND_OPTION_LONG_LIBRARY, help = "Path(s) to directories containing Orbit library products")
		.file()
		.multiple()

	private val verbose by option("-v", COMMAND_OPTION_LONG_VERBOSE, help = "Print out detailed compiler events")
		.flag(default = false)

	private val maxDepth by option("-x", COMMAND_OPTION_LONG_MAX_CYCLES, help = "Set the maximum allowed recursive cycles when resolving dependency graph")
		.int()
		.default(COMMAND_OPTION_DEFAULT_MAX_CYCLES)

	@ExperimentalTime
	override fun run() {
		println("Compilation completed in " + measureTime {
			try {
				startKoin {
					modules(mainModule)
				}

				loadKoinModules(module {
					single { BuildConfig(maxDepth) }
				})

				if (!outputPath.isDirectory) {
					throw invocation.make("Specified output path '${outputPath.absolutePath}' is not a directory")
				}

				// Creates an Orbit Library Directory
				val completeLibraryOutputDirectoryPath = outputPath.toPath()
					.resolve(output)

				val completeLibraryOutputDirectory = completeLibraryOutputDirectoryPath.toFile()

				if (!completeLibraryOutputDirectory.exists()) {
					completeLibraryOutputDirectory.mkdirs()
				}

				val orbitLibraryPath = completeLibraryOutputDirectoryPath.resolve("$output.orbl")
				val swiftLibraryPath = completeLibraryOutputDirectoryPath.resolve("$output.swift")

				val importedLibs = mutableListOf<OrbitLibrary>()

				for (lpath in libraryPaths) {
					if (lpath.isDirectory) {
						val paths = lpath.listFiles(OrbitLibrary)
							?: continue

						paths.forEach { println("Importing Orbit library '${it}'...")}
						paths.map(OrbitLibrary.Companion::fromPath)
							.forEach(importedLibs::add)
					} else {
						throw invocation.make("Library path provided via --library-path option must be a directory")
					}
				}

				loadKoinModules(module {
					single { ImportManager(importedLibs) }
				})

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

					if (verbose) {
						println("Compiler event: $eventName")
					}
				}

				compilerGenerator.run(CompilationScheme)

				val parserResult = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser)

				val orbitLibraryFile = orbitLibraryPath.toFile()

				if (!orbitLibraryFile.exists()) {
					orbitLibraryFile.createNewFile()
				}

				val libraryExports = OrbitLibrary.fromInvocation(invocation)

				libraryExports.write(orbitLibraryFile)

				val swiftLibraryFile = swiftLibraryPath.toFile()

				if (!swiftLibraryFile.exists()) {
					swiftLibraryFile.createNewFile()
				}

				val swiftCode = CodeWriter.execute(parserResult.ast as ProgramNode)

				val fw = FileWriter(swiftLibraryFile)

				fw.write(swiftCode)

				fw.close()

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