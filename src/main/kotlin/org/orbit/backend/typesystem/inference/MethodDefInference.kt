package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.AnyArrow
import org.orbit.precess.backend.utils.TypeUtils
import org.orbit.util.Invocation

object MethodDefInference : ITypeInference<MethodDefNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodDefNode, env: Env): IType<*> {
        val signature = TypeSystemUtils.inferAs<MethodSignatureNode, AnyArrow>(node.signature, env)

        env.extendInPlace(Decl.TypeAlias(node.signature.identifierNode.identifier, Expr.AnyTypeLiteral(signature)))

        val nEnv = env.extend(Decl.Clone())
            .withSelf(signature)

        val pDecl = node.signature.getAllParameterPairs()
            .map { Decl.Assignment(it.identifierNode.identifier, TypeSystemUtils.infer(it.typeExpressionNode, nEnv)) }
            .reduce(Decl::plus)

        nEnv.extendInPlace(pDecl)

        val returnType = TypeSystemUtils.infer(node.body, nEnv)

        return when (TypeUtils.checkEq(nEnv, returnType, signature.getCodomain())) {
            true -> signature
            else -> throw invocation.make<TypeSystem>("Method `${node.signature.identifierNode.identifier}` declared return Type of `${signature.getCodomain().id}`, found `${returnType.id}`", node)
        }
    }
}