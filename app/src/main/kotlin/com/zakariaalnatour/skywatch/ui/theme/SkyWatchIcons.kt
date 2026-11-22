package com.zakariaalnatour.skywatch.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight Material-style 24dp vector icons so SkyWatch ships a lean ~14 MB APK
 * without bundling 2,000+ unused icons from `material-icons-extended`.
 */
object SkyWatchIcons {
  val Add: ImageVector by lazy {
    icon("Add") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 13f); horizontalLineTo(13f); verticalLineTo(19f); horizontalLineTo(11f)
        verticalLineTo(13f); horizontalLineTo(5f); verticalLineTo(11f); horizontalLineTo(11f)
        verticalLineTo(5f); horizontalLineTo(13f); verticalLineTo(11f); horizontalLineTo(19f)
        close()
      }
    }
  }

  val Remove: ImageVector by lazy {
    icon("Remove") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 13f); horizontalLineTo(5f); verticalLineTo(11f); horizontalLineTo(19f); close()
      }
    }
  }

  val Close: ImageVector by lazy {
    icon("Close") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 6.41f); lineTo(17.59f, 5f); lineTo(12f, 10.59f); lineTo(6.41f, 5f)
        lineTo(5f, 6.41f); lineTo(10.59f, 12f); lineTo(5f, 17.59f); lineTo(6.41f, 19f)
        lineTo(12f, 13.41f); lineTo(17.59f, 19f); lineTo(19f, 17.59f); lineTo(13.41f, 12f); close()
      }
    }
  }

  val Search: ImageVector by lazy {
    icon("Search") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(15.5f, 14f); horizontalLineToRelative(-0.79f); lineToRelative(-0.28f, -0.27f)
        curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        reflectiveCurveTo(3f, 5.91f, 3f, 9.5f)
        reflectiveCurveTo(5.91f, 16f, 9.5f, 16f)
        curveToRelative(1.61f, 0f, 3.09f, -0.59f, 4.23f, -1.57f)
        lineToRelative(0.27f, 0.28f); verticalLineToRelative(0.79f)
        lineToRelative(5f, 4.99f); lineTo(20.49f, 19f); lineToRelative(-4.99f, -5f); close()
        moveTo(9.5f, 14f); curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        reflectiveCurveTo(7.01f, 5f, 9.5f, 5f)
        reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
        reflectiveCurveTo(11.99f, 14f, 9.5f, 14f); close()
      }
    }
  }

  val Refresh: ImageVector by lazy {
    icon("Refresh") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(17.65f, 6.35f)
        curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
        curveToRelative(-4.42f, 0f, -7.99f, 3.58f, -7.99f, 8f)
        reflectiveCurveToRelative(3.57f, 8f, 7.99f, 8f)
        curveToRelative(3.73f, 0f, 6.84f, -2.55f, 7.73f, -6f)
        horizontalLineToRelative(-2.08f)
        curveToRelative(-0.82f, 2.33f, -3.04f, 4f, -5.65f, 4f)
        curveToRelative(-3.31f, 0f, -6f, -2.69f, -6f, -6f)
        reflectiveCurveToRelative(2.69f, -6f, 6f, -6f)
        curveToRelative(1.66f, 0f, 3.14f, 0.69f, 4.22f, 1.78f)
        lineTo(13f, 11f); horizontalLineToRelative(7f); verticalLineTo(4f); lineToRelative(-2.35f, 2.35f); close()
      }
    }
  }

  val PlayArrow: ImageVector by lazy {
    icon("PlayArrow") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(8f, 5f); verticalLineToRelative(14f); lineToRelative(11f, -7f); close()
      }
    }
  }

  val Pause: ImageVector by lazy {
    icon("Pause") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); close()
        moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); close()
      }
    }
  }

  val Radar: ImageVector by lazy {
    icon("Radar") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
        reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
        reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
        moveTo(12f, 20f)
        curveToRelative(-4.41f, 0f, -8f, -3.59f, -8f, -8f)
        reflectiveCurveToRelative(3.59f, -8f, 8f, -8f)
        curveToRelative(1.85f, 0f, 3.55f, 0.63f, 4.9f, 1.69f)
        lineTo(12.71f, 9.88f)
        curveTo(12.49f, 9.8f, 12.25f, 9.75f, 12f, 9.75f)
        curveToRelative(-1.24f, 0f, -2.25f, 1.01f, -2.25f, 2.25f)
        reflectiveCurveToRelative(1.01f, 2.25f, 2.25f, 2.25f)
        reflectiveCurveToRelative(2.25f, -1.01f, 2.25f, -2.25f)
        curveToRelative(0f, -0.25f, -0.05f, -0.49f, -0.13f, -0.71f)
        lineToRelative(4.19f, -4.19f)
        curveTo(19.37f, 8.45f, 20f, 10.15f, 20f, 12f)
        curveToRelative(0f, 4.41f, -3.59f, 8f, -8f, 8f); close()
      }
    }
  }

  val Flight: ImageVector by lazy {
    icon("Flight") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(21f, 16f); verticalLineToRelative(-2f); lineToRelative(-8f, -5f); verticalLineTo(3.5f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        reflectiveCurveTo(10f, 2.67f, 10f, 3.5f); verticalLineTo(9f)
        lineToRelative(-8f, 5f); verticalLineToRelative(2f); lineToRelative(8f, -2.5f); verticalLineTo(19f)
        lineToRelative(-2f, 1.5f); verticalLineTo(22f); lineToRelative(3.5f, -1f); lineToRelative(3.5f, 1f)
        verticalLineToRelative(-1.5f); lineTo(13f, 19f); verticalLineToRelative(-5.5f); lineToRelative(8f, 2.5f); close()
      }
    }
  }

  val FlightTakeoff: ImageVector by lazy {
    icon("FlightTakeoff") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(2.5f, 19f); horizontalLineToRelative(19f); verticalLineToRelative(2f); horizontalLineToRelative(-19f); close()
        moveTo(22.07f, 9.64f)
        curveToRelative(-0.21f, -0.8f, -1.04f, -1.28f, -1.84f, -1.06f)
        lineTo(14.92f, 10f); lineTo(8.02f, 3.57f); lineTo(6.09f, 4.08f)
        lineToRelative(4.14f, 7.17f); lineToRelative(-4.97f, 1.33f); lineToRelative(-1.97f, -1.54f)
        lineToRelative(-1.45f, 0.39f); lineToRelative(1.82f, 3.16f); lineToRelative(0.77f, 1.33f)
        lineToRelative(1.6f, -0.43f); lineToRelative(5.31f, -1.42f); lineToRelative(4.35f, -1.16f)
        lineTo(21f, 11.49f); curveToRelative(0.81f, -0.23f, 1.28f, -1.05f, 1.07f, -1.85f); close()
      }
    }
  }

  val MilitaryShield: ImageVector by lazy {
    icon("MilitaryShield") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f); lineTo(4f, 5f); verticalLineToRelative(6.09f)
        curveToRelative(0f, 5.05f, 3.41f, 9.76f, 8f, 10.91f)
        curveToRelative(4.59f, -1.15f, 8f, -5.86f, 8f, -10.91f); verticalLineTo(5f); lineTo(12f, 2f); close()
        moveTo(12f, 15.5f); lineToRelative(-3.09f, 1.63f); lineToRelative(0.59f, -3.44f)
        lineTo(7f, 11.25f); lineToRelative(3.45f, -0.5f); lineTo(12f, 7.62f)
        lineToRelative(1.55f, 3.13f); lineToRelative(3.45f, 0.5f); lineToRelative(-2.5f, 2.44f)
        lineToRelative(0.59f, 3.44f); close()
      }
    }
  }

  val PublicGlobe: ImageVector by lazy {
    icon("PublicGlobe") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
        reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
        reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
        moveTo(11f, 19.93f)
        curveToRelative(-3.95f, -0.49f, -7f, -3.85f, -7f, -7.93f)
        curveToRelative(0f, -0.62f, 0.08f, -1.21f, 0.21f, -1.79f)
        lineTo(9f, 15f); verticalLineToRelative(1f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f); verticalLineToRelative(1.93f); close()
        moveTo(17.9f, 17.39f)
        curveToRelative(-0.26f, -0.81f, -1f, -1.39f, -1.9f, -1.39f)
        horizontalLineToRelative(-1f); verticalLineToRelative(-3f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        horizontalLineTo(8f); verticalLineToRelative(-2f); horizontalLineToRelative(2f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f); verticalLineTo(7f); horizontalLineToRelative(2f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); verticalLineToRelative(-0.41f)
        curveToRelative(2.93f, 1.19f, 5f, 4.06f, 5f, 7.41f)
        curveToRelative(0f, 2.08f, -0.8f, 3.97f, -2.1f, 5.39f); close()
      }
    }
  }

  val Devices: ImageVector by lazy {
    icon("Devices") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 6f); horizontalLineToRelative(18f); verticalLineTo(4f); horizontalLineTo(4f)
        curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f); verticalLineToRelative(11f); horizontalLineTo(0f)
        verticalLineToRelative(3f); horizontalLineToRelative(14f); verticalLineToRelative(-3f)
        horizontalLineTo(4f); verticalLineTo(6f); close()
        moveTo(23f, 8f); horizontalLineToRelative(-6f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f); verticalLineToRelative(10f)
        curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f); horizontalLineToRelative(6f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f); verticalLineTo(9f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f); close()
        moveTo(22f, 17f); horizontalLineToRelative(-4f); verticalLineToRelative(-7f); horizontalLineToRelative(4f); close()
      }
    }
  }

  val OpenInNew: ImageVector by lazy {
    icon("OpenInNew") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 19f); horizontalLineTo(5f); verticalLineTo(5f); horizontalLineToRelative(7f)
        verticalLineTo(3f); horizontalLineTo(5f)
        curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f); verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f); horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); verticalLineToRelative(-7f)
        horizontalLineToRelative(-2f); verticalLineToRelative(7f); close()
        moveTo(14f, 3f); verticalLineToRelative(2f); horizontalLineToRelative(3.59f)
        lineToRelative(-9.83f, 9.83f); lineToRelative(1.41f, 1.41f)
        lineTo(19f, 6.41f); verticalLineTo(10f); horizontalLineToRelative(2f); verticalLineTo(3f); close()
      }
    }
  }

  val Keyboard: ImageVector by lazy {
    icon("Keyboard") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 5f); horizontalLineTo(4f)
        curveTo(2.9f, 5f, 2.01f, 5.9f, 2.01f, 7f); lineTo(2f, 17f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f); horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); verticalLineTo(7f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f); close()
        moveTo(11f, 8f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineToRelative(-2f); close()
        moveTo(11f, 11f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineToRelative(-2f); close()
        moveTo(8f, 8f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineToRelative(-2f); close()
        moveTo(8f, 11f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineToRelative(-2f); close()
        moveTo(5f, 8f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineToRelative(-2f); close()
        moveTo(5f, 11f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineToRelative(-2f); close()
        moveTo(16f, 17f); horizontalLineTo(8f); verticalLineToRelative(-2f); horizontalLineToRelative(8f); close()
        moveTo(16f, 13f); horizontalLineToRelative(-2f); verticalLineToRelative(-2f); horizontalLineToRelative(2f); close()
        moveTo(16f, 10f); horizontalLineToRelative(-2f); verticalLineTo(8f); horizontalLineToRelative(2f); close()
        moveTo(19f, 13f); horizontalLineToRelative(-2f); verticalLineToRelative(-2f); horizontalLineToRelative(2f); close()
        moveTo(19f, 10f); horizontalLineToRelative(-2f); verticalLineTo(8f); horizontalLineToRelative(2f); close()
      }
    }
  }

  val Layers: ImageVector by lazy {
    icon("Layers") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(11.99f, 18.54f); lineToRelative(-7.37f, -5.73f); lineTo(3f, 14.07f)
        lineToRelative(9f, 7f); lineToRelative(9f, -7f); lineToRelative(-1.63f, -1.27f); close()
        moveTo(12f, 16f); lineToRelative(7.36f, -5.73f); lineTo(21f, 9f)
        lineToRelative(-9f, -7f); lineToRelative(-9f, 7f); lineToRelative(1.63f, 1.27f); close()
      }
    }
  }

  val MyLocation: ImageVector by lazy {
    icon("MyLocation") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 8f)
        curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
        reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
        reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
        reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f); close()
        moveTo(20.94f, 11f)
        curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f)
        verticalLineTo(1f); horizontalLineToRelative(-2f); verticalLineToRelative(2.06f)
        curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11f)
        horizontalLineTo(1f); verticalLineToRelative(2f); horizontalLineToRelative(2.06f)
        curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f)
        verticalLineTo(23f); horizontalLineToRelative(2f); verticalLineToRelative(-2.06f)
        curveToRelative(4.17f, -0.46f, 7.48f, -3.77f, 7.94f, -7.94f)
        horizontalLineTo(23f); verticalLineToRelative(-2f); horizontalLineToRelative(-2.06f); close()
        moveTo(12f, 19f)
        curveToRelative(-3.87f, 0f, -7f, -3.13f, -7f, -7f)
        reflectiveCurveToRelative(3.13f, -7f, 7f, -7f)
        reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
        reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f); close()
      }
    }
  }

  val Timeline: ImageVector by lazy {
    icon("Timeline") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(23f, 8f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        curveToRelative(-0.18f, 0f, -0.35f, -0.02f, -0.51f, -0.07f)
        lineToRelative(-3.56f, 3.55f)
        curveToRelative(0.05f, 0.16f, 0.07f, 0.34f, 0.07f, 0.52f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        reflectiveCurveToRelative(-2f, -0.9f, -2f, -2f)
        curveToRelative(0f, -0.18f, 0.02f, -0.36f, 0.07f, -0.52f)
        lineToRelative(-2.55f, -2.55f)
        curveToRelative(-0.16f, 0.05f, -0.34f, 0.07f, -0.52f, 0.07f)
        reflectiveCurveToRelative(-0.36f, -0.02f, -0.52f, -0.07f)
        lineToRelative(-4.55f, 4.56f)
        curveToRelative(0.05f, 0.16f, 0.07f, 0.33f, 0.07f, 0.51f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        reflectiveCurveToRelative(-2f, -0.9f, -2f, -2f)
        reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
        curveToRelative(0.18f, 0f, 0.35f, 0.02f, 0.51f, 0.07f)
        lineToRelative(4.56f, -4.55f)
        curveTo(8.02f, 9.36f, 8f, 9.18f, 8f, 9f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
        curveToRelative(0f, 0.18f, -0.02f, 0.36f, -0.07f, 0.52f)
        lineToRelative(2.55f, 2.55f)
        curveToRelative(0.16f, -0.05f, 0.34f, -0.07f, 0.52f, -0.07f)
        reflectiveCurveToRelative(0.36f, 0.02f, 0.52f, 0.07f)
        lineToRelative(3.55f, -3.56f)
        curveTo(19.02f, 8.35f, 19f, 8.18f, 19f, 8f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        reflectiveCurveToRelative(2f, 0.9f, 2f, 2f); close()
      }
    }
  }

  val Route: ImageVector by lazy {
    icon("Route") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 15.18f); verticalLineTo(7f)
        curveToRelative(0f, -2.21f, -1.79f, -4f, -4f, -4f)
        reflectiveCurveToRelative(-4f, 1.79f, -4f, 4f); verticalLineToRelative(10f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        reflectiveCurveToRelative(-2f, -0.9f, -2f, -2f); verticalLineTo(8.82f)
        curveTo(8.16f, 8.4f, 9f, 7.3f, 9f, 6f)
        curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
        reflectiveCurveTo(3f, 4.34f, 3f, 6f)
        curveToRelative(0f, 1.3f, 0.84f, 2.4f, 2f, 2.82f); verticalLineTo(17f)
        curveToRelative(0f, 2.21f, 1.79f, 4f, 4f, 4f)
        reflectiveCurveToRelative(4f, -1.79f, 4f, -4f); verticalLineTo(7f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        reflectiveCurveToRelative(2f, 0.9f, 2f, 2f); verticalLineToRelative(8.18f)
        curveToRelative(-1.16f, 0.41f, -2f, 1.51f, -2f, 2.82f)
        curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
        reflectiveCurveToRelative(3f, -1.34f, 3f, -3f)
        curveToRelative(0f, -1.3f, -0.84f, -2.4f, -2f, -2.82f); close()
      }
    }
  }

  val ContentCopy: ImageVector by lazy {
    icon("ContentCopy") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(16f, 1f); horizontalLineTo(4f)
        curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f); verticalLineToRelative(14f); horizontalLineToRelative(2f)
        verticalLineTo(3f); horizontalLineToRelative(12f); verticalLineTo(1f); close()
        moveTo(19f, 5f); horizontalLineTo(8f)
        curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f); verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f); horizontalLineToRelative(11f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); verticalLineTo(7f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f); close()
        moveTo(19f, 21f); horizontalLineTo(8f); verticalLineTo(7f); horizontalLineToRelative(11f); close()
      }
    }
  }

  val Block: ImageVector by lazy {
    icon("Block") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
        reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
        reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
        moveTo(4f, 12f)
        curveToRelative(0f, -4.42f, 3.58f, -8f, 8f, -8f)
        curveToRelative(1.85f, 0f, 3.55f, 0.63f, 4.9f, 1.69f)
        lineTo(5.69f, 16.9f)
        curveTo(4.63f, 15.55f, 4f, 13.85f, 4f, 12f); close()
        moveTo(12f, 20f)
        curveToRelative(-1.85f, 0f, -3.55f, -0.63f, -4.9f, -1.69f)
        lineTo(18.31f, 7.1f)
        curveTo(19.37f, 8.45f, 20f, 10.15f, 20f, 12f)
        curveToRelative(0f, 4.42f, -3.58f, 8f, -8f, 8f); close()
      }
    }
  }

  private inline fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
      name = name,
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply(block).build()
}
