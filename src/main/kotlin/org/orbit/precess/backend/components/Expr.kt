package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.*

sealed interface Expr<Self : Expr<Self>> : Substitutable<Self>, Inf<Self>, IPrecessComponent {
    data class Var(val name: String) : Expr<Var> {
        override fun substitute(substitution: Substitution): Var = this

        override fun infer(env: Env): IType<*> =
            env.getRef(name)?.type ?: IType.Never("`$name` is undefined in the current context")

        override fun toString(): String = name
    }

    sealed interface ITypeExpr<Self: ITypeExpr<Self>> : Expr<Self>

    data class Safe(val term: AnyExpr) : Expr<Safe> {
        override fun substitute(substitution: Substitution): Safe
            = Safe(term.substitute(substitution))

        override fun toString(): String
            = "safe $term"

        override fun infer(env: Env): IType<*>
            = IType.Safe(term.infer(env))
    }

    data class Type(val typeId: String) : Expr<Type> {
        override fun substitute(substitution: Substitution): Type = this
        override fun toString(): String = typeId

        override fun infer(env: Env): IType<*> =
            env.getElement(typeId) ?: IType.Never("Unknown Type `$typeId` in current context:\n$env")
    }

    data class AnyTypeLiteral(val type: AnyType) : Expr<AnyTypeLiteral> {
        override fun substitute(substitution: Substitution): AnyTypeLiteral = AnyTypeLiteral(type.substitute(substitution))
        override fun toString(): String = type.id
        override fun infer(env: Env): AnyType
            = type.exists(env)
    }

    data class Arrow1(val domainExpr: AnyExpr, val codomainExpr: AnyExpr) : ITypeExpr<Arrow1> {
        override fun substitute(substitution: Substitution): Arrow1
            = Arrow1(domainExpr.substitute(substitution), codomainExpr.substitute(substitution))

        override fun toString(): String = "($domainExpr) -> $codomainExpr"

        override fun infer(env: Env): IType<*>
            = IType.Arrow1(domainExpr.infer(env), codomainExpr.infer(env))
    }

    data class Block(val body: List<AnyExpr>) : Expr<Block> {
        override fun substitute(substitution: Substitution): Block = Block(body.map { it.substitute(substitution) })

        override fun infer(env: Env): IType<*> = when (body.isEmpty()) {
            true -> IType.Unit
            else -> body.last().infer(env)
        }

        override fun toString(): String = """
                `{
                    ${body.joinToString("\n\t") { it.toString() }}
                }`
            """.trimIndent()
    }

    data class Return(val expr: AnyExpr) : Expr<Return> {
        override fun substitute(substitution: Substitution): Return = Return(expr.substitute(substitution))

        override fun toString(): String = "`return $expr`"

        override fun infer(env: Env): IType<*> = expr.infer(env)
    }

    sealed interface MatchResult {
        data class ReachablePattern(val env: Env) : MatchResult
        data class UnreachablePattern(val reason: IType.Never) : MatchResult
    }

    sealed interface IPattern : Expr<IPattern> {
        fun match(env: Env, target: AnyExpr): MatchResult
    }

    object ElsePattern : IPattern {
        override fun substitute(substitution: Substitution): IPattern = this
        override fun infer(env: Env): IType<*> = IType.Unit
        override fun match(env: Env, target: AnyExpr): MatchResult = MatchResult.ReachablePattern(env)

        override fun toString(): String = "`else`"
    }

    data class EqPattern(val expr: AnyExpr) : IPattern {
        override fun substitute(substitution: Substitution): IPattern = this
        override fun infer(env: Env): AnyType = expr.infer(env)
        override fun match(env: Env, target: AnyExpr): MatchResult =
            when (TypeUtils.check(env, expr, target.infer(env))) {
                true -> MatchResult.ReachablePattern(env)
                else -> MatchResult.UnreachablePattern(IType.Never("$expr will never match against $target"))
            }

        override fun toString(): String = "`case ? == $expr`"
    }

//        data class ConstructorPattern(val type: IType.Type, val) {
//        }

    data class Case(val pattern: IPattern, val block: Block) : Expr<Case> {
        override fun substitute(substitution: Substitution): Case =
            Case(pattern.substitute(substitution), block.substitute(substitution))

        override fun infer(env: Env): IType<*> = IType.Arrow1(pattern.infer(env), block.infer(env))
    }

    data class Select(val target: AnyExpr, val cases: List<Case>) : Expr<Select> {
        override fun substitute(substitution: Substitution): Select =
            Select(target.substitute(substitution), cases.map { it.substitute(substitution) })

        override fun infer(env: Env): IType<*> = cases.map { it.block.infer(env) as IType.UnifiableType<*> }
            .reduce { acc, next -> TypeUtils.unify(env, acc, next) }

        fun verify(env: Env): Select {
            val unreachable = cases.map { it.pattern.match(env, target) }
                .filterIsInstance<MatchResult.UnreachablePattern>()

            if (unreachable.isEmpty()) return this

            unreachable.fold(IType.Never("The following errors were found while verifying Select expression:")) { acc, next ->
                acc.unify(env, next.reason) as IType.Never
            }.panic()
        }
    }

    data class Invoke(val arrow: AnyArrow, val args: List<AnyExpr>) : Expr<Invoke> {
        constructor(arrow: AnyArrow, arg: AnyExpr) : this(arrow, listOf(arg))

        override fun substitute(substitution: Substitution): Invoke =
            Invoke(arrow.substitute(substitution), args.map { it.substitute(substitution) })

        override fun toString(): String = "${arrow.id}(${args.joinToString(", ") { it.toString() }})"

        override fun infer(env: Env): IType<*> {
            val exit = { arrow.never(args.map { it.infer(env) }) }
            val domain = arrow.getDomain()

            if (args.count() != domain.count()) return exit()

            val checked = args.zip(domain).fold(true) { acc, next ->
                if (!acc) return exit()

                acc && TypeUtils.check(env, next.first, next.second)
            }

            if (!checked) return exit()

            return arrow.getCodomain()
        }
    }

    data class Symbol(val name: String) : Expr<Symbol> {
        override fun substitute(substitution: Substitution): Symbol = this
        override fun infer(env: Env): IType<*> = IType.Never("TODO - Symbol")
        override fun toString(): String = name
    }

    data class Box(val term: AnyExpr) : Expr<Box> {
        override fun substitute(substitution: Substitution): Box = Box(term.substitute(substitution))
        override fun infer(env: Env): IType<*> = IType.Box(term)
        override fun toString(): String = "⎡$term⎦"
    }

    data class Unbox(val term: AnyExpr) : Expr<Unbox> {
        override fun substitute(substitution: Substitution): Unbox = Unbox(term.substitute(substitution))
        override fun infer(env: Env): IType<*> = when (val box = term.infer(env).exists(env)) {
            is IType.UnboxableType -> box.unbox(env)
            else -> IType.Never("Cannot unbox non-boxed expression: `$term`")
        }
        override fun toString(): String = "⎣$term⎤"
    }
}