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
import kotlin.math.max
import kotlin.math.min

class TooltipBox: SvgComponent() {
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

    private val myPointerBox = PointerBox()
    private val myTextBox = TextBox()

    internal val pointerDirection get() = myPointerBox.pointerDirection // for tests

    private var useNewPointerStyleForTooltip = false
    private var addDataPointColorElement = false
    private val additionalIndentInContentRect get() = if (addDataPointColorElement) H_CONTENT_PADDING * 2 else 0.0

    override fun buildComponent() {
        add(myPointerBox)
        add(myTextBox)
    }

    fun update(
        fillColor: Color,
        textColor: Color,
        borderColor: Color,
        dataPointColor: Color?,
        strokeWidth: Double,
        lines: List<TooltipSpec.Line>,
        style: String,
        tooltipMinWidth: Double? = null,
        pointerStyle: TipLayoutHint.PointerStyle? = null,
        useNewPointerStyle: Boolean
    ) {
        useNewPointerStyleForTooltip = useNewPointerStyle
        addDataPointColorElement = useNewPointerStyle && dataPointColor != null

        addClassName(style)
        myTextBox.update(
            lines,
            labelTextColor = DARK_TEXT_COLOR,
            valueTextColor = textColor,
            tooltipMinWidth
        )
        myPointerBox.updateStyle(fillColor, borderColor, dataPointColor, strokeWidth, pointerStyle)
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
        internal var pointerDirection: PointerDirection? = null

        private val myBoxPath = SvgPathElement()

        // additional elements for the new style tooltips (pointer path, highlight point, color marker)
        private val myNewStyleTooltip = RetainableComponents(::NewStylePointerBox, rootGroup)

        override fun buildComponent() {
            add(myBoxPath)
        }

        private fun getNewStyleTooltipElements(): NewStylePointerBox? {
            return if (useNewPointerStyleForTooltip) {
                myNewStyleTooltip.provide(1).first()
            } else {
                null
            }
        }
        internal fun updateStyle(
            fillColor: Color,
            borderColor: Color,
            dataPointColor: Color?,
            strokeWidth: Double,
            pointerStyle: TipLayoutHint.PointerStyle?
        ) {
            myBoxPath.apply {
                strokeColor().set(borderColor)
                strokeOpacity().set(strokeWidth)
                fillColor().set(fillColor)
            }
            getNewStyleTooltipElements()?.updateStyle(pointerStyle, dataPointColor) ?: myNewStyleTooltip.provide(0)
        }

        internal fun update(pointerCoord: DoubleVector, orientation: Orientation, stemLen: Double) {
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

            return getNewStyleTooltipElements()?.update(pointerCoord, stemLen) ?: update(pointerCoord)
        }

        private fun update(pointerCoord: DoubleVector) {
            val vertFootingIndent = -calculatePointerFootingIndent(contentRect.height)
            val horFootingIndent = calculatePointerFootingIndent(contentRect.width)

            myBoxPath.d().set(
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
                        lineToIf(pointerCoord, pointerDirection == UP)
                        lineTo(left + horFootingIndent, top)
                        lineTo(left, top)

                        // left side
                        lineTo(left, top - vertFootingIndent)
                        lineToIf(pointerCoord, pointerDirection == LEFT)
                        lineTo(left, bottom + vertFootingIndent)
                        lineTo(left, bottom)

                        // bottom
                        lineTo(left + horFootingIndent, bottom)
                        lineToIf(pointerCoord, pointerDirection == DOWN)
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

        private inner class NewStylePointerBox : SvgComponent() {
            private val myHighlightPoint = SvgCircleElement(DoubleVector.ZERO, 0.0)
            private val myWhitePointerPath = SvgPathElement()
            private val myPointerPath = SvgPathElement()
            private val myWhiteHighlightPoint = SvgCircleElement(DoubleVector.ZERO, 0.0)
            private val myDataPointColorMarker = SvgPathElement()

            private lateinit var myPointerStyle: TipLayoutHint.PointerStyle

            override fun buildComponent() {
                add(myWhitePointerPath)
                add(myPointerPath)
                add(myWhiteHighlightPoint)
                add(myHighlightPoint)
                add(myDataPointColorMarker)
            }

            internal fun updateStyle(pointerStyle: TipLayoutHint.PointerStyle?, dataPointColor: Color?) {
                myPointerStyle = pointerStyle ?: TipLayoutHint.PointerStyle()

                val borderSize = 1.0
                val whiteBackLineSize = 3.0

                // white border for the path
                myWhitePointerPath.apply {
                    strokeColor().set(Color.WHITE)
                    strokeWidth().set(whiteBackLineSize)
                }

                // path to point
                myPointerPath.apply {
                    strokeColor().set(Color.BLACK)
                    strokeWidth().set(borderSize)
                }

                // white border for highlight point
                if (myPointerStyle.isTransparent()) {
                    myWhiteHighlightPoint.apply {
                        fillOpacity().set(0.0)
                        strokeWidth().set(whiteBackLineSize)
                        strokeColor().set(Color.WHITE)
                    }
                }
                // highlight point
                myHighlightPoint.apply {
                    if (myPointerStyle.isTransparent()) {
                        fillOpacity().set(0.0)
                        strokeWidth().set(borderSize)
                    } else {
                        fillColor().set(myPointerStyle.fillColor)
                    }
                    strokeColor().set(myPointerStyle.strokeColor)
                }
                // data point marker
                myDataPointColorMarker.apply {
                    if (dataPointColor != null) {
                        strokeColor().set(dataPointColor)
                        strokeOpacity().set(1.0)
                        strokeWidth().set(4.0)
                    } else {
                        strokeOpacity().set(0.0)
                    }
                }
            }

            fun update(pointerCoord: DoubleVector, stemLen: Double) {
                // tooltip rectangle (todo add shadows)
                myBoxPath.d().set(
                    SvgPathDataBuilder().apply {
                        val s = 4.0
                        with(contentRect) {
                            moveTo(left + s, bottom)

                            lineTo(right - s, bottom)
                            curveTo(
                                DoubleVector(right - s, bottom),
                                DoubleVector(right, bottom),
                                DoubleVector(right, bottom - s)
                            )

                            lineTo(right, top + s)
                            curveTo(
                                DoubleVector(right, top + s),
                                DoubleVector(right, top),
                                DoubleVector(right - s, top)
                            )

                            lineTo(left + s, top)
                            curveTo(
                                DoubleVector(left + s, top),
                                DoubleVector(left, top),
                                DoubleVector(left, top + s)
                            )

                            lineTo(left, bottom - s)
                            curveTo(
                                DoubleVector(left, bottom - s),
                                DoubleVector(left, bottom),
                                DoubleVector(left + s, bottom)
                            )
                        }
                    }.build()
                )

                // data point color marker
                if (addDataPointColorElement) {
                    val vertOffset = V_CONTENT_PADDING
                    val horIndent = H_CONTENT_PADDING * 1.5
                    myDataPointColorMarker.d().set(
                        SvgPathDataBuilder().apply {
                            with(contentRect) {
                                moveTo(left + horIndent, bottom - vertOffset)
                                lineTo(left + horIndent, top + vertOffset)
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

                // pointer path with the highlight point
                myWhitePointerPath.d().set(svgPathData)
                myPointerPath.d().set(svgPathData)
                myWhiteHighlightPoint.apply {
                    cx().set(pointerCoord.x)
                    cy().set(pointerCoord.y)
                    r().set(myPointerStyle.size)
                }
                myHighlightPoint.apply {
                    cx().set(pointerCoord.x)
                    cy().set(pointerCoord.y)
                    r().set(myPointerStyle.size)
                }
            }
        }
    }

    private inner class TextBox : SvgComponent() {
        private val myLinesContainer = SvgSvgElement().apply {
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

        val dimension get() = myContent.run {
            DoubleVector(
                width().get()!! + additionalIndentInContentRect,
                height().get()!!
            )
        }

        override fun buildComponent() {
            add(myContent)
            myContent.children().add(myLinesContainer)
        }

        internal fun update(
            lines: List<TooltipSpec.Line>,
            labelTextColor: Color,
            valueTextColor: Color,
            tooltipMinWidth: Double?
        ) {
            myLinesContainer.children().clear()

            val components: List<Pair<TextLabel?, TextLabel>> = lines.map { line ->
                Pair(
                    line.label?.let(::TextLabel)?.also { if (useNewPointerStyleForTooltip) it.setFontWeight("bold") },
                    TextLabel(line.value)
                )
            }
            // for labels
            components.onEach { (labelComponent, _) ->
                if (labelComponent != null) {
                    labelComponent.textColor().set(labelTextColor)
                    myLinesContainer.children().add(labelComponent.rootGroup)
                }
            }
            // for values
            components.onEach { (_, valueComponent) ->
                valueComponent.textColor().set(valueTextColor)
                myLinesContainer.children().add(valueComponent.rootGroup)
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

            myLinesContainer.apply {
                x().set(H_CONTENT_PADDING)
                y().set(V_CONTENT_PADDING)
                width().set(textSize.x)
                height().set(textSize.y)
            }

            myContent.apply {
                width().set(textSize.x + H_CONTENT_PADDING * 2)
                height().set(textSize.y + V_CONTENT_PADDING * 2)
                moveTo(additionalIndentInContentRect, 0.0)
            }
        }
    }
}