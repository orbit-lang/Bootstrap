package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.util.Invocation
import org.orbit.util.getKoinInstance

sealed interface Decl {
    enum class ConflictStrategy {
        Reject, Replace, Ignore
    }

    data class Clone(val name: String? = null, val cloneElements: Boolean = true, val cloneRefs: Boolean = true) : Decl {
        override fun exists(env: Env): Boolean = false
        override fun xtend(env: Env): Env = when (cloneElements) {
            true -> when (cloneRefs) {
                true -> Env(name ?: env.name, env.elements, env.refs, env.projections, env.expressionCache, env.context)
                else -> Env(name ?: env.name, env.elements, emptyList(), env.projections, env.expressionCache, env.context)
            }

            else -> when (cloneRefs) {
                true -> Env(name ?: env.name, emptyList(), env.refs, env.projections, env.expressionCache, env.context)
                else -> Env(name ?: env.name, emptyList(), emptyList(), env.projections, env.expressionCache, env.context)
            }
        }

        override fun reduce(env: Env): Env = env
    }

    data class Merge(val root: Env) : Decl {
        override fun exists(env: Env): Boolean = true
        override fun xtend(env: Env): Env {
//            if (root.components.contains(env.name)) return root
//            if (env.components.contains(root.name)) return env

            val nElements = (root.elements + env.elements).distinctBy { it.id }
            val nRefs = (root.refs + env.refs).distinctBy { it.uniqueId }
            val nProjections = (root.projections + env.projections).distinctBy { it.uniqueId }
            val nExpressionCache = (root.expressionCache + env.expressionCache)
            val nName = when (root.name) {
                env.name -> root.name
                else -> "${root.name} & ${env.name}"
            }

            return Env(nName, nElements, nRefs, nProjections, nExpressionCache, root.context + env.context, listOf(root.name, env.name))
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
        override fun xtend(env: Env): Env = env.withElement(context)
        override fun reduce(env: Env): Env = env.withoutElement(context)
    }

    data class Signature(val signature: IType.Signature) : Decl {
        override fun exists(env: Env): Boolean {
            TODO("Not yet implemented")
        }

        override fun xtend(env: Env): Env = env.withElement(signature)
        override fun reduce(env: Env): Env = env.withoutElement(signature)
    }

    data class Operator(val op: IType.IOperatorArrow<*, *>) : Decl {
        override fun exists(env: Env): Boolean = env.elements.filterIsInstance<IType.IOperatorArrow<*, *>>().any { it == op }
        override fun xtend(env: Env): Env = env.withElement(IType.Alias(op.identifier, op))
        override fun reduce(env: Env): Env = env.withoutElement(IType.Alias(op.identifier, op))
    }

    data class TypeVariable(val name: String) : Decl {
        override fun exists(env: Env): Boolean = env.elements.any { it is IType.TypeVar && it.name == name }
        override fun xtend(env: Env): Env = env.withElement(IType.TypeVar(name))
        override fun reduce(env: Env): Env = env.withoutElement(IType.TypeVar(name))
    }

    data class Type(val type: IType.Type, val members: List<IType.Member> = emptyList()) : Decl {
        constructor(type: IType.Type, members: Map<String, IType.Entity<*>> = emptyMap()) : this(type, members.map {
            IType.Member(it.key, it.value, type)
        })

        override fun exists(env: Env): Boolean = env.elements.any { it.id == type.id }
        override fun xtend(env: Env): Env = env.withElement(type)
            .withRefs(members.map { Ref(it.id, it.type) })

        override fun reduce(env: Env): Env = env.withoutElements(env.getDeclaredMembers(type) + type)
    }

    data class Trait(val trait: IType.Trait) : Decl {
        override fun exists(env: Env): Boolean = env.elements.any { it.id == trait.id }
        override fun xtend(env: Env): Env = env.withElement(trait)
        override fun reduce(env: Env): Env = env.withoutElement(trait)
    }

    data class Assignment(val name: String, val type: AnyType) : Decl {
        override fun exists(env: Env): Boolean = env.refs.any { it.name == name }
        override fun xtend(env: Env): Env {
            if (env.getRef(name) != null) {
                val invocation = getKoinInstance<Invocation>()

                throw invocation.make<TypeSystem>("`$name` is already bound in the current context: `$env`", SourcePosition.unknown)
            }

            return env.withRef(Ref(name, type))
        }

        override fun reduce(env: Env): Env = env.withoutRef(Ref(name, type))
    }

    data class TypeAlias(val name: String, val type: AnyType, val conflictStrategy: ConflictStrategy = ConflictStrategy.Reject) : Decl {
        constructor(path: Path, type: AnyType) : this(path.toString(OrbitMangler), type)

        override fun exists(env: Env): Boolean = env.elements.any { it.getCanonicalName() == name }
        override fun xtend(env: Env): Env {
            val isAlreadyDefined = when (conflictStrategy) {
                ConflictStrategy.Ignore -> false
                else -> env.elements.any { it is IType.Alias && it.name == name && it.type.id == type.id }
            }

            val nElements = if (isAlreadyDefined) {
                when (conflictStrategy) {
                    ConflictStrategy.Reject -> {
                        val invocation = getKoinInstance<Invocation>()

                        // TODO - There's probably a better way to catch naming conflicts, maybe in CanonicalNameResolver?
                        throw invocation.make<TypeSystem>("Attempt to redeclare `$name : ${type.id}`", Token.empty)
                    }

                    ConflictStrategy.Replace -> env.elements.filterNot { it is IType.Alias && it.name == name }

                    else -> env.elements
                }
            } else {
                env.elements
            }

            return env.withElementsReplaced(nElements + IType.Alias(name, type))
        }

        override fun reduce(env: Env): Env = env.withoutElements { it is IType.Alias && it.name == name }
    }

    data class Alias(val name: String, val ref: IRef) : Decl {
        override fun exists(env: Env): Boolean = env.refs.any { it.name == name }
        override fun xtend(env: Env): Env = env.withAlias(name, ref)
        override fun reduce(env: Env): Env = env.withoutAlias(name)
    }

    data class Extension(val type: AnyType, val signatures: List<IType.Signature>) : Decl {
        override fun exists(env: Env): Boolean = type.exists(env) === type
        override fun xtend(env: Env): Env {
            val signatureDecls = signatures.map(Decl::Signature)

            return env.extendAll(signatureDecls)
        }

        override fun reduce(env: Env): Env {
            val signatureDecls = signatures.map(Decl::Signature)

            return env.reduceAll(signatureDecls)
        }
    }

    data class Projection(val source: AnyType, val target: IType.Trait) : Decl {
        override fun exists(env: Env): Boolean = true

        override fun xtend(env: Env): Env = env.withProjection(source, target)
        override fun reduce(env: Env): Env = env.withoutProjection(source, target)
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

    fun extend(env: Env): Env = xtend(env) //Env.capture { xtend(env) }
    fun reduce(env: Env): Env
}

operator fun List<Decl>.unaryPlus() : Decl
    = reduce { acc, next -> Decl.Compound(acc, next) }