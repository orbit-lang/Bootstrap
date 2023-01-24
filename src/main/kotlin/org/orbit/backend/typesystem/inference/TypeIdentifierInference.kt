package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.getTypeOrNull
import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.util.Invocation
import java.lang.Exception

object TypeIdentifierInference : ITypeInference<TypeIdentifierNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeIdentifierNode, env: ITypeEnvironment): AnyType {
        if (node.isDiscard) return IType.Always

        val path = try { node.getPath() } catch (ex: Exception) {
            println(ex)
            throw ex
        }

        if (path == IType.Unit.getPath()) return IType.Unit

        return env.getTypeOrNull(path)?.component
            ?: throw invocation.make<TypeSystem>("Undefined Type `${path.toString(OrbitMangler)}`", node)
    }
}