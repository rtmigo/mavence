/**
 * SPDX-FileCopyrightText: (c) 2022 Artsiom iG (rtmigo.github.io)
 * SPDX-License-Identifier: ISC
 **/

package tools

import org.jline.terminal.TerminalBuilder

private val termWidth = try {
    TerminalBuilder.builder().build().use { it.width }
} catch (_: Throwable) {
    80
}

fun eprintHeader(text: String) {
    fun sized(n: Int) = List(n.coerceAtLeast(2)) { '•' }.joinToString("")
    System.err.println()
    val prefix = "••[ " + text.uppercase() + " ]"
    System.err.println(prefix + sized(termWidth - prefix.length))
    System.err.println()
}

fun eprint(s: String) = System.err.println(s)
fun eprint() = System.err.println("")