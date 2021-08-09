package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.Node
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

class ModuleTypeResolver(override val node: ModuleNode, override val binding: Binding) : TypeResolver<ModuleNode, Module>,
    KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    constructor(pair: Pair<ModuleNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context): Module {
        val path = node.getPath()

        val typeAliases = node.typeAliasNodes
            .map {
                val targetType = TypeExpressionTypeResolver(it.targetTypeIdentifier, binding)
                    .resolve(environment, context)
                    as? Type
                    ?: throw MissingTypeException(it.targetTypeIdentifier.value)

                if (targetType.isRequired) {
                    val code = printer.apply("type ${it.sourceTypeIdentifier.value} = ${targetType.name}", PrintableKey.Italics)
                    throw invocation.make<TypeChecker>("Right-hand side of a type alias cannot be a required type:\n\t\t$code", it)
                }

                TypeAlias(it.sourceTypeIdentifier.value, targetType)
            }

        return Module(path.toString(OrbitMangler), typeAliases).also {
            node.annotate(it, Annotations.Type)
        }
    }
}