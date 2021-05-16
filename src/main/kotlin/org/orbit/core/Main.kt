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
import org.orbit.backend.phase.MainResolver
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.phase.CodeWriter
import org.orbit.backend.codegen.ProgramUnitFactory
import org.orbit.backend.codegen.swift.units.ProgramUnit
import org.orbit.core.components.CompilationScheme
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.core.components.CompilationEventBus
import org.orbit.core.phase.CompilerGenerator
import org.orbit.frontend.*
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.ObserverPhase
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ProgramRule
import org.orbit.graph.pathresolvers.*
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.*
import org.orbit.util.nodewriters.html.HtmlNodeWriterFactory
import org.orbit.util.nodewriters.write
import java.io.FileReader
import java.io.FileWriter
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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
				val params = signature.parameters.map(Parameter::type)
					.map(TypeProtocol::name).joinToString("_", transform = mang)

				val ret = mang(signature.returnType.name)

				return "${receiver}_${signature.name}_${params}_$ret"
			}
		}
	}
}

class Main {
    companion object : KoinComponent {
		private val invocation: Invocation by inject()
		private val compilerGenerator: CompilerGenerator by inject()
		private val compilationEventBus: CompilationEventBus by inject()

        @ExperimentalTime
		@JvmStatic fun main(args: Array<String>) {
			println("Compilation completed in " + measureTime {
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
					compilerGenerator[CompilationSchemeEntry.mainResolver] = MainResolver

					compilationEventBus.events.registerObserver {
						val printer = Printer(invocation.platform.getPrintableFactory())
						val eventName = printer.apply(it.identifier, PrintableKey.Bold, PrintableKey.Underlined)

						//println("Compiler event: $eventName")
					}

					compilerGenerator.run(CompilationScheme.Intrinsics)

					val parserResult = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser)
					val html = (parserResult.ast as ProgramNode).write(HtmlNodeWriterFactory, 0)

					val fileReader = FileReader("output.css")
					val css = fileReader.readText()

					fileReader.close()

					val fileWriter = FileWriter("output.html")

					fileWriter.write("<html><head>$css</head><body>${html}</body></html>")
					fileWriter.close()

					println(CodeWriter.execute(parserResult.ast))

					println(invocation.dumpWarnings())
				} catch (ex: Exception) {
					println(ex.message)
				}
			})
        }
    }
}