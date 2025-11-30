package org.onion.diffusion.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onion.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.onion.diffusion.native.DiffusionLoader
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class ChatViewModel  : ViewModel() {

    private var diffusionLoader:DiffusionLoader = DiffusionLoader()
    private var modelPath = ""
    private var initModel = false
    // 0 default,1 loading,2 loading completely
    var loadingModelState = MutableStateFlow(0)

    suspend fun selectLLMModelFile(): String{
        loadingModelState.emit(1)
        modelPath = diffusionLoader.getModelFilePath()
        return modelPath
    }

    fun initLLM(){
        if(initModel) return
        initModel = true
        viewModelScope.launch(Dispatchers.Default) {
            loadingModelState.emit(1)
            // ---- read chatTemplate and contextSize ------
            diffusionLoader.loadModel(modelPath)
            loadingModelState.emit(2)
        }
    }

    private var responseGenerationJob: Job? = null
    private var isInferenceOn: Boolean = false
    @OptIn(ExperimentalTime::class)
    fun getTalkerResponse(query: String, onCancelled: () -> Unit, onError: (Throwable) -> Unit){
        runCatching {
            responseGenerationJob = viewModelScope.launch(Dispatchers.Default) {
                isInferenceOn = true
                val duration = measureTime {
                    // Call txt2Img to generate image from the query prompt
                    val imageByteArray = diffusionLoader.txt2Img(
                        prompt = query,
                        negative = "",
                        width = 512,
                        height = 512,
                        steps = 20,
                        cfg = 7.5f,
                        seed = Clock.System.now().toEpochMilliseconds()
                    )

                    // Debug logging to verify image format
                    println("=== Image Generation Debug ===")
                    println("Image size: ${imageByteArray?.size} bytes")
                    if (imageByteArray != null && imageByteArray.size >= 10) {
                        println("First 10 bytes: ${imageByteArray.take(10).joinToString { it.toString() }}")
                        // PNG signature: 137 80 78 71 13 10 26 10
                        // JPEG signature: 255 216 255
                        val isPNG = imageByteArray.size >= 8 &&
                                imageByteArray[0].toInt() == 137 &&
                                imageByteArray[1].toInt() == 80
                        val isJPEG = imageByteArray.size >= 3 &&
                                imageByteArray[0].toInt() and 0xFF == 255 &&
                                imageByteArray[1].toInt() and 0xFF == 216
                        println("Format detection - PNG: $isPNG, JPEG: $isJPEG")
                    }
                    println("Expected size for 512x512 RGBA: ${512 * 512 * 4} bytes")
                    println("Expected size for 512x512 RGB: ${512 * 512 * 3} bytes")
                    println("==============================")

                    // Update the last message in the chat with the generated image
                    // Using removeAt + add instead of index assignment to trigger recomposition
                    if (_currentChatMessages.isNotEmpty()) {
                        val lastIndex = _currentChatMessages.lastIndex
                        _currentChatMessages.removeAt(lastIndex)
                        _currentChatMessages.add(lastIndex, ChatMessage(
                            message = "图片生成完成:${imageByteArray?.size}字节",
                            isUser = false,
                            image = imageByteArray
                        ))
                    }
                }
                isGenerating.value = false
            }
        }.getOrElse { exception ->
            isInferenceOn = false
            if(exception is CancellationException){
                onCancelled()
            }else onError(exception)
        }
    }

    // ========================================================================================
    //                              Chat Message State
    // ========================================================================================

    /** Current active chat conversation messages */
    private val _currentChatMessages = mutableStateListOf<ChatMessage>()
    val currentChatMessages: SnapshotStateList<ChatMessage> = _currentChatMessages

    /** Flag indicating if response generation is in progress */
    val isGenerating = mutableStateOf(false)

    // region Message Handling & Generation
    // ========================================================================================
    //                          Public Message Methods
    // ========================================================================================
    fun sendMessage(message: String, isUser: Boolean = true) {
        viewModelScope.launch {
            if(isGenerating.value) stopGeneration()
            if(message.isBlank()) return@launch
            _currentChatMessages.add(ChatMessage(message, isUser))
            _currentChatMessages.add(ChatMessage("", false))
            isGenerating.value = true
            getTalkerResponse(message,{},{})
        }

    }

    fun stopGeneration() {
        isGenerating.value = false
        responseGenerationJob?.cancel()
    }
}