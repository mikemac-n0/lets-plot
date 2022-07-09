/*
 * Copyright (c) 2020. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.builder.coord

import jetbrains.datalore.base.interval.DoubleSpan
import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.base.spatial.MercatorUtils
import jetbrains.datalore.base.spatial.projections.Projection
import jetbrains.datalore.plot.base.Scale
import jetbrains.datalore.plot.base.ScaleMapper
import jetbrains.datalore.plot.base.scale.Mappers
import jetbrains.datalore.plot.base.scale.ScaleBreaks
import jetbrains.datalore.plot.common.data.SeriesUtil
import kotlin.math.abs

internal class ProjectionCoordProvider(
    private val projection: Projection,
    xLim: DoubleSpan?,
    yLim: DoubleSpan?,
    flipped: Boolean
) : CoordProviderBase(xLim, yLim, flipped) {

    override fun with(
        xLim: DoubleSpan?,
        yLim: DoubleSpan?,
        flipped: Boolean
    ): CoordProvider {
        return ProjectionCoordProvider(projection, xLim, yLim, flipped)
    }

    protected override fun adjustDomainsIntern(
        hDomain: DoubleSpan,
        vDomain: DoubleSpan
    ): Pair<DoubleSpan, DoubleSpan> {
        @Suppress("NAME_SHADOWING")
        val xDomain = toValidDomain(projection.validRect().xRange(), hDomain)

        @Suppress("NAME_SHADOWING")
        val yDomain = toValidDomain(projection.validRect().yRange(), vDomain)
        return (xDomain to yDomain)
    }

    override fun adjustGeomSize(
        hDomain: DoubleSpan,
        vDomain: DoubleSpan,
        geomSize: DoubleVector
    ): DoubleVector {
        // Adjust geom dimensions ratio.
        val h0 = projectionX.apply(hDomain.lowerEnd)
        val h1 = projectionX.apply(hDomain.upperEnd)
        val v0 = projectionY.apply(vDomain.lowerEnd)
        val v1 = projectionY.apply(vDomain.upperEnd)

        val domainRatio = abs(h1 - h0) / abs(v1 - v0)
        return FixedRatioCoordProvider.reshapeGeom(geomSize, domainRatio)
    }

    override fun buildAxisScaleX(
        scaleProto: Scale<Double>,
        domain: DoubleSpan,
        breaks: ScaleBreaks
    ): Scale<Double> {
        return if (projection.nonlinear) {
            buildAxisScaleWithProjection(
                projectionX,
                scaleProto,
                domain,
                breaks
            )
        } else {
            super.buildAxisScaleX(scaleProto, domain, breaks)
        }
    }

    override fun buildAxisScaleY(
        scaleProto: Scale<Double>,
        domain: DoubleSpan,
        breaks: ScaleBreaks
    ): Scale<Double> {
        return if (projectionY.nonlinear) {
            buildAxisScaleWithProjection(
                projectionY,
                scaleProto,
                domain,
                breaks
            )
        } else {
            super.buildAxisScaleY(scaleProto, domain, breaks)
        }
    }

    override fun buildAxisXScaleMapper(domain: DoubleSpan, axisLength: Double): ScaleMapper<Double> {
        return if (projectionX.nonlinear) {
            buildAxisScaleMapperWithProjection(
                projectionX,
                domain,
                axisLength,
            )
        } else {
            super.buildAxisXScaleMapper(domain, axisLength)
        }
    }

    override fun buildAxisYScaleMapper(domain: DoubleSpan, axisLength: Double): ScaleMapper<Double> {
        return if (projectionY.nonlinear) {
            buildAxisScaleMapperWithProjection(
                projectionY,
                domain,
                axisLength,
            )
        } else {
            super.buildAxisXScaleMapper(domain, axisLength)
        }
    }


    private fun toValidDomainY(domain: DoubleSpan): DoubleSpan {
        if (projection.validRect().yRange().connected(domain)) {
            return projection.validRect().yRange().intersection(domain)
        }
        throw IllegalArgumentException("Illegal latitude range: $domain")
    }

    private fun toValidDomainX(domain: DoubleSpan): DoubleSpan {
        if (projection.validRect().xRange().connected(domain)) {
            return projection.validRect().xRange().intersection(domain)
        }
        throw IllegalArgumentException("Illegal longitude range: $domain")
    }


    companion object {
        private fun toValidDomain(validRangle: DoubleSpan, domain: DoubleSpan): DoubleSpan {
            if (validRangle.connected(domain)) {
                return validRangle.intersection(domain)
            }
            throw IllegalArgumentException("Illegal range: $domain")
        }

        private fun buildAxisScaleWithProjection(
            projection: Projection, scaleProto: Scale<Double>,
            domain: DoubleSpan,
            breaks: ScaleBreaks
        ): Scale<Double> {

            val validDomain = toValidDomain(projection.validRect().xRange(), domain)
            val validBreaks = validateBreaks(validDomain, breaks)
            return buildAxisScaleDefault(
                scaleProto,
                validBreaks
            )
        }

        private fun validateBreaks(validDomain: DoubleSpan, breaks: ScaleBreaks): ScaleBreaks {
            val validIndices = ArrayList<Int>()
            var i = 0
            for (v in breaks.domainValues) {
                if (v is Double && validDomain.contains(v)) {
                    validIndices.add(i)
                }
                i++
            }

            if (validIndices.size == breaks.domainValues.size) {
                return breaks
            }

            val validDomainValues = SeriesUtil.pickAtIndices(breaks.domainValues, validIndices)
            val validLabels = SeriesUtil.pickAtIndices(breaks.labels, validIndices)
            val validTransformedValues = SeriesUtil.pickAtIndices(breaks.transformedValues, validIndices)
            return ScaleBreaks(
                validDomainValues,
                validTransformedValues,
                validLabels
            )
        }

        private fun buildAxisScaleMapperWithProjection(
            projection: Projection,
            domain: DoubleSpan,
            axisLength: Double,
        ): ScaleMapper<Double> {
            val linearMapper = linearMapper(
                domain,
                axisLength
            )

            val validDomain = toValidDomain(projection.validRect().xRange(), domain)
            val validDomainProjected = DoubleSpan(
                projection.apply(validDomain.lowerEnd),
                projection.apply(validDomain.upperEnd)
            )

            val projectionInverse = Mappers.linear(validDomainProjected, validDomain)
            return twistScaleMapper(
                projection,
                projectionInverse,
                linearMapper
            )
        }

        private fun twistScaleMapper(
            projection: Projection,
            projectionInverse: ScaleMapper<Double>,
            scaleMapper: ScaleMapper<Double>
        ): ScaleMapper<Double> {
            return object : ScaleMapper<Double> {
                override fun invoke(v: Double?): Double? {
                    return v?.let {
                        val projected = projection.apply(it)
                        val unProjected = projectionInverse(projected)
                        scaleMapper(unProjected)
                    }
                }
            }
        }
    }
}
