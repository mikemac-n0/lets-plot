/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.builder.layout.axis

import jetbrains.datalore.base.interval.DoubleSpan
import jetbrains.datalore.plot.base.Scale
import jetbrains.datalore.plot.base.scale.BreaksGenerator

abstract class AxisBreaksProviderFactory {
    abstract fun createAxisBreaksProvider(axisDomain: DoubleSpan): AxisBreaksProvider

    companion object {
        fun forScale(scale: Scale<Double>): AxisBreaksProviderFactory {
            return if (scale.hasBreaks()) {
                FixedBreaksProviderFactory(FixedAxisBreaksProvider(scale.getScaleBreaks()))
            } else {
                AdaptableBreaksProviderFactory(scale.getBreaksGenerator())
            }
        }
    }

    class FixedBreaksProviderFactory(private val breaksBrovider: FixedAxisBreaksProvider) :
        AxisBreaksProviderFactory() {
        override fun createAxisBreaksProvider(axisDomain: DoubleSpan): AxisBreaksProvider {
            return breaksBrovider
        }
    }

    class AdaptableBreaksProviderFactory(private val breaksGenerator: BreaksGenerator) : AxisBreaksProviderFactory() {
        override fun createAxisBreaksProvider(axisDomain: DoubleSpan): AxisBreaksProvider {
            return AdaptableAxisBreaksProvider(axisDomain, breaksGenerator)
        }
    }
}