package org.orbit.util

class Stack<T> {
    private val storage = mutableListOf<T>()
    val size: Int
        get() = storage.count()

    fun push(item: T) {
        storage.add(item)
    }

    fun insert(item: T, index: Int) {
        storage.add(index, item)
    }

    fun pop() : T = storage.removeLast()
    fun peek() : T? = storage.lastOrNull()
    fun isEmpty() = storage.isEmpty()
    fun isNotEmpty() = storage.isNotEmpty()
}