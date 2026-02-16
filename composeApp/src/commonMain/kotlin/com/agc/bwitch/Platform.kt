package com.agc.bwitch

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform