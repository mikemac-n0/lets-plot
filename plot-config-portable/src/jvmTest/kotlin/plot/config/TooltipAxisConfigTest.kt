/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.config

import jetbrains.datalore.base.datetime.Date
import jetbrains.datalore.base.datetime.DateTime
import jetbrains.datalore.base.datetime.Duration
import jetbrains.datalore.base.datetime.Month
import jetbrains.datalore.base.gcommon.collect.ClosedRange
import jetbrains.datalore.plot.base.Aes
import jetbrains.datalore.plot.base.interact.TooltipLineSpec
import jetbrains.datalore.plot.builder.GeomLayer
import jetbrains.datalore.plot.common.time.TimeUtil
import jetbrains.datalore.plot.config.Option.Layer.TOOLTIP_FORMATS
import jetbrains.datalore.plot.config.Option.Layer.TOOLTIP_LINES
import jetbrains.datalore.plot.config.Option.Scale
import jetbrains.datalore.plot.config.Option.TooltipFormat.FIELD
import jetbrains.datalore.plot.config.Option.TooltipFormat.FORMAT
import kotlin.test.Test
import kotlin.test.assertEquals

class TooltipAxisConfigTest {
    private val data = mapOf("v" to listOf(0.34447))
    private val mapping = mapOf(
        Aes.X.name to "v",
        Aes.Y.name to "v"
    )
    private val aesYInGeneralTooltip = TOOLTIP_LINES to listOf("^y")

    // ggplot(data) + geom_point(aes('v','v'), tooltips = layer_tooltips().line('^y'))

    @Test
    fun default() {
        val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip))
        assertGeneralTooltip(geomLayer, "0.34")
        assertYAxisTooltip(geomLayer, "0.34")
        assertEquals("0.3", getYTick(geomLayer))
    }

    @Test
    fun `scale format does not apply to tooltips`() {
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to false,
                    Scale.FORMAT to "scale = {} %"
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "0.34")
            assertYAxisTooltip(geomLayer, "0.34")
            assertEquals("scale = 0.3 %", getYTick(geomLayer))
        }
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to false,
                    Scale.FORMAT to "scale = {.3f} %"
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "0.34")
            assertYAxisTooltip(geomLayer, "0.34")
            assertEquals("scale = 0.300 %", getYTick(geomLayer))
        }
    }

    @Test
    fun `scale_y_discrete(format)`() {
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to true,
                    Scale.FORMAT to "scale = {} %"
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "scale = 0.34447 %")
            assertYAxisTooltip(geomLayer, "scale = 0.34447 %")
            assertEquals("scale = 0.34447 %", getYTick(geomLayer))
        }
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to true,
                    Scale.FORMAT to "scale = {.4f} %"
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "scale = 0.3445 %")
            assertYAxisTooltip(geomLayer, "scale = 0.3445 %")
            assertEquals("scale = 0.3445 %", getYTick(geomLayer))
        }
    }

    @Test
    fun `tooltip format for the 'y'`() {
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "tooltip = {} %"
                    )
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig)
            assertGeneralTooltip(geomLayer, "tooltip = 0.34 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.34 %")
            assertEquals("0.3", getYTick(geomLayer))
        }
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "tooltip = {.4f} %"
                    )
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig)
            assertGeneralTooltip(geomLayer, "tooltip = 0.3445 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.3445 %")
            assertEquals("0.3", getYTick(geomLayer))
        }
    }

    @Test
    fun `scale(format) + tooltip format() - the tooltip formatting is applied to the axis tooltip`() {
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "tooltip = {} %"
                    )
                )
            )
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to false,
                    Scale.FORMAT to "scale = {} %"
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "tooltip = 0.34 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.34 %")
            assertEquals("scale = 0.3 %", getYTick(geomLayer))
        }
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "tooltip = {.4f} %"
                    )
                )
            )
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to false,
                    Scale.FORMAT to "scale = {.3f} %"
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "tooltip = 0.3445 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.3445 %")
            assertEquals("scale = 0.300 %", getYTick(geomLayer))
        }
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to ".4f"
                    )
                )
            )
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to false,
                    Scale.FORMAT to ".3f"
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "0.3445")
            assertYAxisTooltip(geomLayer, "0.3445")
            assertEquals("0.300", getYTick(geomLayer))
        }
    }

    @Test
    fun `tooltip format() for the variable`() {
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "@v",
                        FORMAT to "tooltip = {} %"
                    )
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig)
            assertGeneralTooltip(geomLayer, "tooltip = 0.34 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.34 %")
            assertEquals("0.3", getYTick(geomLayer))
        }
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "@v",
                        FORMAT to "tooltip = {.4f} %"
                    )
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig)
            assertGeneralTooltip(geomLayer, "tooltip = 0.3445 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.3445 %")
            assertEquals("0.3", getYTick(geomLayer))
        }
        run {
            val tooltipConfig = mapOf(
                TOOLTIP_LINES to listOf("@v"),     // as variable
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "@v",
                        FORMAT to "tooltip = {} %"
                    )
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig)
            assertGeneralTooltip(geomLayer, "tooltip = 0.34447 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.34 %")
            assertEquals("0.3", getYTick(geomLayer))
        }
    }

    @Test
    fun log10() {
        val closedRange = ClosedRange(-0.5, -0.5)
        run {
            // default
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.CONTINUOUS_TRANSFORM to "log10"
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "0.344")
            assertYAxisTooltip(geomLayer, "0.344")
            assertEquals("0.32", getYTick(geomLayer, closedRange))
        }
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.CONTINUOUS_TRANSFORM to "log10",
                    Scale.FORMAT to "scale = {} %"
                )
            )
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "tooltip = {} %"
                    )
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "tooltip = 0.344 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.344 %")
            assertEquals("scale = 0.32 %", getYTick(geomLayer, closedRange))
        }
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.CONTINUOUS_TRANSFORM to "log10",
                    Scale.FORMAT to "scale = {.3f} %"
                )
            )
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "tooltip = {.4f} %"
                    )
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "tooltip = 0.3445 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.3445 %")
            assertEquals("scale = 0.316 %", getYTick(geomLayer, closedRange))
        }
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.CONTINUOUS_TRANSFORM to "log10",
                    Scale.FORMAT to ".3f"
                )
            )
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to ".4f"
                    )
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "0.3445")
            assertYAxisTooltip(geomLayer, "0.3445")
            assertEquals("0.316", getYTick(geomLayer, closedRange))
        }
    }

    @Test
    fun dateTime() {
        val instants = List(3) {
            DateTime(Date(1, Month.JANUARY, 2021)).add(Duration.WEEK.mul(it.toLong()))
        }.map { TimeUtil.asInstantUTC(it).toDouble() }
        val closedRange = ClosedRange(instants.first(), instants.last())
        val dt = mapOf("date" to instants, "v" to listOf(0, 1, 2))
        val dtMapping = mapOf(
            Aes.X.name to "v",
            Aes.Y.name to "date"
        )

        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DATE_TIME to true
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(dt, dtMapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "00:00")
            assertYAxisTooltip(geomLayer, "00:00")
            assertEquals("Jan 7", getYTick(geomLayer, closedRange))
        }
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "%b %Y"
                    )
                )
            )
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DATE_TIME to true,
                    Scale.FORMAT to "%b %Y"
                )
            )
            val geomLayer = TestUtil.buildPointLayer(dt, dtMapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "Jan 2021")
            assertYAxisTooltip(geomLayer, "Jan 2021")
            assertEquals("Jan 2021", getYTick(geomLayer, closedRange))
        }
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "tooltip = {}"
                    )
                )
            )
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DATE_TIME to true,
                    Scale.FORMAT to "scale = {}"
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(dt, dtMapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "tooltip = 00:00")
            assertYAxisTooltip(geomLayer, "tooltip = 00:00")
            assertEquals("scale = Jan 7", getYTick(geomLayer, closedRange))
        }
        run {
            val tooltipConfig = mapOf(
                aesYInGeneralTooltip,
                TOOLTIP_FORMATS to listOf(
                    mapOf(
                        FIELD to "^y",
                        FORMAT to "tooltip = {%b %Y}"
                    )
                )
            )
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DATE_TIME to true,
                    Scale.FORMAT to "scale = {%b %Y}"
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(dt, dtMapping, tooltips = tooltipConfig, scales = scales)

            assertGeneralTooltip(geomLayer, "tooltip = Jan 2021")
            assertYAxisTooltip(geomLayer, "tooltip = Jan 2021")
            assertEquals("scale = Jan 2021", getYTick(geomLayer, closedRange))
        }
    }

    companion object {
        private fun assertGeneralTooltip(geomLayer: GeomLayer, expected: String) {
            val dataPoints = geomLayer.contextualMapping.getDataPoints(index = 0)
            val generalTooltip = dataPoints
                .filterNot(TooltipLineSpec.DataPoint::isOutlier)
                .map(TooltipLineSpec.DataPoint::value)
                .firstOrNull()
            assertEquals(expected, generalTooltip, "Wrong general tooltip")
        }

        private fun assertYAxisTooltip(geomLayer: GeomLayer, expected: String) {
            val dataPoints = geomLayer.contextualMapping.getDataPoints(index = 0)
            val yAxisTooltip = dataPoints
                .filter(TooltipLineSpec.DataPoint::isAxis)
                .filter { it.aes == Aes.Y }
                .map(TooltipLineSpec.DataPoint::value)
                .firstOrNull()
            assertEquals(expected, yAxisTooltip, "Wrong axis tooltip")
        }

        private fun getYTick(geomLayer: GeomLayer, closedRange: ClosedRange<Double> = ClosedRange(0.3, 0.4)): String {
            return ScaleConfigLabelsTest.getScaleLabels(
                geomLayer.scaleMap[Aes.Y],
                targetCount = 1,
                closedRange
            ).first()
        }
    }
}