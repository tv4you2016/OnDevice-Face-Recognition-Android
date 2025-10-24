package com.ioline.ithink.ai.presentation.screens.detect_screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ioline.ithink.ai.data.RecognitionMetrics
import com.ioline.ithink.ai.domain.ImageVectorUseCase
import com.ioline.ithink.ai.domain.PersonUseCase
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DetectScreenViewModel(
    val personUseCase: PersonUseCase,
    val imageVectorUseCase: ImageVectorUseCase,
) : ViewModel() {
    val faceDetectionMetricsState = mutableStateOf<RecognitionMetrics?>(null)

    fun getNumPeople(): Long = personUseCase.getCount()
}
