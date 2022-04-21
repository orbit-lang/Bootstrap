package org.orbit.core.components

open class CompilationScheme(entries: List<CompilationSchemeEntry>) : MutableListIterator<CompilationSchemeEntry> {
	private val _entries = entries.toMutableList()

	companion object Intrinsics : CompilationScheme(listOf(
        CompilationSchemeEntry.commentParser,
        CompilationSchemeEntry.lexer,
        CompilationSchemeEntry.parser,
        CompilationSchemeEntry.canonicalNameResolver,
        CompilationSchemeEntry.observers,
        CompilationSchemeEntry.typeSystem,
//		CompilationSchemeEntry.traitEnforcer,
//        CompilationSchemeEntry.mainResolver
	))

	override fun next(): CompilationSchemeEntry {
		return _entries.removeFirst()
	}

	override fun hasPrevious(): Boolean {
		return false
	}

	override fun nextIndex(): Int {
		return 0
	}

	override fun previous(): CompilationSchemeEntry {
		TODO("Not yet implemented")
	}

	override fun previousIndex(): Int {
		TODO("Not yet implemented")
	}

	override fun add(element: CompilationSchemeEntry) {
		_entries.add(0, element)
	}

	override fun hasNext(): Boolean = _entries.isNotEmpty()

	override fun remove() {
		_entries.removeFirst()
	}

	override fun set(element: CompilationSchemeEntry) {
		TODO("Not yet implemented")
	}
}