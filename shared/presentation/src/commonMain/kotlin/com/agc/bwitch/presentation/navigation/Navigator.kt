package com.agc.bwitch.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Navigator(
    start: Destination = Destination.Portal
) {
    private val backStack = ArrayDeque<Destination>().apply { add(start) }

    private val _current = MutableStateFlow(backStack.last())
    val current: StateFlow<Destination> = _current.asStateFlow()

    fun navigate(to: Destination) {
        backStack.add(to)
        _current.value = backStack.last()
    }

    fun canGoBack(): Boolean = backStack.size > 1

    fun goBack(): Boolean {
        if (!canGoBack()) return false
        backStack.removeLast()
        _current.value = backStack.last()
        return true
    }

    fun resetToRoot(root: Destination = Destination.Portal) {
        backStack.clear()
        backStack.add(root)
        _current.value = backStack.last()
    }
}
