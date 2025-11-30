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
                    
                    // Update the last message in the chat with the generated image
                    if (_currentChatMessages.isNotEmpty()) {
                        val lastIndex = _currentChatMessages.lastIndex
                        val lastMessage = _currentChatMessages[lastIndex]
                        _currentChatMessages[lastIndex] = ChatMessage(
                            message = lastMessage.message,
                            isUser = false,
                            image = imageByteArray
                        )
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