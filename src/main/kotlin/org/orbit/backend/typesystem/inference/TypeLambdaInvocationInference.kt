package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.TypeLambdaInvocationNode
import org.orbit.util.Invocation
import java.lang.Integer.max

object TypeLambdaInvocationInference : ITypeInference<TypeLambdaInvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private fun checkDependentType(expected: AnyType, actual: AnyType, env: ITypeEnvironment) : Boolean {
        if (actual !is IValue<*, *>) return false

        return TypeUtils.checkEq(env, actual.type, expected)
    }

    override fun infer(node: TypeLambdaInvocationNode, env: IMutableTypeEnvironment): AnyType {
        val pArrow = TypeInferenceUtils.infer(node.typeIdentifierNode, env)

        val arrow = when (val fArrow = pArrow.flatten(pArrow, env)) {
            is ConstrainedArrow -> fArrow
            else -> {
                if (fArrow is Attribute) {
                    throw invocation.compilerError<TypeSystem>("Attempting to invoke Attribute $fArrow where Type Lambda expected. Attribute invocation only happens in Type Lambda `where` clauses", node)
                }

                throw invocation.make<TypeSystem>("Cannot invoke Type $pArrow", node.typeIdentifierNode)
            }
        }

        val args = TypeInferenceUtils.inferAll(node.arguments, env)
        val domain = arrow.getDomain()

        val variadic = when (val tv = domain.lastOrNull() as? TypeVar) {
            null -> null
            else -> when (tv.isVariadic) {
                true -> tv
                else -> null
            }
        }

        val validRange = when (val v = variadic?.variadicBound) {
            null -> IntRange(domain.count(), domain.count())
            else -> {
                val minCount = max(0, domain.count() - 1)
                val maxCount = minCount + v.maxSize()

                IntRange(minCount, maxCount)
            }
        }

        if (!validRange.contains(args.count())) {
            if (variadic == null) {
                throw invocation.make<TypeSystem>("Invocation of Type Lambda $arrow expects ${arrow.getDomain().count()} Type arguments, found ${args.count()}", node)
            } else {
                throw invocation.make<TypeSystem>("Invocation of Variadic Type Lambda $arrow expects ${arrow.getDomain().count()} Type arguments, found ${args.count()}", node)
            }
        }

        for ((idx, tv) in domain.withIndex()) {
            if (tv !is TypeVar) continue
            if (!tv.isDependent) continue

            val arg = args[idx]
            val dt = tv.dependentType ?: TODO("MISSING DEPENDENT TYPE")

            if (!checkDependentType(dt, arg, env)) {
                throw invocation.make<TypeSystem>("Type Lambda requires parameter at index $idx to be a constant value of Type $dt, found $arg", node)
            }
        }

        val nArrow = when (variadic) {
            null -> arrow
            else -> {
                val maxVIdx = arrow.referencedVariadicIndices.maxOrNull()
                    ?: throw invocation.make<TypeSystem>("Variadic Type Parameter $variadic is not consumed (sliced) in body of Type Lambda", node)

                val nonVCount = arrow.getDomain().count { !(it is TypeVar && it.isVariadic) }
                val vCount = args.count() - nonVCount

                if (vCount <= maxVIdx) {
                    val referencedSlices = arrow.referencedVariadicIndices.map { VariadicSlice(variadic, it) }
                        .joinToString(", ")

                    throw invocation.make<TypeSystem>("Variadic Type Lambda requires at least ${nonVCount + maxVIdx + 1} arguments because the following slices are consumed in the body:\n\t$referencedSlices", node)
                }

                val vArgs = args.drop(nonVCount)
                val vSubs = arrow.referencedVariadicIndices.map {
                    Substitution(VariadicSlice(variadic, it), vArgs[it])
                }

                vSubs.fold(arrow) { acc, next -> acc.substitute(next) as ConstrainedArrow }
            }
        }

        val mArrow = nArrow.getDomain().zip(args).fold(nArrow) {
            acc, next -> acc.substitute(Substitution(next.first, next.second)) as ConstrainedArrow
        }

        mArrow.constraints.forEach {
            try {
                it.invoke(LocalEnvironment(env))
            } catch (e: Exception) {
                return mArrow.fallback ?: throw e
            }
        }

        return mArrow.getCodomain()
    }
}