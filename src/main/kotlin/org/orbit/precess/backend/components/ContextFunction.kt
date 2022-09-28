package org.orbit.precess.backend.components

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeUtils

interface ContextFunction {
    sealed interface Result {
        data class Success(val env: Env) : Result
        data class Failure(val reason: IType.Never) : Result
    }

    object Intrinsics {
        data class AssertEq<E : Expr<E>, T : IType.Entity<T>>(val expression: E, val type: T) : ContextFunction {
            override fun invoke(env: Env): Result = when (TypeUtils.check(env, expression, type)) {
                true -> Result.Success(env)
                else -> Result.Failure(
                    IType.Never(
                        "Expression $expression expected to be of Type ${type.id}, found ${
                            expression.infer(
                                env
                            ).id
                        }"
                    )
                )
            }
        }
    }

    sealed interface Predicate : Clause {
        data class Exists(val element: AnyType) : Predicate {
            override fun weaken(env: Env): Env = when (env.elements.contains(element)) {
                true -> env
                else -> env.extend(Decl.DenyElement(element.id))
            }
        }

//        data class Conforms(val type: IType.Type, val trait: IType.ITrait) : Clause {
//            private fun getContract(): Contract.Implements<*> = when (trait) {
//                is IType.ITrait.MembershipTrait -> Contract.Implements.Membership(type, trait)
//            }
//
//            override fun weaken(env: Env): Env {
//                val contract = getContract()
//
//                return when (contract.verify(env)) {
//                    is Contract.ContractResult.Verified -> env.extend(Decl.Projection(type, trait))
//                    is Contract.ContractResult.Violated -> IType.Never("Type ${type.id} does not conform to Trait ${trait.id}")
//                        .panic()
//                }
//            }
//        }

        data class Used(val name: String, val useCount: Int) : Predicate {
            override fun weaken(env: Env): Env {
                val ref = env.refs.firstOrNull { it.name == name } ?: return env
                val uses = ref.getHistory().filterIsInstance<RefEntry.Use>()

                return when (uses.count()) {
                    useCount -> env
                    else -> env.denyRef(name)
                }
            }
        }
    }

    fun invoke(env: Env): Result
}