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

    var diffusionLoader:DiffusionLoader = DiffusionLoader()
    var diffusionModelPath = mutableStateOf("")
    var vaePath = mutableStateOf("")
    var llmPath = mutableStateOf("")
    private var initModel = false
    // 0 default,1 loading,2 loading completely
    var loadingModelState = MutableStateFlow(0)
    var isDiffusionModelLoading = mutableStateOf(false)
    var isVaeModelLoading = mutableStateOf(false)
    var isLlmModelLoading = mutableStateOf(false)
    
    // ========================================================================================
    //                              Image Generation Settings
    // ========================================================================================
    /** Image width - options: 128, 256, 512, 768, 1024 */
    var imageWidth = mutableStateOf(512)
    
    /** Image height - options: 128, 256, 512, 768, 1024 */
    var imageHeight = mutableStateOf(512)
    
    /** Steps for generation - range: 1-50 */
    var generationSteps = mutableStateOf(5)
    
    /** CFG Scale - range: 1.0-15.0 */
    var cfgScale = mutableStateOf(2f)

    /** Flash Attention - optimize memory usage */
    var diffusionFlashAttn = mutableStateOf(false)

    /** Quantization Type - 0: F32, 1: F16, 2: Q4_0, etc. */
    var wtype = mutableStateOf(0)

    /** Offload to CPU - offload model computations to CPU */
    var offloadToCpu = mutableStateOf(false)

    /** Keep CLIP on CPU - keep CLIP model on CPU */
    var keepClipOnCpu = mutableStateOf(false)

    /** Keep VAE on CPU - keep VAE decoder on CPU */
    var keepVaeOnCpu = mutableStateOf(false)


    suspend fun selectDiffusionModelFile(): String{
        isDiffusionModelLoading.value = true
        val diffusionModelPath = diffusionLoader.getModelFilePath()
        this.diffusionModelPath.value = diffusionModelPath
        isDiffusionModelLoading.value = false
        return diffusionModelPath
    }

    suspend fun selectVaeFile(): String{
        isVaeModelLoading.value = true
        val path = diffusionLoader.getModelFilePath()
        vaePath.value = path
        isVaeModelLoading.value = false
        return path
    }

    suspend fun selectLlmFile(): String{
        isLlmModelLoading.value = true
        val path = diffusionLoader.getModelFilePath()
        llmPath.value = path
        isLlmModelLoading.value = false
        return path
    }

    fun initLLM(){
        if(initModel) return
        initModel = true
        viewModelScope.launch(Dispatchers.Default) {
            loadingModelState.emit(1)
            diffusionLoader.loadModel(
                modelPath = diffusionModelPath.value,
                vaePath = vaePath.value,
                llmPath = llmPath.value,
                offloadToCpu = offloadToCpu.value,
                keepClipOnCpu = keepClipOnCpu.value,
                keepVaeOnCpu = keepVaeOnCpu.value,
                diffusionFlashAttn = diffusionFlashAttn.value,
                wtype = wtype.value
            )
            println("=== Model Path ===")
            println("Model Path: ${diffusionModelPath.value}")
            println("VAE Path: ${vaePath.value}")
            println("LLM Path: ${llmPath.value}")
            loadingModelState.emit(2)
        }
    }

    private var responseGenerationJob: Job? = null
    private var isInferenceOn: Boolean = false
    private val defaultNegative = "worst quality,low quality,ugly,blurry"
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
                        width = imageWidth.value,
                        height = imageHeight.value,
                        steps = generationSteps.value,//模型渲染细节的 “迭代次数”，步数越多细节越丰富，但耗时越长（20-30 步性价比最高）
                        cfg = cfgScale.value,// 控制模型 “遵守正向提示词” 的严格程度，数值越高越贴合提示词，越低越自由发挥（7.0-9.0 最常用）
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
        val lastIndex = _currentChatMessages.lastIndex
        _currentChatMessages.removeAt(lastIndex)
    }
}