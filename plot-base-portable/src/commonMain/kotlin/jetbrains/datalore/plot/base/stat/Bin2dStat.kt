/*
 * Copyright (c) 2020. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.base.stat

import jetbrains.datalore.base.gcommon.collect.ClosedRange
import jetbrains.datalore.plot.base.Aes
import jetbrains.datalore.plot.base.DataFrame
import jetbrains.datalore.plot.base.StatContext
import jetbrains.datalore.plot.base.data.TransformVar
import jetbrains.datalore.plot.base.util.MutableDouble
import jetbrains.datalore.plot.common.data.SeriesUtil
import kotlin.math.floor

/**
 * Default stat for geom_bin2d
 *
 * @param binCountX Number of bins (overridden by binWidth).
 * @param binCountY Number of bins (overridden by binWidth).
 * @param binWidthX Used to compute binCount such that bins covers the range of the data.
 * @param binWidthY Used to compute binCount such that bins covers the range of the data.
 * @param drop if TRUE removes all cells with 0 counts.
 *
 * Computed values:
 *
 * count - number of points in bin
 * density - density of points in bin, scaled to integrate to 1
 * ncount - count, scaled to maximum of 1
 * ndensity - density, scaled to maximum of 1
 */
class Bin2dStat(
    binCountX: Int,
    binWidthX: Double?,
    binCountY: Int,
    binWidthY: Double?,
    private val drop: Boolean
) : BaseStat(DEF_MAPPING) {
    private val binOptionsX = BinStatUtil.BinOptions(binCountX, binWidthX)
    private val binOptionsY = BinStatUtil.BinOptions(binCountY, binWidthY)

    override fun requires(): List<Aes<*>> {
        return listOf(Aes.X, Aes.Y)
    }

    override fun apply(data: DataFrame, statCtx: StatContext): DataFrame {
        if (!hasRequiredValues(data)) {
            return withEmptyStatValues()
        }

        val xRange = statCtx.overallXRange()
        val yRange = statCtx.overallYRange()
        if (xRange == null || yRange == null) {
            return withEmptyStatValues()
        }

        // replace 0-range --> 1-range (span can't be 0)
//        if (SeriesUtil.span(xRange) <= SeriesUtil.TINY) {
//            xRange = ClosedRange(xRange.lowerEndpoint() - 0.5, xRange.lowerEndpoint() + 0.5)
//        }
//        if (SeriesUtil.span(yRange) <= SeriesUtil.TINY) {
//            yRange = ClosedRange(yRange.lowerEndpoint() - 0.5, yRange.lowerEndpoint() + 0.5)
//        }

        //        var xStart = xRange.lowerEndpoint()
//        var yStart = yRange.lowerEndpoint()
//        var xSpan = SeriesUtil.span(xRange)
//        var ySpan = SeriesUtil.span(yRange)
//
//        // span can't be 0
//        if (xSpan <= SeriesUtil.TINY) {
//            xSpan = 1.0
//            yStart
//        }
//        if (ySpan <= SeriesUtil.TINY) {
//            ySpan = 1.0
//        }

//        var xSpan = SeriesUtil.span(xRange)
//        var ySpan = SeriesUtil.span(yRange)
//        // span can't be 0
//        if (xSpan <= SeriesUtil.TINY) {
//            xSpan = 1.0
//        }
//        if (ySpan <= SeriesUtil.TINY) {
//            ySpan = 1.0
//        }


        // initial bin width and count

        val xRangeInit = adjustRangeInitial(xRange)
        val yRangeInit = adjustRangeInitial(yRange)

        var xCountAndWidthInit = BinStatUtil.binCountAndWidth(SeriesUtil.span(xRangeInit), binOptionsX)
        var yCountAndWidthInit = BinStatUtil.binCountAndWidth(SeriesUtil.span(yRangeInit), binOptionsY)

        // final bin width and count

        val xRangeFinal = adjustRangeFinal(xRange, xCountAndWidthInit.width)
        val yRangeFinal = adjustRangeFinal(yRange, yCountAndWidthInit.width)

        var xCountAndWidthFinal = BinStatUtil.binCountAndWidth(SeriesUtil.span(xRangeFinal), binOptionsX)
        var yCountAndWidthFinal = BinStatUtil.binCountAndWidth(SeriesUtil.span(yRangeFinal), binOptionsY)

        val countTotal = xCountAndWidthFinal.count * yCountAndWidthFinal.count
        val densityNormalizingFactor =
            densityNormalizingFactor(SeriesUtil.span(xRangeFinal), SeriesUtil.span(yRangeFinal), countTotal)

        val binsData = computeBins(
            data.getNumeric(TransformVar.X),
            data.getNumeric(TransformVar.Y),
            xRangeFinal.lowerEndpoint(),
            yRangeFinal.lowerEndpoint(),
            xCountAndWidthFinal.count,
            yCountAndWidthFinal.count,
            xCountAndWidthFinal.width,
            yCountAndWidthFinal.width,
            BinStatUtil.weightAtIndex(data),
            densityNormalizingFactor
        )

        return DataFrame.Builder()
            .putNumeric(Stats.X, binsData.x)
            .putNumeric(Stats.Y, binsData.y)
            .putNumeric(Stats.COUNT, binsData.count)
            .putNumeric(Stats.DENSITY, binsData.density)
            .build()
    }

    private fun computeBins(
        xValues: List<Double?>,
        yValues: List<Double?>,
        xStart: Double,
        yStart: Double,
        binCountX: Int,
        binCountY: Int,
        binWidth: Double,
        binHeight: Double,
        weightAtIndex: (Int) -> Double,
        densityNormalizingFactor: Double
    ): Bins2dData {

        var totalCount = 0.0
        val countByBinIndexKey = HashMap<Pair<Int, Int>, MutableDouble>()
        for (dataIndex in xValues.indices) {
            val x = xValues[dataIndex]
            val y = yValues[dataIndex]
            if (!SeriesUtil.allFinite(x, y)) {
                continue
            }
            val weight = weightAtIndex(dataIndex)
            totalCount += weight
            val binIndexX = floor((x!! - xStart) / binWidth).toInt()
            val binIndexY = floor((y!! - yStart) / binHeight).toInt()
            val binIndexKey = Pair(binIndexX, binIndexY)
            if (!countByBinIndexKey.containsKey(binIndexKey)) {
                countByBinIndexKey[binIndexKey] = MutableDouble(0.0)
            }
            countByBinIndexKey[binIndexKey]!!.getAndAdd(weight)
        }

        val xs = ArrayList<Double>()
        val ys = ArrayList<Double>()
        val counts = ArrayList<Double>()
        val densities = ArrayList<Double>()

        val x0 = xStart + binWidth / 2
        val y0 = yStart + binHeight / 2
        for (xIndex in 0 until binCountX) {
            for (yIndex in 0 until binCountY) {
                val binIndexKey = Pair(xIndex, yIndex)
                var count = 0.0
                if (countByBinIndexKey.containsKey(binIndexKey)) {
                    count = countByBinIndexKey[binIndexKey]!!.get()
                }

                if (drop && count == 0.0) {
                    continue
                }

                xs.add(x0 + xIndex * binWidth)
                ys.add(y0 + yIndex * binHeight)
                counts.add(count)
                val density = count / totalCount * densityNormalizingFactor
                densities.add(density)
            }
        }

        return Bins2dData(xs, ys, counts, densities)
    }


    companion object {
        private val DEF_MAPPING: Map<Aes<*>, DataFrame.Variable> = mapOf(
            Aes.X to Stats.X,
            Aes.Y to Stats.Y,
            Aes.FILL to Stats.COUNT
        )

        private fun adjustRangeInitial(r: ClosedRange<Double>): ClosedRange<Double> {
            // span can't be 0
            return if (SeriesUtil.span(r) <= SeriesUtil.TINY) {
                ClosedRange(r.lowerEndpoint() - 0.5, r.lowerEndpoint() + 0.5)
            } else {
                r
            }
        }

        private fun adjustRangeFinal(r: ClosedRange<Double>, binWidth: Double): ClosedRange<Double> {
            return if (SeriesUtil.span(r) <= SeriesUtil.TINY) {
                // 0 span allways becomes 0
                ClosedRange(r.lowerEndpoint() - 0.5, r.lowerEndpoint() + 0.5)
            } else {
                // Expand range by half of bin width (arbitrary choise - can be any positive num) to
                // avoid data-points on the marginal bin margines.
                val exp = binWidth / 2.0
                ClosedRange(r.lowerEndpoint() - exp, r.upperEndpoint() + exp)
            }
        }

        private fun densityNormalizingFactor(
            xSpan: Double,
            ySpan: Double,
            count: Int
        ): Double {
            // density should integrate to 1.0
            val area = xSpan * ySpan
            val binArea = area / count
            return 1.0 / binArea
        }
    }

    class Bins2dData(
        internal val x: List<Double>,
        internal val y: List<Double>,
        internal val count: List<Double>,
        internal val density: List<Double>
    )

    class Bins2dWeightedCounts(
        internal val total: Double,
        internal val countByBinXY: Map<Pair<Double, Double>, Double>
    )
}