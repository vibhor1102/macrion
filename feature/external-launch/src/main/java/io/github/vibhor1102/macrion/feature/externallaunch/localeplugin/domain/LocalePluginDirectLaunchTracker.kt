/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalePluginDirectLaunchTracker @Inject constructor() {

    private val pendingRequests = ConcurrentHashMap.newKeySet<String>()
    private val openedRequests = ConcurrentHashMap.newKeySet<String>()
    private val latestRequestId = AtomicReference<String?>(null)
    private val activeExecutionRequestId = AtomicReference<String?>(null)
    private val awaitingProjectionDirectRequestId = AtomicReference<String?>(null)
    private val autoRunRequestIds = ConcurrentHashMap.newKeySet<String>()

    fun markPending(configurationJson: String) {
        clearAllAutoRun()
        latestRequestId.set(configurationJson)
        pendingRequests.add(configurationJson)
        openedRequests.remove(configurationJson)
    }

    /**
     * Claims a direct background launch. A request which timed out, was replaced, or was stopped
     * must never be allowed to revive when Android delivers its PendingIntent late.
     */
    fun claimDirectLaunch(requestId: String): Boolean {
        if (!isLatest(requestId)) return false
        if (activeExecutionRequestId.get() == requestId) return true
        if (!pendingRequests.remove(requestId)) return false

        activeExecutionRequestId.set(requestId)
        if (!isLatest(requestId)) {
            activeExecutionRequestId.compareAndSet(requestId, null)
            return false
        }
        openedRequests.add(requestId)
        return true
    }

    /** Claims the latest notification continuation only when another launch is not already on screen. */
    fun claimFallbackLaunch(requestId: String): Boolean {
        if (!isLatest(requestId)) return false
        if (activeExecutionRequestId.get()?.let { it != requestId } == true) return false
        activeExecutionRequestId.set(requestId)
        if (!isLatest(requestId)) {
            activeExecutionRequestId.compareAndSet(requestId, null)
            return false
        }
        return true
    }

    fun claimRecoveredLaunch(requestId: String) {
        latestRequestId.set(requestId)
        activeExecutionRequestId.set(requestId)
    }

    fun consumeOpened(configurationJson: String): Boolean {
        pendingRequests.remove(configurationJson)
        return openedRequests.remove(configurationJson)
    }

    fun abandon(requestId: String) {
        clearAwaitingProjection(requestId)
        autoRunRequestIds.remove(requestId)
        pendingRequests.remove(requestId)
        openedRequests.remove(requestId)
    }

    /**
     * A second background request must not be sent to the same translucent helper Activity while
     * a first request is awaiting permission or being displayed. The caller should offer the
     * notification continuation instead.
     */
    fun hasAnotherInFlightRequest(requestId: String): Boolean =
        (activeExecutionRequestId.get()?.let { it != requestId } == true) ||
            pendingRequests.any { it != requestId }

    fun markExecutionClosed(requestId: String?) {
        if (requestId != null) {
            clearAwaitingProjection(requestId)
            autoRunRequestIds.remove(requestId)
            activeExecutionRequestId.compareAndSet(requestId, null)
        }
    }

    fun markAwaitingProjection(requestId: String) {
        if (isCurrentExecution(requestId)) {
            awaitingProjectionDirectRequestId.set(requestId)
        }
    }

    fun clearAwaitingProjection(requestId: String?) {
        if (requestId != null) {
            awaitingProjectionDirectRequestId.compareAndSet(requestId, null)
        }
    }

    fun getAwaitingProjectionRequestId(): String? {
        val id = awaitingProjectionDirectRequestId.get() ?: return null
        return if (isCurrentExecution(id)) id else null
    }

    fun markAutoRunPending(requestId: String) {
        if (getAwaitingProjectionRequestId() == requestId) {
            autoRunRequestIds.add(requestId)
        }
    }

    fun consumeAutoRun(requestId: String): Boolean {
        clearAwaitingProjection(requestId)
        return autoRunRequestIds.remove(requestId)
    }

    fun clearAllAutoRun() {
        awaitingProjectionDirectRequestId.set(null)
        autoRunRequestIds.clear()
    }

    fun isLatest(requestId: String): Boolean = latestRequestId.get() == requestId

    fun isCurrentExecution(requestId: String?): Boolean =
        requestId != null && isLatest(requestId) && activeExecutionRequestId.get() == requestId
}
