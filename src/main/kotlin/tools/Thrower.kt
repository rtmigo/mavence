/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

package tools

inline fun <R> rethrowingState(
    error: () -> String,
    call: () -> R,
): R {
    try {
        return call()
    } catch (e: Exception) {
        throw IllegalStateException(error(), e)
    }
}