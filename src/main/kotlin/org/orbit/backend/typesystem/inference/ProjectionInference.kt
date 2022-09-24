package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.MethodDelegateNode
import org.orbit.core.nodes.ProjectionNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object MethodDelegateInference : ITypeInference<MethodDelegateNode> {
    override fun infer(node: MethodDelegateNode, env: Env): IType<*> {
        val delegateType = TypeSystemUtils.infer(node.delegate, env)

        return delegateType
    }
}

object ProjectionInference : ITypeInference<ProjectionNode> {
    override fun infer(node: ProjectionNode, env: Env): IType<*> {
        val projectedType = TypeSystemUtils.infer(node.typeIdentifier, env)
        val projectedTrait = TypeSystemUtils.inferAs<TypeExpressionNode, IType.Trait>(node.traitIdentifier, env)

        val bodyTypes = TypeSystemUtils.inferAll(node.body, env)

        val projection = Decl.Projection(projectedType, projectedTrait)

        env.extendInPlace(projection)

        return projectedType
    }
}