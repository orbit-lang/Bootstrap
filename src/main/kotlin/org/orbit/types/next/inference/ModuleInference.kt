package org.orbit.types.next.inference

import org.orbit.core.getPath
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.Module
import org.orbit.types.next.components.Type

object ModuleInference : Inference<ModuleNode, Module> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ModuleNode): InferenceResult {
        val typeDefs = node.search(TypeDefNode::class.java)
        val types = typeDefs.map { inferenceUtil.inferAs<TypeDefNode, Type>(it) }

        // TODO - Imports (`with` statements)
        return Module(node.getPath())
            .extendAll(types)
            .inferenceResult()
    }
}