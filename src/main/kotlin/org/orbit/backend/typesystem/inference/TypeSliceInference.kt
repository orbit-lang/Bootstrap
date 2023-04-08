package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.core.nodes.TypeSliceNode
import org.orbit.util.Invocation

object TypeSliceInference : ITypeInference<TypeSliceNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeSliceNode, env: ITypeEnvironment): AnyType {
        val tv = TypeInferenceUtils.infer(node.identifier, env)
        val typeVar = tv as? IType.TypeVar
            ?: throw invocation.make<TypeSystem>("Cannot slice into non-Variadic Type $tv", node.identifier)

        val variadicBound = typeVar.variadicBound
            ?: throw invocation.make<TypeSystem>("Cannot slice into non-Variadic Type Variable $typeVar", node.identifier)

        if (!variadicBound.isSatisfied(node.index)) {
            throw invocation.make<TypeSystem>("Cannot slice into index ${node.index} of Variadic Type Variable $typeVar", node.identifier)
        }

        return IType.VariadicSlice(typeVar, node.index)
    }
}