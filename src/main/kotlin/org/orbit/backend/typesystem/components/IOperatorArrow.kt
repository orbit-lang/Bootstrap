package org.orbit.backend.typesystem.components

import org.orbit.core.nodes.OperatorFixity
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

sealed interface IOperatorArrow<A: IArrow<A>, Self: IOperatorArrow<A, Self>> : IArrow<Self> {
    val fixity: OperatorFixity
    val symbol: String
    val identifier: String
    val arrow: A

    override val id: String get() = "$identifier:${arrow.id}"
    override val effects: List<Effect> get() = arrow.effects

    override fun getDomain(): List<AnyType> = arrow.getDomain()
    override fun getCodomain(): AnyType = arrow.getCodomain()
    override fun curry(): IArrow<*> = arrow.curry()
    override fun never(args: List<AnyType>): Never = Never("")
    override fun getCardinality(): ITypeCardinality
        = arrow.getCodomain().getCardinality()

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()
        val prettyName = printer.apply("${fixity.name} $identifier `$symbol`", PrintableKey.Italics)

        return "$prettyName $arrow"
    }
}