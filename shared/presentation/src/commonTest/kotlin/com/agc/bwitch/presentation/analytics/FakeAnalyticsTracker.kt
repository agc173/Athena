package com.agc.bwitch.presentation.analytics

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker

class FakeAnalyticsTracker : AnalyticsTracker {
    private val _events = mutableListOf<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = _events

    override fun track(event: AnalyticsEvent) {
        _events += event
    }
}
