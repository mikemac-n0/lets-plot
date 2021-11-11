/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.base.scale.transform

import jetbrains.datalore.base.gcommon.collect.ClosedRange
import jetbrains.datalore.base.stringFormat.StringFormat
import jetbrains.datalore.plot.base.scale.BreaksGenerator
import jetbrains.datalore.plot.base.scale.BreaksGenerator.Companion.getLabelFormatter
import jetbrains.datalore.plot.base.scale.ScaleBreaks
import jetbrains.datalore.plot.base.scale.breaks.DateTimeBreaksHelper

class DateTimeBreaksGen(
    private val formatter: StringFormat? = null
) : BreaksGenerator {
    override fun generateBreaks(domain: ClosedRange<Double>, targetCount: Int): ScaleBreaks {
        val helper = breaksHelper(domain, targetCount)
        val ticks = helper.breaks
        val labelFormatter = getLabelFormatter(formatter, helper.formatter)
        val labels = ArrayList<String>()
        for (tick in ticks) {
            labels.add(labelFormatter(tick))
        }
        return ScaleBreaks(ticks, ticks, labels)
    }

    private fun breaksHelper(
        domainAfterTransform: ClosedRange<Double>,
        targetCount: Int
    ): DateTimeBreaksHelper {
        return DateTimeBreaksHelper(
            domainAfterTransform.lowerEnd,
            domainAfterTransform.upperEnd,
            targetCount
        )
    }

    override fun labelFormatter(domain: ClosedRange<Double>, targetCount: Int): (Any) -> String {
        return getLabelFormatter(formatter, defaultFormatter(domain, targetCount))
    }

    override fun defaultFormatter(domain: ClosedRange<Double>, targetCount: Int): (Any) -> String {
        return breaksHelper(domain, targetCount).formatter
    }
}
