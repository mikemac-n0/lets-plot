/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.builder.scale.mapper

import jetbrains.datalore.base.stringFormat.StringFormat
import jetbrains.datalore.plot.builder.scale.GuideMapper
import jetbrains.datalore.plot.builder.scale.WithGuideBreaks

internal class GuideMapperWithGuideBreaks<DomainT, TargetT>(
    private val mapper: (Double?) -> TargetT?,
    override val breaks: List<DomainT>,
    override val formatter: StringFormat
) : GuideMapper<TargetT>,
    WithGuideBreaks<DomainT> {

    override val isContinuous = false

    override fun apply(value: Double?): TargetT? {
        return mapper(value)
    }
}
