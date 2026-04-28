package com.agc.bwitch.analytics

import android.app.Application
import android.os.Bundle
import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker

/**
 * Reflection-based Firebase Analytics adapter.
 *
 * It logs events when firebase-analytics is present in the runtime classpath.
 * If the dependency is missing, this tracker degrades gracefully to no-op.
 */
class AndroidFirebaseAnalyticsTracker(
    app: Application,
) : AnalyticsTracker {
    private val firebaseInstance: Any? = runCatching {
        val firebaseClass = Class.forName("com.google.firebase.analytics.FirebaseAnalytics")
        val getInstance = firebaseClass.getMethod("getInstance", android.content.Context::class.java)
        getInstance.invoke(null, app)
    }.getOrNull()

    override fun track(event: AnalyticsEvent) {
        val instance = firebaseInstance ?: return
        runCatching {
            val bundle = Bundle().apply {
                event.params().forEach { (key, value) ->
                    putString(key, value)
                }
            }
            val logEvent = instance::class.java.getMethod("logEvent", String::class.java, Bundle::class.java)
            logEvent.invoke(instance, event.name, bundle)
        }
    }
}
