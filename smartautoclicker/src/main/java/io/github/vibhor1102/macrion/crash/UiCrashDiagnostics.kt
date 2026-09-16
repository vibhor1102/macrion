/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.crash

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.github.vibhor1102.macrion.core.base.crash.CrashDiagnostics

/** Lifecycle callbacks only; no polling, screen content or activity references retained. */
fun Application.registerUiCrashDiagnostics() {
    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            CrashDiagnostics.record(CrashDiagnostics.Event.ACTIVITY_RESUMED, activity.javaClass.name,
                attached = activity.window.decorView.isAttachedToWindow)
        }
        override fun onActivityPaused(activity: Activity) {
            CrashDiagnostics.record(CrashDiagnostics.Event.ACTIVITY_PAUSED, activity.javaClass.name,
                attached = activity.window.decorView.isAttachedToWindow)
        }
        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    })
}
