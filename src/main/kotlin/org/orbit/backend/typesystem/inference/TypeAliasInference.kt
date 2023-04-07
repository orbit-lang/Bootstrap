package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeAliasNode
import org.orbit.util.Invocation

object TypeAliasInference : ITypeInference<TypeAliasNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeAliasNode, env: IMutableTypeEnvironment): AnyType {
        val path = node.getPath()
        val dummy = IType.Alias(Path.self, IType.Unit)
        val nEnv = env.fork()

        nEnv.add(dummy)

        val type = TypeInferenceUtils.infer(node.targetType, nEnv)
        val alias = IType.Alias(path, type)
        val pAlias = env.aliasGuard(path.toString(OrbitMangler))

        if (pAlias != null) {
            throw invocation.make<TypeSystem>("Alias `${node.sourceTypeIdentifier}` is already defined as:\n\t${pAlias}", node.sourceTypeIdentifier)
        }

        env.add(alias)

        GlobalEnvironment.tag(type, path.toString(OrbitMangler))

        return type
    }
}