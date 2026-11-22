package com.zakariaalnatour.skywatch.geo

import com.zakariaalnatour.skywatch.model.GeoPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteMetrics(
  val totalDistanceNm: Double,
  val remainingDistanceNm: Double?,
  val progressFraction: Float?,
  val initialBearingDeg: Double,
)

/**
 * True spherical great-circle (geodesic) calculations for flight distances,
 * route progress, SLERP arc interpolation, and antimeridian-safe rendering segments.
 */
object GreatCircleCalculator {
  const val EARTH_RADIUS_NM = 3440.065
  const val EARTH_RADIUS_KM = 6371.0088

  fun distanceNm(from: GeoPoint, to: GeoPoint): Double =
    centralAngleRad(from, to) * EARTH_RADIUS_NM

  fun distanceKm(from: GeoPoint, to: GeoPoint): Double =
    centralAngleRad(from, to) * EARTH_RADIUS_KM

  fun initialBearingDeg(from: GeoPoint, to: GeoPoint): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
  }

  fun computeRouteMetrics(
    origin: GeoPoint,
    destination: GeoPoint,
    currentPosition: GeoPoint?,
  ): RouteMetrics {
    val totalNm = distanceNm(origin, destination)
    val bearing = initialBearingDeg(origin, destination)
    if (currentPosition == null || totalNm < 1.0) {
      return RouteMetrics(totalNm, null, null, bearing)
    }
    val remainingNm = distanceNm(currentPosition, destination)
    val coveredNm = distanceNm(origin, currentPosition)
    val progress = (coveredNm / (coveredNm + remainingNm)).toFloat().coerceIn(0f, 1f)
    return RouteMetrics(
      totalDistanceNm = totalNm,
      remainingDistanceNm = remainingNm,
      progressFraction = progress,
      initialBearingDeg = bearing,
    )
  }

  /**
   * Computes intermediate points along the shortest great-circle arc between [from] and [to]
   * using spherical linear interpolation (SLERP).
   */
  fun greatCircleArc(
    from: GeoPoint,
    to: GeoPoint,
    segments: Int = 64,
  ): List<GeoPoint> {
    val safeSegments = segments.coerceAtLeast(2)
    val delta = centralAngleRad(from, to)
    if (delta < 1e-6) {
      return listOf(from, to)
    }

    val lat1 = Math.toRadians(from.latitude)
    val lon1 = Math.toRadians(from.longitude)
    val lat2 = Math.toRadians(to.latitude)
    val lon2 = Math.toRadians(to.longitude)
    val sinDelta = sin(delta)

    return List(safeSegments + 1) { index ->
      val f = index.toDouble() / safeSegments.toDouble()
      val a = sin((1.0 - f) * delta) / sinDelta
      val b = sin(f * delta) / sinDelta
      val x = a * cos(lat1) * cos(lon1) + b * cos(lat2) * cos(lon2)
      val y = a * cos(lat1) * sin(lon1) + b * cos(lat2) * sin(lon2)
      val z = a * sin(lat1) + b * sin(lat2)
      val lat = Math.toDegrees(atan2(z, sqrt(x * x + y * y)))
      val lon = Math.toDegrees(atan2(y, x))
      GeoPoint(lat, lon)
    }
  }

  /**
   * Splits a polyline into continuous sub-polylines whenever consecutive points jump across
   * the antimeridian (|Δlon| > 180°), preventing wrap-around artifacts on Mercator maps.
   */
  fun splitSegmentsAtAntimeridian(points: List<GeoPoint>): List<List<GeoPoint>> {
    if (points.size < 2) return if (points.isEmpty()) emptyList() else listOf(points)
    val result = mutableListOf<List<GeoPoint>>()
    var current = mutableListOf(points.first())
    for (i in 1 until points.size) {
      val prev = points[i - 1]
      val next = points[i]
      if (abs(next.longitude - prev.longitude) > 180.0) {
        if (current.size >= 2) result += current
        current = mutableListOf(next)
      } else {
        current += next
      }
    }
    if (current.size >= 2) result += current
    return result
  }

  private fun centralAngleRad(from: GeoPoint, to: GeoPoint): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val sinDLat = sin(dLat / 2.0)
    val sinDLon = sin(dLon / 2.0)
    val h = (sinDLat * sinDLat + cos(lat1) * cos(lat2) * sinDLon * sinDLon).coerceIn(0.0, 1.0)
    return 2.0 * asin(sqrt(h))
  }
}
