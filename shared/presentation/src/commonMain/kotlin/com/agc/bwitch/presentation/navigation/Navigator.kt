package com.agc.bwitch.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Navigator(
    start: Destination = Destination.AuthGate
) {
    private val backStack = ArrayDeque<Destination>().apply { addLast(start) }

    private val _current = MutableStateFlow(backStack.last())
    val current: StateFlow<Destination> = _current.asStateFlow()

    fun navigate(to: Destination) {
        backStack.addLast(to)
        _current.value = backStack.last()
    }

    fun replace(to: Destination) {
        if (backStack.isNotEmpty()) backStack.removeLast()
        backStack.addLast(to)
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
        _current.value = backStack.last()
    }

    fun canGoBack(): Boolean = backStack.size > 1

    fun goBack(): Boolean {
        if (!canGoBack()) return false
        backStack.removeLast()
        _current.value = backStack.last()
        return true
    }

    /**
     * Cambia el root (AuthGate/Portal/etc) y resetea el stack.
     */
    fun resetToRoot(root: Destination = Destination.Portal) {
        backStack.clear()
        backStack.addLast(root)
        _current.value = backStack.last()
    }
}