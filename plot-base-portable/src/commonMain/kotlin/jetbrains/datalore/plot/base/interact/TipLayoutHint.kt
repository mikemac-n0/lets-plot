/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.base.interact

import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.base.values.Color

// `open` - for Mockito tests
open class TipLayoutHint(
    open val kind: Kind,
    open val coord: DoubleVector?,
    open val objectRadius: Double,
    open val color: Color?,
    open val stemLength: StemLength,
    open val pointerStyle: PointerStyle?
) {

    enum class StemLength(val value: Double) {
        LONG(32.0),
        NORMAL(8.0),
        SHORT(5.0),
        NONE(0.0)
    }

    class PointerStyle(
        val fillColor: Color? = Color.BLACK,
        val strokeColor: Color = Color.WHITE,
        val size: Double = 2.0
    ) {
        fun isTransparent() = fillColor == null
    }

    override fun toString(): String {
        return "$kind"
    }


    enum class Kind {
        VERTICAL_TOOLTIP,
        HORIZONTAL_TOOLTIP,
        CURSOR_TOOLTIP,
        X_AXIS_TOOLTIP,
        Y_AXIS_TOOLTIP,
        ROTATED_TOOLTIP
    }


    companion object {

        fun verticalTooltip(
            coord: DoubleVector?,
            objectRadius: Double,
            color: Color?,
            stemLength: StemLength = StemLength.NORMAL,
            pointerStyle: PointerStyle? = null
        ): TipLayoutHint {
            return TipLayoutHint(
                Kind.VERTICAL_TOOLTIP,
                coord,
                objectRadius,
                color,
                stemLength,
                pointerStyle
            )
        }

        fun horizontalTooltip(coord: DoubleVector?, objectRadius: Double, color: Color?, stemLength: StemLength = StemLength.NORMAL): TipLayoutHint {
            return TipLayoutHint(
                Kind.HORIZONTAL_TOOLTIP,
                coord,
                objectRadius,
                color,
                stemLength,
                pointerStyle = null
            )
        }

        fun cursorTooltip(coord: DoubleVector?, color: Color?, stemLength: StemLength = StemLength.NORMAL): TipLayoutHint {
            return TipLayoutHint(
                kind = Kind.CURSOR_TOOLTIP,
                coord = coord,
                objectRadius = 0.0,
                color = color,
                stemLength = stemLength,
                pointerStyle = null
            )
        }

        fun xAxisTooltip(coord: DoubleVector?, color: Color?, axisRadius: Double = 0.0, stemLength: StemLength = StemLength.NONE): TipLayoutHint {
            return TipLayoutHint(
                kind = Kind.X_AXIS_TOOLTIP,
                coord = coord,
                objectRadius = axisRadius,
                color = color,
                stemLength = stemLength,
                pointerStyle = null
            )
        }

        fun yAxisTooltip(coord: DoubleVector?, color: Color?, axisRadius: Double = 0.0, stemLength: StemLength = StemLength.NONE): TipLayoutHint {
            return TipLayoutHint(
                kind = Kind.Y_AXIS_TOOLTIP,
                coord = coord,
                objectRadius = axisRadius,
                color = color,
                stemLength = stemLength,
                pointerStyle = null
            )
        }

        fun rotatedTooltip(coord: DoubleVector?, objectRadius: Double, color: Color?, stemLength: StemLength = StemLength.NORMAL): TipLayoutHint {
            return TipLayoutHint(
                Kind.ROTATED_TOOLTIP,
                coord,
                objectRadius,
                color,
                stemLength,
                pointerStyle = null
            )
        }
    }
}
