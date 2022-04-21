package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.orbit.core.OrbitMangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.injectResult
import org.orbit.core.nodes.BlockNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.graph.components.Binding
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.Context
import org.orbit.types.components.TypeSignature
import org.orbit.types.typeresolvers.MethodBodyTypeResolver
import org.orbit.util.Printer

class SpecialisedMethodReturnTypeCheck(private val signature: TypeSignature, private val body: BlockNode) : TypeAction, KoinComponent {
    private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)

    override fun execute(context: Context) = context.withSubContext { ctx ->
        signature.parameters.forEach { ctx.bind(it.name, it.type) }
//        signature.typeParameters.forEach {
//            ctx.add(it.asSelfType())
//        }

        val path = OrbitMangler.unmangle(OrbitMangler.mangle(signature))
        val resolver = MethodBodyTypeResolver(signature, body, Binding(Binding.Kind.Method, signature.name, path), signature.returnType)

        resolver.resolve(nameResolverResult.environment, ctx)

        return@withSubContext
    }

    override fun describe(printer: Printer): String {
        return "Checking return type for method ${OrbitMangler.mangle(signature)}"
    }
}

class MethodReturnTypeCheck(private val node: MethodDefNode) : TypeAction, KoinComponent {
    private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)

    private lateinit var signature: TypeSignature

    override fun execute(context: Context) = context.withSubContext { ctx ->
        signature = node.signature.getType() as TypeSignature
        signature.parameters.forEach { ctx.bind(it.name, it.type) }

        val resolver = MethodBodyTypeResolver(signature, node.body, Binding(Binding.Kind.Method, signature.name, node.signature.getPath()), signature.returnType)

        resolver.resolve(nameResolverResult.environment, ctx)

        return@withSubContext
    }

    override fun describe(printer: Printer): String {
        return "Checking return type for method ${signature.toString(printer)}"
    }
}