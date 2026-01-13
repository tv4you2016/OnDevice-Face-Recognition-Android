package com.ioline.ithink.ai.presentation.screens.face_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioline.ithink.ai.domain.ImageVectorUseCase
import com.ioline.ithink.ai.domain.PersonUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class FaceListScreenViewModel(
    val imageVectorUseCase: ImageVectorUseCase,
    val personUseCase: PersonUseCase,
) : ViewModel() {
    val personFlow = personUseCase.getAll()

    // Remove the person from `PersonRecord`
    // and all associated face embeddings from `FaceImageRecord`
    fun removeFace(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            personUseCase.removePerson(id)
            imageVectorUseCase.removeImages(id)
        }
    }
}
