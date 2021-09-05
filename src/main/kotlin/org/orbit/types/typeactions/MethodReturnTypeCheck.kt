package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.injectResult
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.ReturnStatementNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.extensions.annotate
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.Context
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.types.components.TypeSignature
import org.orbit.types.typeresolvers.MethodBodyTypeResolver
import org.orbit.util.Printer

class MethodReturnTypeCheck(private val node: MethodDefNode) : TypeAction, KoinComponent {
    private companion object : KoinComponent {
        private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)
    }

    private lateinit var signature: TypeSignature

    override fun execute(context: Context) = context.withSubContext { ctx ->
        signature = node.signature.getType() as TypeSignature
        signature.parameters
            .forEach { ctx.bind(it.name, it.type) }

        val resolver = MethodBodyTypeResolver(node.body, Binding(Binding.Kind.Method, signature.name, node.signature.getPath()), signature.returnType)

        resolver.resolve(nameResolverResult.environment, ctx)

        return@withSubContext

//        val returnTypeAnnotation = signature.returnType
//        val returnStatements = node.search(ReturnStatementNode::class.java)
//
//        returnStatements.forEach {
//            val rType = TypeInferenceUtil.infer(ctx, it.valueNode, returnTypeAnnotation)
//
//            it.annotate(rType, Annotations.Type)
//            it.valueNode.annotate(rType, Annotations.Type)
//        }
    }

    override fun describe(printer: Printer): String {
        return "Checking return types for method ${signature.toString(printer)}"
    }
}