/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@schuermann.eu>
 * Copyright (C) 2013-2015 Juha Kuitunen
 * Copyright (C) 2013 Mohammed Lakkadshaw
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cosmos.unreddit.parser

import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.CODE_TAG
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.ESCAPE_TAG
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.HR_TAG
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.LIST_TAG
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.ORDERED_LIST_TAG
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.QUOTE_TAG
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.SPOILER_TAG
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.SUPERSCRIPT_TAG
import com.cosmos.unreddit.parser.RedditTagHandler.Tag.UNORDERED_LIST_TAG
import org.xml.sax.XMLReader
import java.util.*

class RedditTagHandler : Html.TagHandler {

    /**
     * Newer versions of the Android SDK's {@link Html.TagHandler} handles &lt;ul&gt; and &lt;li&gt;
     * tags itself which means they never get delegated to this class. We want to handle the tags
     * ourselves so before passing the string html into Html.fromHtml(), we can use this method to
     * replace the &lt;ul&gt; and &lt;li&gt; tags with tags of our own.
     *
     * @param html String containing HTML, for example: "<b>Hello world!</b>"
     * @return html with replaced <ul> and <li> tags
     * @see <a href="https://github.com/android/platform_frameworks_base/commit/8b36c0bbd1503c61c111feac939193c47f812190">Specific Android SDK Commit</a>
     */
    fun overrideTags(html: String): String {
        val escapedHtml = "<$ESCAPE_TAG/>$html"
        return escapedHtml.replace("<ul", "<$UNORDERED_LIST_TAG")
            .replace("</ul>", "</$UNORDERED_LIST_TAG>")
            .replace("<ol", "<$ORDERED_LIST_TAG")
            .replace("</ol>", "</$ORDERED_LIST_TAG>")
            .replace("<li", "<$LIST_TAG")
            .replace("</li>", "</$LIST_TAG>")
            .replace("<sup", "<$SUPERSCRIPT_TAG")
            .replace("</sup>", "</$SUPERSCRIPT_TAG>")
            .replace("<blockquote", "<$QUOTE_TAG")
            .replace("</blockquote>", "</$QUOTE_TAG>")
            .replace(SPOILER_REGEX) {
                "<$SPOILER_TAG>${it.groupValues[1]}</$SPOILER_TAG>"
            }
    }

    /**
     * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list
     * and on top of Stack is the most nested list
     */
    private val lists = Stack<String>()

    /**
     * Tracks indexes of ordered lists so that after a nested list ends
     * we can continue with correct index of outer list
     */
    private val olNextIndex = Stack<Int>()

    override fun handleTag(
        opening: Boolean,
        tag: String,
        output: Editable,
        xmlReader: XMLReader
    ) {
        when (Tag.fromTag(tag)) {
            QUOTE_TAG -> handleQuoteTag(opening, output)
            CODE_TAG -> handleTag(opening, output, Code(), CodeSpan())
            HR_TAG -> handleHrTag(opening, output)
            UNORDERED_LIST_TAG -> {
                if (opening) {
                    lists.push(tag)
                } else {
                    lists.pop()
                }
            }
            ORDERED_LIST_TAG -> {
                if (opening) {
                    lists.push(tag)
                    olNextIndex.push(1)
                } else {
                    lists.pop()
                    olNextIndex.pop()
                }
            }
            LIST_TAG -> handleListTag(opening, output)
            SPOILER_TAG -> handleTag(opening, output, Spoiler(), SpoilerSpan())
            SUPERSCRIPT_TAG -> handleTag(opening, output, Superscript(), SuperscriptSpan())
            else -> {
                // ignore
            }
        }
    }

    private fun handleQuoteTag(opening: Boolean, text: Editable) {
        if (opening) {
            addNewLine(text)
            start(text, Quote())
        } else {
            addNewLine(text)
            val obj: Any? = getLast(text, Quote::class.java)
            if (obj != null) {
                var where = text.getSpanStart(obj)
                text.removeSpan(obj)
                val len = text.length
                if (where != len) {
                    if (text[where] == '\n') {
                        where++
                    }
                    text.setSpan(
                        BlockquoteSpan(),
                        where,
                        len,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    /**
     * @see <a href="https://stackoverflow.com/a/43750749">StackOverflow answer</a>
     */
    private fun handleHrTag(opening: Boolean, text: Editable) {
        val placeholder = "-"
        if (opening) {
            text.insert(text.length, placeholder)
        } else {
            text.setSpan(
                BreakSpan(),
                text.length - placeholder.length,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun handleListTag(opening: Boolean, text: Editable) {
        if (opening) {
            addNewLine(text)
            if (lists.isNotEmpty()) {
                val parentList = lists.peek()
                if (parentList.equals(ORDERED_LIST_TAG.tagName, ignoreCase = true)) {
                    start(text, Ol())
                    olNextIndex.push(olNextIndex.pop() + 1)
                } else if (parentList.equals(UNORDERED_LIST_TAG.tagName, ignoreCase = true)) {
                    start(text, Ul())
                }
            }
        } else {
            if (lists.isNotEmpty()) {
                val parentList = lists.peek()
                if (parentList.equals(UNORDERED_LIST_TAG.tagName, ignoreCase = true)) {
                    addNewLine(text)
                    // Nested BulletSpans increases distance between bullet and text, so we must prevent it.
                    val indent = getListIndent(BulletSpan(LIST_INDENT))
                    end(
                        text,
                        Ul::class.java,
                        LeadingMarginSpan.Standard(LIST_ITEM_INDENT * (lists.size - 1)),
                        BulletSpan(indent)
                    )
                } else if (parentList.equals(ORDERED_LIST_TAG.tagName, ignoreCase = true)) {
                    addNewLine(text)

                    // Nested NumberSpans increases distance between number and text, so we must prevent it.
                    val indent = getListIndent(
                        NumberSpan(LIST_INDENT, olNextIndex.lastElement() - 1)
                    )
                    end(
                        text,
                        Ol::class.java,
                        LeadingMarginSpan.Standard(LIST_ITEM_INDENT * (lists.size - 1)),
                        NumberSpan(indent, olNextIndex.lastElement() - 1)
                    )
                }
            }
        }
    }

    private fun getListIndent(span: BulletSpan): Int {
        var newIndent = LIST_INDENT
        if (lists.size > 1) {
            newIndent -= span.getLeadingMargin(true)
            if (lists.size > 2) {
                newIndent -= (lists.size - 2) * LIST_ITEM_INDENT
            }
        }
        return newIndent
    }

    private fun handleTag(opening: Boolean, text: Editable, mark: Any, repl: Any) {
        if (opening) {
            start(text, mark)
        } else {
            end(text, mark::class.java, repl)
        }
    }

    private fun addNewLine(text: Editable) {
        if (text.isNotEmpty() && text[text.length - 1] != '\n') {
            text.append("\n")
        }
    }

    /**
     * @see Html
     */
    private fun setSpanFromMark(text: Spannable, mark: Any, vararg spans: Any) {
        val where = text.getSpanStart(mark)
        text.removeSpan(mark)
        val len = text.length
        if (where != len) {
            for (span in spans) {
                text.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    /**
     * From [Html]
     */
    private fun start(text: Editable, mark: Any) {
        val len = text.length
        text.setSpan(mark, len, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    /**
     * From [Html]
     */
    private fun end(text: Editable, kind: Class<*>, vararg repl: Any) {
        val obj: Any? = getLast(text, kind)
        if (obj != null) {
            setSpanFromMark(text, obj, *repl)
        }
    }

    /**
     * From [Html]
     */
    private fun <T> getLast(text: Spanned, kind: Class<T>): T? {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        val objs = text.getSpans(0, text.length, kind)
        return if (objs.isEmpty()) {
            null
        } else {
            objs[objs.size - 1]
        }
    }

    private class Quote
    private class Code
    private class Ol
    private class Ul
    private class Spoiler
    private class Superscript

    private enum class Tag(val tagName: String) {
        ESCAPE_TAG("escape_tag"),
        QUOTE_TAG("blockquote_tag"),
        CODE_TAG("code"),
        HR_TAG("hr"),
        UNORDERED_LIST_TAG("ul_tag"),
        ORDERED_LIST_TAG("ol_tag"),
        LIST_TAG("li_tag"),
        SPOILER_TAG("spoiler"),
        SUPERSCRIPT_TAG("sup_tag");

        override fun toString(): String {
            return this.tagName
        }

        companion object {
            fun fromTag(tag: String): Tag? {
                return when (tag) {
                    ESCAPE_TAG.tagName -> ESCAPE_TAG
                    QUOTE_TAG.tagName -> QUOTE_TAG
                    CODE_TAG.tagName -> CODE_TAG
                    HR_TAG.tagName -> HR_TAG
                    UNORDERED_LIST_TAG.tagName -> UNORDERED_LIST_TAG
                    ORDERED_LIST_TAG.tagName -> ORDERED_LIST_TAG
                    LIST_TAG.tagName -> LIST_TAG
                    SPOILER_TAG.tagName -> SPOILER_TAG
                    SUPERSCRIPT_TAG.tagName -> SUPERSCRIPT_TAG
                    else -> null
                }
            }
        }
    }

    companion object {
        private val SPOILER_REGEX = Regex(
            "<span class=\"md-spoiler-text\">(.+?)</span>",
            RegexOption.DOT_MATCHES_ALL
        )

        private const val LIST_INDENT = 25
        private const val LIST_ITEM_INDENT = LIST_INDENT * 2
    }
}
