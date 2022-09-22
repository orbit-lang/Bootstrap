package org.orbit.precess.backend.components

import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyExpr
import org.orbit.precess.backend.utils.AnyType
import org.orbit.util.Invocation
import org.orbit.util.getKoinInstance

sealed interface Decl : IPrecessComponent {
    enum class ConflictStrategy {
        Reject, Replace, Ignore
    }

    data class Clone(val name: String? = null, val cloneElements: Boolean = true, val cloneRefs: Boolean = true) : Decl {
        override fun exists(env: Env): Boolean = false
        override fun xtend(env: Env): Env = when (cloneElements) {
            true -> when (cloneRefs) {
                true -> Env(name ?: env.name, env.elements, env.refs, env.contracts, env.projections, env.expressionCache, env)
                else -> Env(name ?: env.name, env.elements, emptyList(), env.contracts, env.projections, env.expressionCache, env)
            }

            else -> when (cloneRefs) {
                true -> Env(name ?: env.name, emptyList(), env.refs, env.contracts, env.projections, env.expressionCache, env)
                else -> Env(name ?: env.name, emptyList(), emptyList(), env.contracts, env.projections, env.expressionCache, env)
            }
        }

        override fun reduce(env: Env): Env = env
    }

    data class Merge(val root: Env) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env {
            val nElements = (root.elements + env.elements).distinctBy { it.id }
            val nRefs = (root.refs + env.refs).distinctBy { it.uniqueId }
            val nContracts = (root.contracts + env.contracts)
            val nProjections = (root.projections + env.projections)
            val nExpressionCache = (root.expressionCache + env.expressionCache)

            return Env(env.name, nElements, nRefs, nContracts, nProjections, nExpressionCache)
        }

        override fun reduce(env: Env): Env = env
    }

    data class DenyElement(private val id: String) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env = env.denyElement(id)

        override fun reduce(env: Env): Env = env
    }

    data class DenyRef(private val name: String) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env = env.denyRef(name)

        override fun reduce(env: Env): Env = env
    }

    data class Context(val context: Env) : Decl {
        override fun exists(env: Env): Boolean = env.elements.any { it.id == context.id }
        override fun xtend(env: Env): Env
            = Env(env.name, env.elements + context, env.refs, env.contracts, env.projections, env.expressionCache)

        override fun reduce(env: Env): Env
            = Env(env.name, env.elements - context, env.refs, env.contracts, env.projections, env.expressionCache)
    }

    data class Operator(val op: IType.IOperatorArrow<*, *>) : Decl {
        override fun exists(env: Env): Boolean = env.elements.filterIsInstance<IType.IOperatorArrow<*, *>>().any { it == op }
        override fun xtend(env: Env): Env
            = Env(env.name,env.elements + IType.Alias(op.identifier, op), env.refs, env.contracts, env.projections, env.expressionCache)

        override fun reduce(env: Env): Env
            = Env(env.name, env.elements - IType.Alias(op.identifier, op), env.refs, env.contracts, env.projections, env.expressionCache)
    }

    data class Type(val type: IType.Type, val members: List<IType.Member> = emptyList()) : Decl {
        constructor(type: IType.Type, members: Map<String, IType.Entity<*>> = emptyMap()) : this(type, members.map { IType.Member(it.key, it.value, type) })

        override fun exists(env: Env): Boolean = env.elements.any { it.id == type.id }
        override fun xtend(env: Env): Env {
            val refs = members.map { Ref(it.id, it.type) }
            return Env(env.name, env.elements + type, env.refs + refs, env.contracts, env.projections, env.expressionCache)
        }

        override fun reduce(env: Env): Env {
            val mems = env.getDeclaredMembers(type)

            return Env(env.name, env.elements - mems - type, env.refs, env.contracts, env.projections, env.expressionCache)
        }
    }

    data class Assignment(val name: String, val type: AnyType) : Decl {
        override fun exists(env: Env): Boolean = env.refs.any { it.name == name }
        override fun xtend(env: Env): Env {
            if (env.getRef(name) != null) {
                val invocation = getKoinInstance<Invocation>()

                throw invocation.make<Interpreter>("`$name` is already bound in the current context: `$env`", SourcePosition.unknown)
            }

            return Env(env.name, env.elements, env.refs + Ref(name, type), env.contracts, env.projections, env.expressionCache)
        }

        override fun reduce(env: Env): Env
            = Env(env.name, env.elements, env.refs - Ref(name, type), env.contracts, env.projections, env.expressionCache)
    }

    data class TypeAlias(val name: String, val expr: AnyExpr, val conflictStrategy: ConflictStrategy = ConflictStrategy.Reject) : Decl {
        override fun exists(env: Env): Boolean = env.elements.any { it.getCanonicalName() == name }
        override fun xtend(env: Env): Env {
            val type = expr.infer(env)
            val isAlreadyDefined = when (conflictStrategy) {
                ConflictStrategy.Ignore -> false
                else -> env.elements.any { it is IType.Alias && it.name == name && it.type.id == type.id }
            }

            val nElements = if (isAlreadyDefined) {
                if (conflictStrategy == ConflictStrategy.Reject) {
                    val invocation = getKoinInstance<Invocation>()

                    // TODO - There's probably a better way to catch naming conflicts, maybe in CanonicalNameResolver?
                    throw invocation.make<TypeSystem>("Attempt to redeclare `$name : ${type.id}`", Token.empty)
                } else if (conflictStrategy == ConflictStrategy.Replace) {
                    env.elements.filterNot { it is IType.Alias && it.name == name }
                } else {
                    env.elements
                }
            } else {
                env.elements
            }

            return Env(env.name, nElements + IType.Alias(name, type), env.refs, env.contracts, env.projections, env.expressionCache)
        }

        override fun reduce(env: Env): Env {
            val type = expr.infer(env)
            val nElements = env.elements.filterNot { it is IType.Alias && it.name == name }

            return Env(env.name, nElements, env.refs, env.contracts, env.projections, env.expressionCache)
        }
    }

    data class Alias(val name: String, val ref: IRef) : Decl {
        override fun exists(env: Env): Boolean = env.refs.any { it.name == name }
        override fun xtend(env: Env): Env {
            val alias = org.orbit.precess.backend.components.Alias(name, ref)

            return Env(env.name, env.elements, env.refs + alias, env.contracts, env.projections, env.expressionCache)
        }

        override fun reduce(env: Env): Env {
            val alias = org.orbit.precess.backend.components.Alias(name, ref)

            return Env(env.name, env.elements, env.refs - alias, env.contracts, env.projections, env.expressionCache)
        }
    }

    data class Extension(val typeName: String, val members: Map<String, AnyType>) : Decl {
        constructor(type: IType.Type, members: List<IType.Member>) : this(type.id, members.associate { it.name to it.type })

        override fun exists(env: Env): Boolean = env.elements.containsAll(members.values)
        override fun xtend(env: Env): Env {
            val type = env.getElementAs<IType.Type>(typeName) ?: return env
            val nMembers = members.map { IType.Member(it.key, it.value, type) }

            return Env(env.name, env.elements + nMembers, env.refs, env.contracts, env.projections, env.expressionCache)
        }

        override fun reduce(env: Env): Env {
            val type = env.getElementAs<IType.Type>(typeName) ?: return env
            val allNames = members.keys
            val mems = env.elements.filterIsInstance<IType.Member>()
                .filter { it.owner.name == type.name && it.name in allNames }

            val nElements = env.elements - type - mems

            return Env(env.name, nElements, env.refs, env.contracts, env.projections, env.expressionCache)
        }
    }

    data class Projection(val source: IType.Type, val target: IType.ITrait) : Decl {
        override fun exists(env: Env): Boolean = true

        override fun xtend(env: Env): Env
            = Env(env.name, env.elements, env.refs, env.contracts, env.projections + org.orbit.precess.backend.components.Projection(source, target), env.expressionCache)

        override fun reduce(env: Env): Env
            = Env(env.name, env.elements, env.refs, env.contracts, env.projections - org.orbit.precess.backend.components.Projection(source, target), env.expressionCache)
    }

    data class Cache(val expr: AnyExpr, val type: AnyType) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env
            = Env(env.name, env.elements, env.refs, env.contracts, env.projections, env.expressionCache + (expr.toString() to type))

        override fun reduce(env: Env): Env
            = Env(env.name, env.elements, env.refs, env.contracts, env.projections, env.expressionCache - expr.toString())
    }

    data class Compound<D: Decl, E: Decl>(val a: D, val b: E) : Decl {
        override fun exists(env: Env): Boolean = a.exists(env) && b.exists(env)
        override fun xtend(env: Env): Env = env.extend(a) + env.extend(b)
        override fun reduce(env: Env): Env = env.reduce(a) + env.reduce(b)
    }

    data class Multi(val decls: List<Decl>) : Decl {
        override fun exists(env: Env): Boolean = decls.all { it.exists(env) }

        override fun xtend(env: Env): Env
            = decls.fold(env) { acc, next -> acc.extend(next) }

        override fun reduce(env: Env): Env
            = decls.fold(env) { acc, next -> acc.reduce(next) }
    }

    operator fun plus(other: Decl) : Multi = when (other) {
        is Multi -> Multi(listOf(this) + other.decls)
        else -> Multi(listOf(this, other))
    }

    fun exists(env: Env): Boolean
    fun xtend(env: Env): Env

    fun extend(env: Env): Env = Env.capture { xtend(env) }
    fun reduce(env: Env): Env
}

operator fun List<Decl>.unaryPlus() : Decl
    = reduce { acc, next -> Decl.Compound(acc, next) }