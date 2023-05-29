package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.StringValue
import org.orbit.core.nodes.StringLiteralNode

object StringLiteralInference : ITypeInference<StringLiteralNode, ITypeEnvironment> {
    override fun infer(node: StringLiteralNode, env: ITypeEnvironment): AnyType
        = StringValue(node.text.count() to node.text)
}