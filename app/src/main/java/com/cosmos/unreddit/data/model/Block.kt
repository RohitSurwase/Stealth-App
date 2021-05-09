package com.cosmos.unreddit.data.model

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import androidx.core.view.setPadding
import com.cosmos.unreddit.R
import com.cosmos.unreddit.ui.common.widget.RedditTextView
import com.cosmos.unreddit.util.ClickableMovementMethod

sealed class Block {
    data class TextBlock(val text: CharSequence) : Block()

    class TableBlock : Block() {
        private val rows = mutableListOf<Row>()

        fun addRow(row: Row) {
            rows.add(row)
        }

        fun getTableLayout(
            context: Context,
            clickableMovementMethod: ClickableMovementMethod
        ): TableLayout {
            val padding = context.resources.getDimension(R.dimen.table_padding).toInt()

            val tableLayout = TableLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, padding, 0, padding)
            }

            val rowParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            val colParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            for (row in rows) {
                val tableRow = TableRow(context).apply {
                    layoutParams = rowParams
                }

                for (column in row.columns) {
                    val redditTextView = RedditTextView(context).apply {
                        layoutParams = colParams
                        gravity = column.gravity
                        text = column.text
                        movementMethod = clickableMovementMethod
                        setPadding(padding)
                    }
                    tableRow.addView(redditTextView)
                }
                tableLayout.addView(tableRow)
            }
            return tableLayout
        }

        class Row(val isHeader: Boolean = false) {
            private val _columns = mutableListOf<Column>()
            val columns: List<Column> get() = _columns

            fun addColumn(text: CharSequence, gravity: Int = Gravity.START) {
                _columns.add(Column(text, gravity))
            }
        }

        data class Column(
            val text: CharSequence,
            val gravity: Int
        )
    }
}
