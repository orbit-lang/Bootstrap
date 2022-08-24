package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.AnyExpr
import org.orbit.precess.backend.utils.AnyType

sealed interface Decl {
    data class Clone(val cloneElements: Boolean = true, val cloneRefs: Boolean = true) : Decl {
        override fun exists(env: Env): Boolean = false
        override fun xtend(env: Env): Env = when (cloneElements) {
            true -> when (cloneRefs) {
                true -> Env(env.elements, env.refs, env.contracts, env.projections, env.expressionCache)
                else -> Env(env.elements, emptyList(), env.contracts, env.projections, env.expressionCache)
            }

            else -> when (cloneRefs) {
                true -> Env(emptyList(), env.refs, env.contracts, env.projections, env.expressionCache)
                else -> Env(emptyList(), emptyList(), env.contracts, env.projections, env.expressionCache)
            }
        }
    }

    data class Merge(val root: Env) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env {
            val nElements = (root.elements + env.elements).distinctBy { it.id }
            val nRefs = (root.refs + env.refs).distinctBy { it.uniqueId }
            val nContracts = (root.contracts + env.contracts)
            val nProjections = (root.projections + env.projections)
            val nExpressionCache = (root.expressionCache + env.expressionCache)

            return Env(nElements, nRefs, nContracts, nProjections, nExpressionCache)
        }
    }

    data class DenyElement(private val id: String) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env = env.denyElement(id)
    }

    data class DenyRef(private val name: String) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env = env.denyRef(name)
    }

    data class Type(val type: IType.Type, val members: Map<String, IType.Entity<*>>) : Decl {
        override fun exists(env: Env): Boolean = env.elements.any { it.id == type.id }
        override fun xtend(env: Env): Env {
            val nMembers = members.map { IType.Member(it.key, it.value, type) }

            return Env(
                env.elements + type + nMembers,
                env.refs,
                env.contracts,
                env.projections,
                env.expressionCache
            )
        }
    }

    data class Assignment(val name: String, val expr: Expr<*>) : Decl {
        override fun exists(env: Env): Boolean = env.refs.any { it.name == name }
        override fun xtend(env: Env): Env {
            val type = expr.infer(env)

            return Env(env.elements, env.refs + Ref(name, type), env.contracts, env.projections, env.expressionCache)
        }
    }

    data class Alias(val name: String, val ref: IRef) : Decl {
        override fun exists(env: Env): Boolean = env.refs.any { it.name == name }
        override fun xtend(env: Env): Env {
            val alias = org.orbit.precess.backend.components.Alias(name, ref)

            return Env(env.elements, env.refs + alias, env.contracts, env.projections, env.expressionCache)
        }
    }

    data class Extension(val typeName: String, val members: Map<String, IType.Entity<*>>) : Decl {
        constructor(type: IType.Type, members: List<IType.Member>) : this(
            type.id,
            members.map { it.name to it.type }.toMap()
        )

        override fun exists(env: Env): Boolean = env.elements.containsAll(members.values)
        override fun xtend(env: Env): Env {
            val type = env.getElementAs<IType.Type>(typeName) ?: return env
            val nMembers = members.map { IType.Member(it.key, it.value, type) }

            return Env(env.elements + nMembers, env.refs, env.contracts, env.projections, env.expressionCache)
        }
    }

    data class Projection(val source: IType.Type, val target: IType.ITrait) : Decl {
        override fun exists(env: Env): Boolean = true

        override fun xtend(env: Env): Env = Env(
            env.elements,
            env.refs,
            env.contracts,
            env.projections + org.orbit.precess.backend.components.Projection(source, target),
            env.expressionCache
        )
    }

    data class Cache(val expr: AnyExpr, val type: AnyType) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env = Env(
            env.elements,
            env.refs,
            env.contracts,
            env.projections,
            env.expressionCache + (expr.toString() to type)
        )
    }

    data class Compound<D: Decl, E: Decl>(val a: D, val b: E) : Decl {
        override fun exists(env: Env): Boolean = a.exists(env) && b.exists(env)
        override fun xtend(env: Env): Env = env.extend(a) + env.extend(b)
    }

    fun exists(env: Env): Boolean
    fun xtend(env: Env): Env

    fun extend(env: Env): Env = Env.capture { xtend(env) }
}

operator fun <D: Decl, E: Decl> D.plus(other: E) : Decl.Compound<D, E>
    = Decl.Compound(this, other)

operator fun List<Decl>.unaryPlus() : Decl
    = reduce { acc, next -> Decl.Compound(acc, next) }