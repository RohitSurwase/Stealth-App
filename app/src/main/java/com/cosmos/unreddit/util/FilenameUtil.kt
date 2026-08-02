package com.cosmos.unreddit.util

/**
 * Helpers to build file names that are valid on Android, Windows, macOS and Unix.
 *
 * The rules applied here are the intersection of the four platforms:
 *
 * - Windows forbids `\ / : * ? " < > |` and control characters, ignores trailing dots and spaces,
 *   and reserves a set of legacy device names (`CON`, `NUL`, `COM1`…).
 * - Android enforces the FAT rules on external storage, whatever the actual file system: the
 *   character set is the one of `android.os.FileUtils.isValidFatFilenameChar`, which is the Windows
 *   set plus `DEL` and the C1 controls, and `MediaStore` runs the same sanitization on
 *   `DISPLAY_NAME`. A name starting with a dot is hidden from the media scanner, so the download
 *   would never show up in the gallery.
 * - macOS (HFS+/APFS) forbids `:` and `/`.
 * - Unix only forbids `/` and NUL, but limits each path component to 255 bytes.
 */
object FilenameUtil {

    /**
     * Upper bound for the user-controlled part of a file name.
     *
     * Reddit user names are at most 20 characters, so this only ever kicks in for unexpected input.
     * Nothing [UNSUPPORTED_CHARACTERS] leaves behind takes more than two UTF-8 bytes, so 100
     * characters are at most 200 bytes, and the assembled name stays below the 255 byte limit that
     * ext4, APFS and NTFS put on a path component once the caller has appended its
     * `_yyyyMMdd_HHmmss` timestamp, its `_video` / `_audio` suffix and an extension.
     */
    const val MAX_BASE_LENGTH = 100

    /**
     * Characters no platform accepts: the Windows set and the `Cc` category, which is exactly the
     * control characters Android rejects (NUL, DEL and the C1 range).
     */
    private val ILLEGAL_CHARACTERS = Regex("""[\\/:*?"<>|\p{Cc}]""")

    /**
     * Everything that is neither printable ASCII nor a Latin letter of a western European alphabet
     * (Latin-1 Supplement and Latin Extended-A).
     *
     * Other scripts and emoji are dropped rather than transliterated: they are unexpected in a
     * Reddit user name, and leaving them out keeps every name to one or two bytes per character.
     *
     * The bounds are written as escapes so that the pattern does not depend on the encoding the
     * compiler reads this file with.
     */
    private val UNSUPPORTED_CHARACTERS = Regex("""[^\x20-\x7E\u00C0-\u017F]""")

    /**
     * Legacy MS-DOS device names, which Windows rejects with or without an extension.
     */
    private val RESERVED_NAME = Regex(
        """(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\..*)?""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Turns [name] into a string that can be used as the base of a file name on any platform, or
     * returns null when nothing usable is left.
     */
    fun sanitize(name: String?, maxLength: Int = MAX_BASE_LENGTH): String? {
        val sanitized = name.orEmpty()
            .replace(ILLEGAL_CHARACTERS, "_")
            .replace(UNSUPPORTED_CHARACTERS, "")
            // A leading dot hides the file from the Android media scanner, and it is what turns
            // "." and ".." into directory references rather than names; Windows silently drops
            // trailing dots and spaces
            .trim('.', ' ')
            .take(maxLength)
            // Truncating can uncover a new trailing dot or space
            .trimEnd('.', ' ')

        return when {
            sanitized.isEmpty() -> null
            RESERVED_NAME.matches(sanitized) -> "_$sanitized"
            else -> sanitized
        }
    }
}
