package org.orbit.core

import org.orbit.util.AnyPrintable
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

open class Path(open val relativeNames: List<String>) : AnyPrintable, Collection<String> by relativeNames {
	companion object {
		val empty = Path()
		val self = Path("Self")
		val infer = Path("_")
		val array = Path("[]")
		val any = Path("Any")
		val never = Path("Never")
		val lambda = Path("->")
	}

	constructor(path: String) : this(listOf(path))
	constructor(vararg paths: String) : this(paths.toList())

	var enclosingScope: Scope? = null

	open operator fun plus(other: Path) : Path {
		val a = relativeNames.toList()
		val b = other.relativeNames.toList()

		return Path(a + b)
	}

	operator fun plus(others: List<Path>) : Path {
		return others.fold(this) { acc, next ->
			acc + next
		}
	}

	operator fun plus(other: String) : Path {
		return this + Path(other)
	}

	fun endsWith(other: Path) : Boolean = when (relativeNames.count() > other.count()) {
		true -> {
			val slice = Path(relativeNames.drop(relativeNames.count() - other.count()))

			slice == other
		}

		else -> false
	}

	open operator fun minus(other: Path) : Path {
		return Path(relativeNames.subList(0, relativeNames.indexOf(other.relativeNames.last())))
	}

	fun containsSubPath(other: Path, mangler: INameMangler = OrbitMangler) : Boolean {
		return toString(mangler).startsWith(other.toString(mangler))
	}

	fun dropFirst(n: Int) : Path {
		return Path(relativeNames.drop(n))
	}

	fun dropLast(n: Int) : Path {
		return Path(relativeNames.dropLast(n))
	}

	fun drop(other: Path) : Path {
		return dropFirst(other.size)
	}

	fun isAncestor(of: Path) : Boolean {
		if (this == of) return false

		val sub = of.relativeNames.dropLast(1)

		return relativeNames == sub
	}

	fun containsPart(part: String) : Boolean {
		return relativeNames.contains(part)
	}

	fun matchPartial(other: Path) : Boolean {
		return toString(OrbitMangler).startsWith(other.toString(OrbitMangler))
	}

	fun promote() : FullyQualifiedPath = FullyQualifiedPath(this)

	fun toString(mangler: INameMangler) : String {
		return mangler.mangle(this)
	}

	override fun toString(printer: Printer): String {
		return printer.apply(toString(OrbitMangler), PrintableKey.Bold)
	}

	override fun equals(other: Any?) = when (other) {
		is Path -> other.relativeNames.joinToString("") == relativeNames.joinToString("")
		else -> false
	}
}