/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plotDemo.plotConfig

import jetbrains.datalore.plotDemo.model.plotConfig.ColorScalesViridis
import jetbrains.datalore.vis.demoUtils.PlotSpecsDemoWindowJfx
import java.awt.Dimension

fun main() {
    with(ColorScalesViridis()) {
        PlotSpecsDemoWindowJfx(
            "Color Scales 'Viridis'",
            plotSpecList(),
            plotSize = Dimension(600, 100),
            maxCol = 2
        ).open()
    }
}
