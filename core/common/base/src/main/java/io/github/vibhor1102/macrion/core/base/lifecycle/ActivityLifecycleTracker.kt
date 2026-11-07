/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.base.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.Collections
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Keep unsolicited review prompts from covering an existing editor or permission activity. */
@Singleton
class ActivityLifecycleTracker @Inject constructor() : Application.ActivityLifecycleCallbacks {
    private val activities = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    fun hasActiveActivity(): Boolean = activities.any { !it.isFinishing && !it.isDestroyed }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { activities.add(activity) }
    override fun onActivityDestroyed(activity: Activity) { activities.remove(activity) }
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
