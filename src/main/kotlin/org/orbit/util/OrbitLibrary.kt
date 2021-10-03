package org.orbit.util

import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getResult
import org.orbit.graph.components.Graph
import org.orbit.graph.components.Scope
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.Context
import java.io.*

data class OrbitLibrary(val scopes: List<Scope>, val context: Context, val graph: Graph) : Serializable {
	companion object : FilenameFilter {
		fun fromInvocation(invocation: Invocation) : OrbitLibrary {
			val names = invocation.getResult<NameResolverResult>(CompilationSchemeEntry.canonicalNameResolver)
			val context = invocation.getResult<Context>(CompilationSchemeEntry.typeSystem)

			return OrbitLibrary(names.environment.scopes, context, names.graph)
		}

		fun fromPath(path: File) : OrbitLibrary {
			val fis = FileInputStream(path)
			val ois = ObjectInputStream(fis)

			return ois.use { ois ->
				ois.readObject() as OrbitLibrary
			}
		}

		override fun accept(dir: File?, name: String?): Boolean {
			return name?.endsWith(".orbl") ?: false
		}
	}

	fun write(path: File) {
		val fos = FileOutputStream(path)
		val oos = ObjectOutputStream(fos)

		oos.writeObject(this)

		oos.close()
	}
}