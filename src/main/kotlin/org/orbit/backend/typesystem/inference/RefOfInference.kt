package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.core.nodes.RefOfNode

object RefOfInference : ITypeInference<RefOfNode, ITypeEnvironment> {
    override fun infer(node: RefOfNode, env: ITypeEnvironment): AnyType {
        val refs = env.getAllBindings(node.ref.identifier)
            ?: return IType.Always

        for (ref in refs) {
            println("Ref $ref")
        }

        return IType.Always
    }
}