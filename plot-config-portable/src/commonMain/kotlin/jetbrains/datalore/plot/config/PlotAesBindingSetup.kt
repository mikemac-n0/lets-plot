/*
 * Copyright (c) 2022. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.config

import jetbrains.datalore.plot.base.Aes
import jetbrains.datalore.plot.base.DataFrame
import jetbrains.datalore.plot.builder.VarBinding

internal class PlotAesBindingSetup(
    private val varBindings: List<VarBinding>,
    val dataByVarBinding: Map<VarBinding, DataFrame>,
    val variablesByMappedAes: Map<Aes<*>, List<DataFrame.Variable>>,

    ) {
    fun mappedAesWithoutStatPositional(): Set<Aes<*>> {
        return varBindings.filterNot { it.variable.isStat && Aes.isPositionalXY(it.aes) }
            .map { it.aes }.toSet()
    }

    fun dataByVarBindingWithoutStatPositional(): Map<VarBinding, DataFrame> {
        return dataByVarBinding.filterNot { (binding, _) ->
            binding.variable.isStat && Aes.isPositionalXY(binding.aes)
        }
    }
}