package com.cosmos.unreddit.util

import java.util.Locale

/**
 * Helpers to build file names that are valid on Windows, macOS and Unix.
 *
 * The rules applied here are the intersection of the three platforms:
 *
 * - Windows forbids `\ / : * ? " < > |` and control characters, ignores trailing dots and spaces,
 *   and reserves a set of legacy device names (`CON`, `NUL`, `COM1`…).
 * - macOS (HFS+/APFS) forbids `:` and `/`.
 * - Unix only forbids `/` and NUL, but limits each path component to 255 bytes.
 */
object FilenameUtil {

    /**
     * Longest single path component allowed by ext4/APFS (bytes) and NTFS (UTF-16 units).
     */
    private const val MAX_COMPONENT_LENGTH = 255

    /**
     * Upper bound for the user-controlled part of a file name.
     *
     * Reddit user names are at most 20 characters, so this only ever kicks in for unexpected
     * input. It leaves ample room for the timestamp, the `_video` / `_audio` suffixes and the
     * extension appended by the caller, which keeps the assembled name below
     * [MAX_COMPONENT_LENGTH] on every platform.
     */
    const val MAX_BASE_LENGTH = 100

    private const val REPLACEMENT = '_'

    private val ILLEGAL_CHARACTERS = charArrayOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')

    /**
     * Legacy MS-DOS device names. Windows rejects them with or without an extension.
     */
    private val RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    /**
     * Turns [name] into a string that can be used as the base of a file name on any platform, or
     * returns null when nothing usable is left.
     */
    fun sanitize(name: String?, maxLength: Int = MAX_BASE_LENGTH): String? {
        if (name.isNullOrBlank()) return null

        val sanitized = name
            .map { character ->
                when {
                    character in ILLEGAL_CHARACTERS -> REPLACEMENT
                    character.isISOControl() -> REPLACEMENT
                    else -> character
                }
            }
            .joinToString("")
            .trim()
            .truncate(maxLength)
            // Windows silently drops trailing dots and spaces, which would defeat the truncation
            .trimEnd('.', ' ')

        return when {
            sanitized.isEmpty() -> null
            sanitized.isReserved() -> "$REPLACEMENT$sanitized"
            else -> sanitized
        }
    }

    /**
     * Truncates to [maxLength] characters without splitting a surrogate pair, then to [maxLength]
     * bytes so the result stays valid on file systems that count bytes rather than characters.
     */
    private fun String.truncate(maxLength: Int): String {
        var truncated = if (length > maxLength) {
            val end = if (Character.isHighSurrogate(this[maxLength - 1])) {
                maxLength - 1
            } else {
                maxLength
            }
            substring(0, end)
        } else {
            this
        }

        while (truncated.toByteArray(Charsets.UTF_8).size > MAX_COMPONENT_LENGTH) {
            truncated = truncated.dropLast(1)

            if (truncated.isNotEmpty() && Character.isHighSurrogate(truncated.last())) {
                truncated = truncated.dropLast(1)
            }
        }

        return truncated
    }

    private fun String.isReserved(): Boolean {
        return substringBefore('.').uppercase(Locale.ROOT) in RESERVED_NAMES
    }
}
