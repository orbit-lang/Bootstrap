package org.orbit.precess.backend.components

sealed interface Contract : IContextualComponent {
    sealed interface ContractResult {
        data class Verified(val env: Env) : ContractResult
        data class Violated(val reason: IType.Never) : ContractResult
    }

    sealed interface Invariant : Contract {
        object Intrinsics {
            data class MaxUse(private val ref: Ref, private val limit: Int) : Invariant {
                override fun verify(env: Env): ContractResult =
                    when (val count = ref.getHistoryInstances<RefEntry.Use>().count()) {
                        limit + 1 -> ContractResult.Violated(IType.Never("Contract Violation: ${ref.name} is constrained by a MaxUse invariant in the current context where limit = $limit, uses = $count"))
                        else -> ContractResult.Verified(env)
                    }
            }
        }
    }

//    sealed interface Implements<T : IType.ITrait> : Contract {
//        data class Membership(val type: IType.Type, val trait: IType.ITrait.MembershipTrait) : Implements<IType.ITrait.MembershipTrait> {
//            override fun verify(env: Env): ContractResult {
//                val members = env.getDeclaredMembers(type) + env.getProjectedMembers(type)
//
//                if (members.count() < trait.requiredMembers.count()) {
//                    return ContractResult.Violated(IType.Never("Type ${type.id} does not conform to Trait ${trait.id}"))
//                }
//
//                for (requiredMember in trait.requiredMembers) {
//                    if (!members.contains(requiredMember)) {
//                        return ContractResult.Violated(IType.Never("Type ${type.id} does not conform to Trait ${trait.id}. Missing member: ${requiredMember.id}"))
//                    }
//                }
//
//                return ContractResult.Verified(env.extend(Decl.Projection(type, trait)))
//            }
//        }
//
//        data class Behaviour(val type: IType.Type, val trait: IType.ITrait.SignatureTrait) : Implements<IType.ITrait.SignatureTrait> {
//            override fun verify(env: Env): ContractResult {
//                val specialised = trait.requiredSignatures.map { it.substitute(Substitution(trait, type)) }
//
//                return ContractResult.Verified(env)
//            }
//        }
//    }

    fun verify(env: Env): ContractResult
}