/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.base.scale.transform

import jetbrains.datalore.base.interval.DoubleSpan
import jetbrains.datalore.plot.base.scale.BreaksGenerator
import jetbrains.datalore.plot.base.scale.ScaleBreaks
import jetbrains.datalore.plot.base.scale.breaks.DateTimeBreaksHelper

class DateTimeBreaksGen(
    private val labelFormatter: ((Any) -> String)? = null,
    private val valueFormatter: ((Any) -> String)? = null
) : BreaksGenerator {
    override fun generateBreaks(domain: DoubleSpan, targetCount: Int): ScaleBreaks {
        val helper = breaksHelper(domain, targetCount)
        val ticks = helper.breaks
        val labels = ticks.map {
            val value = valueFormatter?.invoke(it) ?: helper.formatter(it)
            labelFormatter?.invoke(value) ?: value
        }
        return ScaleBreaks(ticks, ticks, labels)
    }

    private fun breaksHelper(
        domainAfterTransform: DoubleSpan,
        targetCount: Int
    ): DateTimeBreaksHelper {
        return DateTimeBreaksHelper(
            domainAfterTransform.lowerEnd,
            domainAfterTransform.upperEnd,
            targetCount
        )
    }

    override fun labelFormatter(domain: DoubleSpan, targetCount: Int): (Any) -> String {
        return labelFormatter ?: defaultFormatter(domain, targetCount)
    }

    override fun defaultFormatter(domain: DoubleSpan, targetCount: Int): (Any) -> String {
        return breaksHelper(domain, targetCount).formatter
    }
}
