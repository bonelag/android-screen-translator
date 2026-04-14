package com.galaxy.airviewdictionary.data.local.vision.model

import android.graphics.Rect
import com.galaxy.airviewdictionary.data.local.vision.WritingDirection
import com.galaxy.airviewdictionary.extensions._unionWith


/**
 * 어러 개의 [Line] 으로 이루어진 문장
 */
data class Sentence(
    val lines: MutableList<Line>,
    override val writingDirection: WritingDirection,
    override val fontHeight: Double
) : VisionText {

    private var boundingBoxCache: Rect? = null
    private var linesHashCodeCache: Int? = null

    override val boundingBox: Rect
        get() {
            val currentWordsHashCode = lines.hashCode()
            if (boundingBoxCache == null || linesHashCodeCache != currentWordsHashCode) {
                boundingBoxCache = if (lines.isEmpty()) {
                    Rect()
                } else {
                    lines.map { it.boundingBox }.reduce { acc, rect -> acc._unionWith(rect) }
                }
                linesHashCodeCache = currentWordsHashCode
            }
            return boundingBoxCache!!
        }

    /**
     * lines 가 형성하는 다각형.
     */
    val boundingPolygon: Polygon
        get() {
            return Polygon.fromRects(lines.map { it.boundingBox })
        }

    override val representation: String
        get() = lines.joinToString(separator = " ") { it.representation }
}