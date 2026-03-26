package com.agc.bwitch.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Navigator(
    start: Destination = Destination.AuthGate
) {
    private val backStack = ArrayDeque<Destination>().apply { addLast(start) }
    private val topLevelRoots = setOf(
        Destination.UserProfile,
        Destination.Astrology,
        Destination.Guide,
        Destination.Rituals,
    )
    private val topLevelStacks = mutableMapOf<Destination, ArrayDeque<Destination>>()

    private val _current = MutableStateFlow(backStack.last())
    val current: StateFlow<Destination> = _current.asStateFlow()

    fun navigate(to: Destination) {
        backStack.addLast(to)
        persistTopLevelStack()
        _current.value = backStack.last()
    }

    fun replace(to: Destination) {
        if (backStack.isNotEmpty()) backStack.removeLast()
        backStack.addLast(to)
        persistTopLevelStack()
        _current.value = backStack.last()
    }

    /**
     * Vuelve al root ACTUAL (sin cambiar cuál es).
     */
    fun popToRoot() {
        if (backStack.isEmpty()) return
        val root = backStack.first()
        backStack.clear()
        backStack.addLast(root)
        persistTopLevelStack()
        _current.value = backStack.last()
    }

    fun canGoBack(): Boolean = backStack.size > 1

    fun goBack(): Boolean {
        if (!canGoBack()) return false
        backStack.removeLast()
        persistTopLevelStack()
        _current.value = backStack.last()
        return true
    }

    fun switchTopLevel(root: Destination) {
        if (root !in topLevelRoots) {
            resetToRoot(root)
            return
        }

        persistTopLevelStack()
        val targetStack = topLevelStacks[root] ?: ArrayDeque<Destination>().apply { addLast(root) }
        backStack.clear()
        backStack.addAll(targetStack)
        _current.value = backStack.last()
    }

    fun isAtRootOf(destination: Destination): Boolean {
        return backStack.size == 1 && backStack.firstOrNull() == destination
    }

    /**
     * Cambia el root y resetea el stack.
     */
    fun resetToRoot(root: Destination = Destination.UserProfile) {
        backStack.clear()
        backStack.addLast(root)
        if (root in topLevelRoots) {
            topLevelStacks[root] = ArrayDeque<Destination>().apply { addLast(root) }
        }
        _current.value = backStack.last()
    }

    private fun persistTopLevelStack() {
        val currentRoot = backStack.firstOrNull() ?: return
        if (currentRoot in topLevelRoots) {
            topLevelStacks[currentRoot] = ArrayDeque<Destination>().apply { addAll(backStack) }
        }
    }
}
