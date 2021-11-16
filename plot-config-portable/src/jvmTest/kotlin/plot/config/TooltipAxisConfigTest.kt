/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.config

import jetbrains.datalore.plot.base.Aes
import jetbrains.datalore.plot.base.interact.TooltipLineSpec
import jetbrains.datalore.plot.builder.GeomLayer
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
    }

    @Test
    fun `scale format does not apply to tooltips`() {
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to false,
                    Scale.FORMAT to "scale = {} %"  // using default formatter
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "0.34")
            assertYAxisTooltip(geomLayer, "0.34")
        }
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to false,
                    Scale.FORMAT to "scale = {.4f} %"
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "0.34")
            assertYAxisTooltip(geomLayer, "0.34")
        }
    }

    @Test
    fun `scale_y_discrete(format)`() {
        run {
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to true,
                    Scale.FORMAT to "scale = {} %"  // using default formatter
                )
            )
            val geomLayer =
                TestUtil.buildPointLayer(data, mapping, tooltips = mapOf(aesYInGeneralTooltip), scales = scales)
            assertGeneralTooltip(geomLayer, "scale = 0.34447 %")
            assertYAxisTooltip(geomLayer, "scale = 0.34447 %")
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
                        FORMAT to "tooltip = {} %"  // using default formatter
                    )
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig)
            assertGeneralTooltip(geomLayer, "tooltip = 0.34447 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.34447 %")
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
                        FORMAT to "tooltip = {} %"  // using default formatter
                    )
                )
            )
            val scales = listOf(
                mapOf(
                    Scale.AES to Aes.Y.name,
                    Scale.DISCRETE_DOMAIN to false,
                    Scale.FORMAT to "scale = {} %"  // using default formatter
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "tooltip = 0.34447 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.34447 %")
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
                    Scale.FORMAT to "scale = {.1f} %"
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig, scales = scales)
            assertGeneralTooltip(geomLayer, "tooltip = 0.3445 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.3445 %")
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
                        FORMAT to "tooltip = {} %"  // using default formatter
                    )
                )
            )
            val geomLayer = TestUtil.buildPointLayer(data, mapping, tooltips = tooltipConfig)
            assertGeneralTooltip(geomLayer, "tooltip = 0.34447 %")
            assertYAxisTooltip(geomLayer, "tooltip = 0.34447 %")
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
    }
}