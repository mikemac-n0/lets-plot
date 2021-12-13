/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.builder.tooltip

import jetbrains.datalore.base.geometry.DoubleRectangle
import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.base.values.Color
import jetbrains.datalore.plot.base.interact.TipLayoutHint
import jetbrains.datalore.plot.base.render.svg.SvgComponent
import jetbrains.datalore.plot.base.render.svg.TextLabel
import jetbrains.datalore.plot.builder.interact.TooltipSpec
import jetbrains.datalore.plot.builder.presentation.Defaults.Common.Tooltip.DARK_TEXT_COLOR
import jetbrains.datalore.plot.builder.presentation.Defaults.Common.Tooltip.DATA_TOOLTIP_FONT_SIZE
import jetbrains.datalore.plot.builder.presentation.Defaults.Common.Tooltip.H_CONTENT_PADDING
import jetbrains.datalore.plot.builder.presentation.Defaults.Common.Tooltip.LABEL_VALUE_INTERVAL
import jetbrains.datalore.plot.builder.presentation.Defaults.Common.Tooltip.LINE_INTERVAL
import jetbrains.datalore.plot.builder.presentation.Defaults.Common.Tooltip.MAX_POINTER_FOOTING_LENGTH
import jetbrains.datalore.plot.builder.presentation.Defaults.Common.Tooltip.POINTER_FOOTING_TO_SIDE_LENGTH_RATIO
import jetbrains.datalore.plot.builder.presentation.Defaults.Common.Tooltip.V_CONTENT_PADDING
import jetbrains.datalore.plot.builder.tooltip.TooltipBox.Orientation.HORIZONTAL
import jetbrains.datalore.plot.builder.tooltip.TooltipBox.Orientation.VERTICAL
import jetbrains.datalore.plot.builder.tooltip.TooltipBox.PointerDirection.*
import jetbrains.datalore.vis.svg.*
import jetbrains.datalore.vis.svg.SvgPathDataBuilder
import jetbrains.datalore.vis.svg.SvgPathElement
import jetbrains.datalore.vis.svg.SvgSvgElement
import kotlin.math.max
import kotlin.math.min

class TooltipBox(
    newStylePointer: Boolean = false
): SvgComponent() {
    enum class Orientation {
        VERTICAL,
        HORIZONTAL
    }

    internal enum class PointerDirection {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    val contentRect get() = DoubleRectangle.span(DoubleVector.ZERO, myTextBox.dimension)

    private val myPointerBox = if (newStylePointer) NewStylePointerBox() else PointerBox()
    private val myTextBox = TextBox()

    internal val pointerDirection get() = myPointerBox.pointerDirection // for tests

    override fun buildComponent() {
        add(myPointerBox)
        add(myTextBox)
    }

    fun update(
        fillColor: Color,
        textColor: Color,
        borderColor: Color,
        strokeWidth: Double,
        lines: List<TooltipSpec.Line>,
        style: String,
        tooltipMinWidth: Double? = null,
        pointerStyle: TipLayoutHint.PointerStyle? = null
    ) {
        addClassName(style)
        myTextBox.update(
            lines,
            labelTextColor = DARK_TEXT_COLOR,
            valueTextColor = textColor,
            tooltipMinWidth
        )
        myPointerBox.updateStyle(fillColor, borderColor, strokeWidth, pointerStyle)
    }

    internal fun setPosition(
        tooltipCoord: DoubleVector,
        pointerCoord: DoubleVector,
        orientation: Orientation,
        stemLength: TipLayoutHint.StemLength
    ) {
        myPointerBox.update(pointerCoord.subtract(tooltipCoord), orientation, stemLength.value)
        moveTo(tooltipCoord)
    }

    open inner class PointerBox : SvgComponent() {
        internal val myPointerPath = SvgPathElement()
        internal var pointerDirection: PointerDirection? = null

        override fun buildComponent() {
            add(myPointerPath)
        }

        internal fun updateStyle(fillColor: Color, borderColor: Color, strokeWidth: Double, pointerStyle: TipLayoutHint.PointerStyle?) {
            myPointerPath.apply {
                strokeColor().set(borderColor)
                strokeOpacity().set(strokeWidth)
                fillColor().set(fillColor)
            }
            updatePointerStyle(pointerStyle)
        }

        protected open fun updatePointerStyle(pointerStyle: TipLayoutHint.PointerStyle?) {
        }

        protected fun calcPointerDirection(pointerCoord: DoubleVector, orientation: Orientation) {
            pointerDirection = when (orientation) {
                HORIZONTAL -> when {
                    pointerCoord.x < contentRect.left -> LEFT
                    pointerCoord.x > contentRect.right -> RIGHT
                    else -> null
                }
                VERTICAL -> when {
                    pointerCoord.y > contentRect.bottom -> DOWN
                    pointerCoord.y < contentRect.top -> UP
                    else -> null
                }
            }
        }

        internal open fun update(pointerCoord: DoubleVector, orientation: Orientation, stemLen: Double) {
            calcPointerDirection(pointerCoord, orientation)

            val vertFootingIndent = -calculatePointerFootingIndent(contentRect.height)
            val horFootingIndent = calculatePointerFootingIndent(contentRect.width)

            myPointerPath.d().set(
                SvgPathDataBuilder().apply {
                    with(contentRect) {

                        fun lineToIf(p: DoubleVector, isTrue: Boolean) { if (isTrue) lineTo(p) }

                        // start point
                        moveTo(right, bottom)

                        // right side
                        lineTo(right, bottom + vertFootingIndent)
                        lineToIf(pointerCoord, pointerDirection == RIGHT)
                        lineTo(right, top - vertFootingIndent)
                        lineTo(right, top)

                        // top side
                        lineTo(right - horFootingIndent, top)
                        lineToIf (pointerCoord, pointerDirection == UP)
                        lineTo(left + horFootingIndent, top)
                        lineTo(left, top)

                        // left side
                        lineTo(left, top - vertFootingIndent)
                        lineToIf (pointerCoord, pointerDirection == LEFT)
                        lineTo(left, bottom + vertFootingIndent)
                        lineTo(left, bottom)

                        // bottom
                        lineTo(left + horFootingIndent, bottom)
                        lineToIf (pointerCoord, pointerDirection == DOWN)
                        lineTo(right - horFootingIndent, bottom)
                        lineTo(right, bottom)
                    }
                }.build()
            )
        }

        private fun calculatePointerFootingIndent(sideLength: Double): Double {
            val footingLength = min(sideLength * POINTER_FOOTING_TO_SIDE_LENGTH_RATIO, MAX_POINTER_FOOTING_LENGTH)
            return (sideLength - footingLength) / 2
        }
    }

    private inner class NewStylePointerBox : PointerBox() {
        private var myPointerStyle: TipLayoutHint.PointerStyle = TipLayoutHint.PointerStyle()
        val borderSize = 1.0
        val whiteBackLineSize = 3.0

        override fun updatePointerStyle(pointerStyle: TipLayoutHint.PointerStyle?) {
            if (pointerStyle != null) {
                myPointerStyle = pointerStyle
            }
        }

        override fun update(pointerCoord: DoubleVector, orientation: Orientation, stemLen: Double) {
            calcPointerDirection(pointerCoord, orientation)

            // tooltip rectangle (todo add shadows)
            myPointerPath.apply {
                d().set(
                    SvgPathDataBuilder().apply {
                        with(contentRect) {
                            moveTo(left, bottom)
                            lineTo(right, bottom)
                            lineTo(right, top)
                            lineTo(left, top)
                            lineTo(left, bottom)
                        }
                    }.build()
                )
            }

            // path to the highlight point

            val pointBorder = if (!myPointerStyle.isTransparent()) {
                 pointerCoord
            } else {
                when (pointerDirection) {
                    LEFT -> pointerCoord.subtract(DoubleVector(myPointerStyle.size, 0.0))
                    RIGHT -> pointerCoord.add(DoubleVector(myPointerStyle.size, 0.0))
                    UP -> pointerCoord.add(DoubleVector(0.0, myPointerStyle.size))
                    DOWN -> pointerCoord.subtract(DoubleVector(0.0, myPointerStyle.size))
                    null -> pointerCoord
                }
            }

            //  start pointer from a middle of a box, not from a corner, increase length of the pointer.
            val fromRectPoint = when (pointerDirection) {
                LEFT -> DoubleVector(contentRect.left, contentRect.center.y)
                RIGHT -> DoubleVector(contentRect.right, contentRect.center.y)
                UP -> DoubleVector(contentRect.center.x, contentRect.top)
                DOWN -> DoubleVector(contentRect.center.x, contentRect.bottom)
                null -> TODO()
            }
            val lineLen = stemLen / 1.5
            val middlePoint = when (pointerDirection) {
                LEFT -> fromRectPoint.subtract(DoubleVector(lineLen, 0.0))
                RIGHT -> fromRectPoint.add(DoubleVector(lineLen, 0.0))
                UP -> fromRectPoint.subtract(DoubleVector(0.0, lineLen))
                DOWN -> fromRectPoint.add(DoubleVector(0.0, lineLen))
                null -> TODO()
            }

            val svgPathData = SvgPathDataBuilder().apply {
                moveTo(fromRectPoint)
                lineTo(middlePoint)
                /// from horizontal/vertical
                moveTo(middlePoint)
                lineTo(pointBorder)
            }.build()

            // white path to point
            SvgPathElement().apply {
                add(this)
                strokeColor().set(Color.WHITE)
                strokeWidth().set(whiteBackLineSize)
                d().set(svgPathData)
            }

            // path to point
            SvgPathElement().apply {
                add(this)
                strokeColor().set(Color.BLACK)
                strokeWidth().set(borderSize)
                d().set(svgPathData)
            }

            // white border for highlight point
            if (myPointerStyle.isTransparent()) {
                SvgCircleElement(pointerCoord, myPointerStyle.size).apply {
                    add(this)
                    fillOpacity().set(0.0)
                    strokeWidth().set(whiteBackLineSize)
                    strokeColor().set(Color.WHITE)
                }
            }
            // highlight point
            SvgCircleElement(pointerCoord, myPointerStyle.size).apply {
                add(this)
                if (myPointerStyle.isTransparent()) {
                    fillOpacity().set(0.0)
                    strokeWidth().set(borderSize)
                } else {
                    fillColor().set(myPointerStyle.fillColor)
                }
                strokeColor().set(myPointerStyle.strokeColor)
            }
        }
    }

    private inner class TextBox : SvgComponent() {
        private val myLines = SvgSvgElement().apply {
            x().set(0.0)
            y().set(0.0)
            width().set(0.0)
            height().set(0.0)
        }
        private val myContent = SvgSvgElement().apply {
            x().set(0.0)
            y().set(0.0)
            width().set(0.0)
            height().set(0.0)
        }

        val dimension get() = myContent.run { DoubleVector(width().get()!!, height().get()!!) }

        override fun buildComponent() {
            add(myContent)
            myContent.children().add(myLines)
        }

        internal fun update(
            lines: List<TooltipSpec.Line>,
            labelTextColor: Color,
            valueTextColor: Color,
            tooltipMinWidth: Double?
        ) {
            val components: List<Pair<TextLabel?, TextLabel>> = lines.map { line ->
                Pair(
                    line.label?.let(::TextLabel),
                    TextLabel(line.value)
                )
            }
            // for labels
            components.onEach { (labelComponent, _) ->
                if (labelComponent != null) {
                    labelComponent.textColor().set(labelTextColor)
                    myLines.children().add(labelComponent.rootGroup)
                }
            }
            // for values
            components.onEach { (_, valueComponent) ->
                valueComponent.textColor().set(valueTextColor)
                myLines.children().add(valueComponent.rootGroup)
            }

            // bBoxes
            fun getBBox(text: String?, textLabel: TextLabel?): DoubleRectangle? {
                if (textLabel == null || text.isNullOrBlank()) {
                    // also for blank string - Batik throws an exception for a text element with a blank string
                    return null
                }
                return textLabel.rootGroup.bBox
            }
            val rawBBoxes = lines.zip(components).map { (line, component) ->
                val (labelComponent, valueComponent) = component
                Pair(
                    getBBox(line.label, labelComponent),
                    getBBox(line.value, valueComponent)
                )
            }

            // max label width - all labels will be aligned to this value
            val maxLabelWidth = rawBBoxes.maxOf { (labelBbox) -> labelBbox?.width ?: 0.0 }

            // max line height - will be used as default height for empty string
            val defaultLineHeight = rawBBoxes.flatMap { it.toList().filterNotNull() }
                .maxOfOrNull(DoubleRectangle::height) ?: DATA_TOOLTIP_FONT_SIZE.toDouble()

            val labelWidths = lines.map { line ->
                when {
                    line.label == null -> {
                        // label null - the value component will be centered
                        0.0
                    }
                    line.label!!.isEmpty() -> {
                        // label is not null, but empty - add space for the label, the value will be moved to the right
                        maxLabelWidth
                    }
                    else -> {
                        // align the label width to the maximum and add interval between label and value
                        maxLabelWidth + LABEL_VALUE_INTERVAL
                    }
                }
            }
            val valueWidths = rawBBoxes.map { (_, valueBBox) -> valueBBox?.dimension?.x ?: 0.0 }
            val lineWidths = labelWidths.zip(valueWidths)

            // max line width
            val maxLineWidth = lineWidths.maxOf { (labelWidth, valueWidth) ->
                max(tooltipMinWidth ?: 0.0, labelWidth + valueWidth)
            }

            // prepare bbox
            val lineBBoxes = rawBBoxes.zip(lineWidths).map { (bBoxes, width) ->
                val (labelBBox, valueBBox) = bBoxes
                val (labelWidth, valueWidth) = width

                val labelDimension = DoubleVector(
                    labelWidth,
                    labelBBox?.run { height + top } ?: 0.0
                )
                val valueDimension = DoubleVector(
                    valueWidth,
                    valueBBox?.run { height + top } ?: if (labelBBox == null) {
                        // it's the empty line - use default height
                        defaultLineHeight
                    } else {
                        0.0
                    }
                )
                Pair(
                    DoubleRectangle(labelBBox?.origin ?: DoubleVector.ZERO, labelDimension),
                    DoubleRectangle(valueBBox?.origin ?: DoubleVector.ZERO, valueDimension)
                )
            }

            val textSize = components
                .zip(lineBBoxes)
                .fold(DoubleVector.ZERO) { textDimension, (lineInfo, bBoxes) ->
                    val (labelComponent,valueComponent) = lineInfo
                    val (labelBBox, valueBBox) = bBoxes

                    // bBox.top is negative baseline of the text.
                    // Can't use bBox.height:
                    //  - in Batik it is close to the abs(bBox.top)
                    //  - in JavaFx it is constant = fontSize
                    val yPosition = textDimension.y - min(valueBBox.top, labelBBox.top)
                    valueComponent.y().set(yPosition)
                    labelComponent?.y()?.set(yPosition)

                    when {
                        labelComponent != null && labelBBox.dimension.x > 0 -> {
                            // Move label to the left border, value - to the right

                            // Again works differently in Batik(some positive padding) and JavaFX (always zero)
                            labelComponent.x().set(-labelBBox.left)

                            valueComponent.x().set(maxLineWidth)
                            valueComponent.setHorizontalAnchor(TextLabel.HorizontalAnchor.RIGHT)
                        }
                        valueBBox.dimension.x == maxLineWidth -> {
                            // No label and value's width is equal to the total width => centered
                            // Again works differently in Batik(some positive padding) and JavaFX (always zero)
                            valueComponent.x().set(-valueBBox.left)
                        }
                        else -> {
                            // Move value to the center
                            valueComponent.setHorizontalAnchor(TextLabel.HorizontalAnchor.MIDDLE)
                            valueComponent.x().set(maxLineWidth / 2)
                        }
                    }
                    DoubleVector(
                        x = maxLineWidth,
                        y = valueComponent.y().get()!! + max(
                            valueBBox.height,
                            labelBBox.height
                        ) + LINE_INTERVAL
                    )
                }
                .subtract(DoubleVector(0.0, LINE_INTERVAL)) // remove LINE_INTERVAL from last line

            myLines.apply {
                x().set(H_CONTENT_PADDING)
                y().set(V_CONTENT_PADDING)
                width().set(textSize.x)
                height().set(textSize.y)
            }

            myContent.apply {
                width().set(textSize.x + H_CONTENT_PADDING * 2)
                height().set(textSize.y + V_CONTENT_PADDING * 2)
            }
        }
    }
}