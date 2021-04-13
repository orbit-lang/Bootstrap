package org.orbit.util

class Repl {
    fun read() {
        print(">> ")
        val line = readLine() ?: return
    }
}