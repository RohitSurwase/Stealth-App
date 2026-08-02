package com.cosmos.unreddit.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilenameUtilTest {

    /**
     * U+3042 HIRAGANA LETTER A, a character outside the supported range.
     *
     * The characters this class exercises are built from their code point rather than written as
     * literals, so that the assertions do not depend on the encoding the compiler reads the file
     * with and stay readable in a diff.
     */
    private val unsupportedCharacter = Char(0x3042).toString()

    /**
     * U+1F600 GRINNING FACE, outside the BMP and so a surrogate pair in UTF-16.
     */
    private val surrogatePair = String(Character.toChars(0x1F600))

    /**
     * U+00E9 LATIN SMALL LETTER E WITH ACUTE, supported and two bytes once encoded to UTF-8.
     */
    private val accentedCharacter = Char(0x00E9).toString()

    @Test
    fun `typical reddit user name is left untouched`() {
        assertEquals("spez", FilenameUtil.sanitize("spez"))
        assertEquals("some_user-name", FilenameUtil.sanitize("some_user-name"))
        assertEquals("[deleted]", FilenameUtil.sanitize("[deleted]"))
    }

    @Test
    fun `path separators and windows reserved characters are replaced`() {
        assertEquals("a_b", FilenameUtil.sanitize("a/b"))
        assertEquals("a_b", FilenameUtil.sanitize("a\\b"))
        assertEquals("a_b", FilenameUtil.sanitize("a:b"))
        assertEquals("_______", FilenameUtil.sanitize("*?\"<>|/"))
    }

    @Test
    fun `control characters are replaced but spaces are kept`() {
        assertEquals("a_b", FilenameUtil.sanitize("a\nb"))
        assertEquals("a_b", FilenameUtil.sanitize("a\tb"))
        assertEquals("a_b", FilenameUtil.sanitize("a" + Char(0x00) + "b"))
        assertEquals("a b", FilenameUtil.sanitize("a b"))
    }

    @Test
    fun `characters rejected by android but allowed by windows are replaced`() {
        // DEL and the C1 range are rejected by FileUtils#isValidFatFilenameChar
        assertEquals("a_b", FilenameUtil.sanitize("a" + Char(0x7F) + "b"))
        assertEquals("a_b", FilenameUtil.sanitize("a" + Char(0x85) + "b"))
        assertEquals("a_b", FilenameUtil.sanitize("a" + Char(0x9F) + "b"))
    }

    @Test
    fun `western european letters are kept`() {
        assertEquals("a${accentedCharacter}b", FilenameUtil.sanitize("a${accentedCharacter}b"))
        // U+00C0 and U+017F, the bounds of the supported range
        listOf(0x00C0, 0x017F).map { Char(it).toString() }.forEach {
            assertEquals(it, FilenameUtil.sanitize(it))
        }
    }

    @Test
    fun `characters outside the supported range are dropped`() {
        assertEquals("ab", FilenameUtil.sanitize("a${unsupportedCharacter}b"))
        assertEquals("ab", FilenameUtil.sanitize("a${surrogatePair}b"))
        // A lone surrogate cannot be encoded and would reach the file system as '?'
        assertEquals("ab", FilenameUtil.sanitize("a" + Char(0xD83D) + "b"))
        assertEquals("ab", FilenameUtil.sanitize("a" + Char(0xDE00) + "b"))
        assertNull(FilenameUtil.sanitize(unsupportedCharacter.repeat(10)))
    }

    @Test
    fun `leading dots are removed so the file is not hidden from the media scanner`() {
        assertEquals("user", FilenameUtil.sanitize(".user"))
        assertEquals("nomedia", FilenameUtil.sanitize(".nomedia"))
        assertEquals("user", FilenameUtil.sanitize(" . . user"))
        assertEquals("user.mp4", FilenameUtil.sanitize("..user.mp4"))
        assertNull(FilenameUtil.sanitize("."))
        assertNull(FilenameUtil.sanitize(".."))
    }

    @Test
    fun `leading and trailing whitespace and trailing dots are removed`() {
        assertEquals("user", FilenameUtil.sanitize("  user  "))
        assertEquals("user", FilenameUtil.sanitize("user..."))
        assertEquals("user", FilenameUtil.sanitize("user. . "))
    }

    @Test
    fun `windows device names are escaped`() {
        assertEquals("_CON", FilenameUtil.sanitize("CON"))
        assertEquals("_nul", FilenameUtil.sanitize("nul"))
        assertEquals("_COM1", FilenameUtil.sanitize("COM1"))
        assertEquals("_LPT9.txt", FilenameUtil.sanitize("LPT9.txt"))
        // Not reserved
        assertEquals("COM0", FilenameUtil.sanitize("COM0"))
        assertEquals("CONSOLE", FilenameUtil.sanitize("CONSOLE"))
    }

    @Test
    fun `blank and unusable input yields null so the caller can fall back`() {
        assertNull(FilenameUtil.sanitize(null))
        assertNull(FilenameUtil.sanitize(""))
        assertNull(FilenameUtil.sanitize("   "))
        assertNull(FilenameUtil.sanitize("..."))
    }

    @Test
    fun `long names are truncated to the base limit`() {
        val sanitized = FilenameUtil.sanitize("a".repeat(500))

        assertEquals(FilenameUtil.MAX_BASE_LENGTH, sanitized?.length)
    }

    @Test
    fun `truncation leaves room for the suffix within the 255 byte component limit`() {
        // The worst case the supported range allows: every character costs two bytes
        val sanitized = FilenameUtil.sanitize(accentedCharacter.repeat(200))!!

        // Longest name MediaDownloadWorker can build out of the sanitized base
        val assembled = "${sanitized}_20260725_120000_video.webm"

        assertTrue(assembled.toByteArray(Charsets.UTF_8).size <= 255)
    }
}
