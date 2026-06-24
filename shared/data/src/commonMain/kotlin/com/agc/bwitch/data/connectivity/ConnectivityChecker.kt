package com.agc.bwitch.data.connectivity

interface ConnectivityChecker {
    fun hasUsableConnection(): Boolean
}

object AlwaysOnlineConnectivityChecker : ConnectivityChecker {
    override fun hasUsableConnection(): Boolean = true
}
