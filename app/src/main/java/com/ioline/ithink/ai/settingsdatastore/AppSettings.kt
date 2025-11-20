package com.ioline.ithink.ai.settingsdatastore

import com.ioline.ithink.ai.layout.Option
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val detectionType: Option = Option.None,
    val facial: FacialSettings = FacialSettings(),
    val proximity: ProximitySettings = ProximitySettings(),
    val camera: CameraSettings = CameraSettings()
)

@Serializable
data class FacialSettings(
    val minConfidence: Float = 0.6f,
    val autoAddFaces: Boolean = false,
    val cameraLens: Int = 0
)

@Serializable
data class ProximitySettings(
    val maxDistance: Float = 5f,
    val triggerDelay: Int = 300,
    val enabled: Boolean = false
)

@Serializable
data class CameraSettings(
    val detectionThreshold: Float = 0.5f,
    val stabilization: Boolean = true
)
