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
    private val defaultNegative = "worst quality, low quality, normal quality, blurry, pixelated, grainy, jpeg artifacts, noise, overexposed, underexposed, dark, dim, distorted, deformed, malformed, bad anatomy, bad hands, bad fingers, extra fingers, missing fingers, fused fingers, mutated fingers, blurry hands, blurry fingers, disproportionate hands, long fingers, short fingers, bad eyes, cross-eyed, misaligned eyes, empty eyes, distorted eyes, bad face, ugly face, disfigured face, asymmetrical face, mutated face, extra limbs, missing limbs, floating limbs, disconnected limbs, deformed limbs, bad body, disproportionate body, fat, skinny, emaciated, obese, watermark, text, signature, logo, username, stamp, caption, frame, border, duplicate, clone, redundant, overlapping, cut off, cropped, incomplete, messy hair, unkempt hair, stringy hair, bad hair texture, plastic skin, rubber skin, waxy skin, unnatural skin tone, gray skin, green skin, unnatural light, harsh light, flat light, no shadow, wrong shadow direction, cartoonish when aiming for realistic, realistic when aiming for cartoon, bad perspective, wrong proportion, distorted background, messy background, cluttered background"
    @OptIn(ExperimentalTime::class)
    fun getTalkerResponse(query: String, onCancelled: () -> Unit, onError: (Throwable) -> Unit){
        runCatching {
            responseGenerationJob = viewModelScope.launch(Dispatchers.Default) {
                isInferenceOn = true
                val duration = measureTime {
                    println("\n=== Image Generation Params===")
                    var negativeContent = defaultNegative
                    var promptContent = query
                    if(query.contains("|")){
                        val inputContent = query.split("|")
                        negativeContent = inputContent.last()
                        promptContent = inputContent.first()
                    }
                    println("Image prompt: $promptContent")
                    println("Image negative: $negativeContent")
                    // Call txt2Img to generate image from the query prompt
                    val imageByteArray = diffusionLoader.txt2Img(
                        prompt = promptContent,
                        negative = negativeContent,
                        // 768×1024（竖版人像）/ 1024×768（横版场景）/ 1024×1344（高清竖版）
                        width = 768,
                        height = 1024,
                        steps = 22,//模型渲染细节的 “迭代次数”，步数越多细节越丰富，但耗时越长（20-30 步性价比最高）
                        cfg = 6f,// 控制模型 “遵守正向提示词” 的严格程度，数值越高越贴合提示词，越低越自由发挥（7.0-9.0 最常用）
                        seed = Clock.System.now().toEpochMilliseconds()
                    )

                    // Debug logging to verify image format
                    println("=== Image Generation Debug ===")
                    println("Image size: ${imageByteArray?.size} bytes")
                    if (imageByteArray != null && imageByteArray.size >= 10) {
                        println("First 10 bytes: ${imageByteArray.take(10).joinToString { it.toString() }}")
                        // PNG signature: 137 80 78 71 13 10 26 10 (需要使用 and 0xFF 转换为无符号值)
                        // JPEG signature: 255 216 255
                        val isPNG = imageByteArray.size >= 8 &&
                                imageByteArray[0].toInt() and 0xFF == 137 &&
                                imageByteArray[1].toInt() and 0xFF == 80 &&
                                imageByteArray[2].toInt() and 0xFF == 78 &&
                                imageByteArray[3].toInt() and 0xFF == 71
                        val isJPEG = imageByteArray.size >= 3 &&
                                imageByteArray[0].toInt() and 0xFF == 255 &&
                                imageByteArray[1].toInt() and 0xFF == 216
                        println("Format detection - PNG: $isPNG, JPEG: $isJPEG")
                    }
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