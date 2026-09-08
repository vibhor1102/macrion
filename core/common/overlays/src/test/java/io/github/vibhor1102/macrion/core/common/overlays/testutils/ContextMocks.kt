/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.overlays.testutils

import android.content.Context

import org.mockito.Mockito

/** Configure a mocked [Context] to return [service] for both system-service lookup overloads. */
fun <T> Context.mockSystemService(serviceClass: Class<T>, serviceName: String, service: T) {
    Mockito.`when`(getSystemServiceName(serviceClass)).thenReturn(serviceName)
    Mockito.`when`(getSystemService(serviceClass)).thenReturn(service)
    Mockito.`when`(getSystemService(serviceName)).thenReturn(service)
}
