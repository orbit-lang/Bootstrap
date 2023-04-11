package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.Path
import org.orbit.core.nodes.IAttributeExpressionNode
import org.orbit.core.nodes.TypeQueryExpressionNode
import org.orbit.util.Invocation

object TypeQueryInference : ITypeInference<TypeQueryExpressionNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeQueryExpressionNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = env.fork()
        val tv = IType.TypeVar(node.value)

        nEnv.add(tv)

        val attribute = TypeInferenceUtils.inferAs<IAttributeExpressionNode, IType.IAttributeExpression>(node.clause, nEnv)

        val results = mutableListOf<AnyType>()
        val allTypes = env.getAllTypes().filterNot { it.component is IType.Always || it.component.getPath() == Path.self }
        for (type in allTypes) {
            val sub = Substitution(tv, type.component)
            val nAttr = attribute.substitute(sub) as IType.IAttributeExpression

            if (nAttr.evaluate(env) is IType.Always) {
                results.add(type.component)
            }
        }

        // TODO - Query Filters, e.g. `query T where T : Foo by Any`
        // TODO - Fallbacks, e.g. `query T where T : Foo else Foo`
        if (results.isEmpty()) {
            throw invocation.make<TypeSystem>("Type Query $attribute returns empty result set", node)
        } else if (results.count() > 1) {
            throw invocation.make<TypeSystem>("Type Query $attribute returns more than one result", node)
        }

        return results[0].also { println(it) }
    }
}