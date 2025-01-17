/*
 * Copyright (c) 2021. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.builder

import jetbrains.datalore.plot.base.interact.GeomTargetCollector
import jetbrains.datalore.plot.base.render.svg.SvgComponent

interface FrameOfReference {
    fun drawBeforeGeomLayer(parent: SvgComponent)

    fun drawAfterGeomLayer(parent: SvgComponent)

    fun buildGeomComponent(layer: GeomLayer, targetCollector: GeomTargetCollector): SvgComponent
}