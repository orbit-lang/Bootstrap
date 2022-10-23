package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.core.nodes.PanicNode

object PanicInference : ITypeInference<PanicNode, ITypeEnvironment> {
    override fun infer(node: PanicNode, env: ITypeEnvironment): AnyType
        // TODO - Infer type of node.expr and check that it conforms to `Error`
        = IType.Never("Panic @ ${node.firstToken.position}")
}