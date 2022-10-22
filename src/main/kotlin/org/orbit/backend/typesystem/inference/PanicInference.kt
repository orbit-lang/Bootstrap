package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.core.nodes.PanicNode

object PanicInference : ITypeInferenceOLD<PanicNode> {
    override fun infer(node: PanicNode, env: Env): AnyType {
        // Infer type of node.expr and check that it conforms to `Error`
        return IType.Never("Panic @ ${node.firstToken.position}")
    }
}