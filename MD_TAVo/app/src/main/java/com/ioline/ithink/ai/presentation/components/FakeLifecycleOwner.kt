package com.ioline.ithink.ai.presentation.components

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class FakeLifecycleOwner : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        // Define o estado inicial como STARTED, necessário para o CameraX funcionar
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    // ✅ Implementa corretamente a propriedade exigida pela interface
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}