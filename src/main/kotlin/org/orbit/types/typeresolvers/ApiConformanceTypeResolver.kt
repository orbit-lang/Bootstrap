package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.injectResult
import org.orbit.core.nodes.ModuleNode
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.Api
import org.orbit.types.components.Context
import org.orbit.types.components.Module
import org.orbit.types.phase.TypeInitialisation
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

class ApiConformanceTypeResolver(override val node: ModuleNode, override val binding: Binding) : TypeResolver<ModuleNode, Api>,
    KoinComponent {
    override val invocation: Invocation by inject()
    private val parserResult: Parser.Result by injectResult(CompilationSchemeEntry.parser)
    private val printer: Printer by inject()

    constructor(pair: Pair<ModuleNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Api {
        val path = node.getPath()
        val module = node.getType() as? Module
            ?: TODO("ApiConformanceTypeResolver:32")

        environment.withScope(node) {
            if (node.implements.isEmpty()) {
                return@withScope Api(path.toString(OrbitMangler), emptyList())
            }

            for (traitIdentifier in node.implements) {
                val binding = environment.getBinding(traitIdentifier.value, Binding.Kind.Api)
                    .unwrap(this, traitIdentifier.firstToken.position)

                val api = context.getTypeByPath(binding.path)
                    as? Api
                    ?: TODO("HERE")

                for (requiredType in api.requiredTypes) {
                    // TODO - TraitConformance if requiredType declares conformance(s)
                    val requiredPath = OrbitMangler.unmangle(requiredType.name)
                    val matches = module.typeAliases
                        .filter {
                            val sourcePath = OrbitMangler.unmangle(it.name)

                            sourcePath.relativeNames.last() == requiredPath.relativeNames.last()
                        }

                    if (matches.count() == 1) {
                        continue
                    }

                    val clause = printer.apply("required type ${requiredPath.relativeNames.last()}", PrintableKey.Italics)

                    if (matches.isEmpty()) {
                        throw invocation.make<TypeInitialisation>("Module '${module.name}' declares conformance to Api '${api.name}', but does not satisfy the following clause:\n\t\t$clause", node)
                    } else if (matches.count() > 1) {
                        throw invocation.make<TypeInitialisation>("Module '${module.name}' duplicate type aliases for the single clause:\n\t\t$clause", node)
                    }
                }
            }
        }

        return Api(path.toString(OrbitMangler), emptyList())
    }
}