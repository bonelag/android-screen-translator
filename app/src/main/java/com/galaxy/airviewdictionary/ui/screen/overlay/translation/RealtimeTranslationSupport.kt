package com.galaxy.airviewdictionary.ui.screen.overlay.translation

import android.graphics.Bitmap
import android.graphics.Rect
import com.galaxy.airviewdictionary.data.local.vision.WritingDirection
import com.galaxy.airviewdictionary.data.local.vision.model.Char
import com.galaxy.airviewdictionary.data.local.vision.model.Line
import com.galaxy.airviewdictionary.data.local.vision.model.Paragraph
import com.galaxy.airviewdictionary.data.local.vision.model.Transaction as VisionTransaction
import com.galaxy.airviewdictionary.data.local.vision.model.Word
import kotlin.math.max

data class SelectionCapture(
    val bitmap: Bitmap,
    val sourceRect: Rect,
)

private data class SelectedVisionSlice(
    val paragraphs: List<Paragraph>,
    val lines: List<Line>,
    val wordLines: List<List<Word>>,
)

fun createSelectionCapture(
    originalBitmap: Bitmap,
    selectionRect: Rect,
    paddingPx: Int,
    minimumEdgePx: Int,
): SelectionCapture {
    val bitmapBounds = Rect(0, 0, max(1, originalBitmap.width), max(1, originalBitmap.height))
    val safeSelection = Rect(selectionRect).apply {
        intersect(bitmapBounds)
    }
    if (safeSelection.isEmpty) {
        return SelectionCapture(
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            sourceRect = Rect(0, 0, 1, 1),
        )
    }

    val expandedRect = Rect(
        safeSelection.left - paddingPx,
        safeSelection.top - paddingPx,
        safeSelection.right + paddingPx,
        safeSelection.bottom + paddingPx,
    )

    val widthShortfall = max(0, minimumEdgePx - expandedRect.width())
    val heightShortfall = max(0, minimumEdgePx - expandedRect.height())
    expandedRect.inset(-widthShortfall / 2, -heightShortfall / 2)
    expandedRect.right += widthShortfall % 2
    expandedRect.bottom += heightShortfall % 2

    val captureRect = Rect(
        expandedRect.left.coerceIn(0, bitmapBounds.right - 1),
        expandedRect.top.coerceIn(0, bitmapBounds.bottom - 1),
        expandedRect.right.coerceIn(1, bitmapBounds.right),
        expandedRect.bottom.coerceIn(1, bitmapBounds.bottom),
    ).apply {
        if (width() <= 0) {
            right = (left + 1).coerceAtMost(bitmapBounds.right)
        }
        if (height() <= 0) {
            bottom = (top + 1).coerceAtMost(bitmapBounds.bottom)
        }
    }

    return SelectionCapture(
        bitmap = Bitmap.createBitmap(
            originalBitmap,
            captureRect.left,
            captureRect.top,
            captureRect.width(),
            captureRect.height(),
        ),
        sourceRect = captureRect,
    )
}

fun offsetVisionTransactionToScreen(
    transaction: VisionTransaction,
    offsetX: Int,
    offsetY: Int,
): VisionTransaction {
    val translatedParagraphs = transaction.paragraphs.map { paragraph ->
        Paragraph(
            lines = paragraph.lines.map { line ->
                Line(
                    words = line.words.map { word ->
                        Word(
                            boundingBox = Rect(word.boundingBox).apply { offset(offsetX, offsetY) },
                            representation = word.representation,
                            writingDirection = word.writingDirection,
                            chars = word.chars.map { char ->
                                Char(
                                    boundingBox = Rect(char.boundingBox).apply { offset(offsetX, offsetY) },
                                    representation = char.representation,
                                    writingDirection = char.writingDirection,
                                )
                            },
                            presetFontHeight = word.fontHeight,
                        )
                    }.toMutableList(),
                    writingDirection = line.writingDirection,
                ).apply {
                    fontColor = line.fontColor
                    backgroundColor = line.backgroundColor
                }
            }.toMutableList(),
            writingDirection = paragraph.writingDirection,
        ).apply {
            hasParallelLines = paragraph.hasParallelLines
        }
    }

    return transaction.copy(paragraphs = translatedParagraphs)
}

fun extractSelectionWord(
    transaction: VisionTransaction,
    selectionRect: Rect,
): Word? {
    val slice = getSelectedVisionSlice(transaction, selectionRect)
    val representation = when {
        slice.wordLines.isNotEmpty() -> {
            slice.wordLines.joinToString(separator = "\n") { words ->
                words.joinToString(separator = " ") { it.representation }
            }
        }

        slice.lines.isNotEmpty() -> {
            slice.lines.joinToString(separator = "\n") { it.representation }
        }

        slice.paragraphs.isNotEmpty() -> {
            slice.paragraphs.joinToString(separator = "\n") { it.representation }
        }

        else -> transaction.text.text.trim()
    }.trim()

    if (representation.isBlank()) {
        return null
    }

    val relevantRects = when {
        slice.wordLines.isNotEmpty() -> slice.wordLines.flatten().map { it.boundingBox }
        slice.lines.isNotEmpty() -> slice.lines.map { it.boundingBox }
        slice.paragraphs.isNotEmpty() -> slice.paragraphs.map { it.boundingBox }
        else -> emptyList()
    }

    val boundingBox = relevantRects.reduceOrNull { acc, rect ->
        Rect(
            minOf(acc.left, rect.left),
            minOf(acc.top, rect.top),
            maxOf(acc.right, rect.right),
            maxOf(acc.bottom, rect.bottom),
        )
    } ?: return null

    val fontHeight = when {
        slice.lines.isNotEmpty() -> slice.lines.map { it.fontHeight }.average()
        slice.wordLines.isNotEmpty() -> slice.wordLines.flatten().map { it.fontHeight }.average()
        else -> transaction.paragraphs.flatMap { it.lines }.map { it.fontHeight }.average()
    }

    val writingDirection = slice.lines.firstOrNull()?.writingDirection
        ?: slice.paragraphs.firstOrNull()?.writingDirection
        ?: transaction.mostFrequentWritingDirection()
        ?: WritingDirection.LTR

    return Word(
        boundingBox = boundingBox,
        representation = representation,
        writingDirection = writingDirection,
        chars = emptyList(),
        presetFontHeight = if (fontHeight.isNaN()) null else fontHeight,
    )
}

fun extractSelectionText(
    transaction: VisionTransaction,
    selectionRect: Rect,
): String {
    return extractSelectionWord(transaction, selectionRect)?.representation
        ?: transaction.text.text.trim()
}

fun computeSelectedContentBounds(
    transaction: VisionTransaction,
    selectionRect: Rect,
): Rect? {
    val slice = getSelectedVisionSlice(transaction, selectionRect)
    val rects = when {
        slice.lines.isNotEmpty() -> slice.lines.map { it.boundingBox }
        slice.paragraphs.isNotEmpty() -> slice.paragraphs.map { it.boundingBox }
        else -> emptyList()
    }
    return rects.reduceOrNull { acc, rect ->
        Rect(
            minOf(acc.left, rect.left),
            minOf(acc.top, rect.top),
            maxOf(acc.right, rect.right),
            maxOf(acc.bottom, rect.bottom),
        )
    }
}

private fun getSelectedVisionSlice(
    transaction: VisionTransaction,
    selectionRect: Rect,
): SelectedVisionSlice {
    val selectedParagraphs = transaction.paragraphs.filter { Rect.intersects(it.boundingBox, selectionRect) }
    val selectedLines = selectedParagraphs.flatMap { paragraph ->
        paragraph.lines.filter { Rect.intersects(it.boundingBox, selectionRect) }
    }
    val selectedWordLines = selectedLines.mapNotNull { line ->
        line.words.filter { Rect.intersects(it.boundingBox, selectionRect) }.takeIf { it.isNotEmpty() }
    }
    return SelectedVisionSlice(
        paragraphs = selectedParagraphs,
        lines = selectedLines,
        wordLines = selectedWordLines,
    )
}
