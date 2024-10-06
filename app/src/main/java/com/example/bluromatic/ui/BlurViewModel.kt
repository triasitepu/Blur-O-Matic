package com.example.bluromatic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.bluromatic.BluromaticApplication
import com.example.bluromatic.data.BlurAmountData
import com.example.bluromatic.data.BluromaticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import com.example.bluromatic.KEY_IMAGE_URI
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope


class BlurViewModel(private val bluromaticRepository: BluromaticRepository) : ViewModel() {

    internal val blurAmount = BlurAmountData.blurAmount

    // val blurUiState: StateFlow<BlurUiState> = MutableStateFlow(BlurUiState.Default)
    val blurUiState: StateFlow<BlurUiState> = bluromaticRepository.outputWorkInfo
        .map { info ->
            val outputImageUri = info.outputData.getString(KEY_IMAGE_URI)
            when {
                info.state.isFinished && !outputImageUri.isNullOrEmpty() -> {
                    BlurUiState.Complete(outputUri = outputImageUri)
                }
                info.state == WorkInfo.State.CANCELLED -> {
                    BlurUiState.Default
                }
                else -> BlurUiState.Loading
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BlurUiState.Default
        )


    fun applyBlur(blurLevel: Int) {
        bluromaticRepository.applyBlur(blurLevel)
    }

    fun cancelWork() {
        bluromaticRepository.cancelWork()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val bluromaticRepository =
                    (this[APPLICATION_KEY] as BluromaticApplication).container.bluromaticRepository
                BlurViewModel(
                    bluromaticRepository = bluromaticRepository
                )
            }
        }
    }
}

sealed interface BlurUiState {
    object Default : BlurUiState
    object Loading : BlurUiState
    data class Complete(val outputUri: String) : BlurUiState
}