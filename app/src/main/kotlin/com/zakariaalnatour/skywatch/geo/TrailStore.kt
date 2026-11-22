package com.zakariaalnatour.skywatch.geo

import com.zakariaalnatour.skywatch.model.Aircraft
import com.zakariaalnatour.skywatch.model.GeoPoint
import com.zakariaalnatour.skywatch.model.TrackPoint

/**
 * Maintains live per-aircraft position trails across polling ticks with automatic
 * de-duplication, jump rejection, ring-buffer capping, and staleness eviction.
 */
class TrailStore(
  private val maxPointsPerAircraft: Int = 64,
  private val staleAfterMs: Long = 10 * 60 * 1000L,
  private val minMovementNm: Double = 0.08,
  private val maxPlausibleJumpNm: Double = 250.0,
) {
  private val trails = LinkedHashMap<String, ArrayDeque<TrackPoint>>()

  @Synchronized
  fun recordSnapshot(
    aircraft: List<Aircraft>,
    nowMs: Long,
  ): Map<String, List<TrackPoint>> {
    for (ac in aircraft) {
      val key = ac.hex.lowercase()
      val deque = trails.getOrPut(key) { ArrayDeque(maxPointsPerAircraft) }
      val candidate = TrackPoint(
        latitude = ac.latitude,
        longitude = ac.longitude,
        altitudeFt = ac.effectiveAltitudeFt,
        timestampMs = nowMs,
      )
      val last = deque.lastOrNull()
      if (last == null) {
        deque.addLast(candidate)
      } else {
        val movedNm = GreatCircleCalculator.distanceNm(
          GeoPoint(last.latitude, last.longitude),
          GeoPoint(candidate.latitude, candidate.longitude),
        )
        when {
          movedNm > maxPlausibleJumpNm -> {
            // Transponder reset or stale jump; start a fresh clean trail
            deque.clear()
            deque.addLast(candidate)
          }
          movedNm >= minMovementNm -> {
            if (deque.size >= maxPointsPerAircraft) {
              deque.removeFirst()
            }
            deque.addLast(candidate)
          }
        }
      }
    }

    // Prune stale tracks no longer seen
    val cutoff = nowMs - staleAfterMs
    val iterator = trails.entries.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      val lastTs = entry.value.lastOrNull()?.timestampMs ?: 0L
      if (lastTs < cutoff) {
        iterator.remove()
      }
    }

    return snapshot()
  }

  @Synchronized
  fun snapshot(): Map<String, List<TrackPoint>> =
    trails.mapValues { (_, deque) -> deque.toList() }

  @Synchronized
  fun clear() {
    trails.clear()
  }
}
