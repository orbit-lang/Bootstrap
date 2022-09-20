package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object SignatureInference : ITypeInference<MethodSignatureNode> {
    override fun infer(node: MethodSignatureNode, env: Env): IType<*> {
        val params = TypeSystemUtils.inferAll(node.getAllParameters(), env)
        val ret = when (val r = node.returnTypeNode) {
            null -> IType.Unit
            else -> TypeSystemUtils.infer(r, env)
        }

        return when (params.count()) {
            0 -> IType.Arrow0(ret)
            1 -> IType.Arrow1(params[0], ret)
            2 -> IType.Arrow2(params[0], params[1], ret)
            3 -> IType.Arrow3(params[0], params[1], params[2], ret)
            else -> TODO("Arrow > 3")
        }
    }
}