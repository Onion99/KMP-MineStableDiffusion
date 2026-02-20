package org.onion.diffusion.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onion.model.ChatMessage
import com.onion.model.LoraConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.onion.diffusion.native.DiffusionLoader
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.getString
import minediffusion.composeapp.generated.resources.Res
import minediffusion.composeapp.generated.resources.image_generation_finished
import minediffusion.composeapp.generated.resources.video_generation_finished
import org.onion.diffusion.getPlatform

class ChatViewModel  : ViewModel() {

    /** Format milliseconds into human-readable duration: "0.85s" / "12.3s" / "2m 15s" */
    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000.0
        return when {
            totalSeconds < 1.0 -> {
                val hundredths = (totalSeconds * 100).roundToInt()
                "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}s"
            }
            totalSeconds < 60.0 -> {
                val tenths = (totalSeconds * 10).roundToInt()
                "${tenths / 10}.${tenths % 10}s"
            }
            else -> {
                val minutes = (totalSeconds / 60).toInt()
                val seconds = (totalSeconds % 60).toInt()
                "${minutes}m ${seconds}s"
            }
        }
    }


    var diffusionLoader:DiffusionLoader = DiffusionLoader()
    var diffusionModelPath = mutableStateOf("")
    var vaePath = mutableStateOf("")
    var llmPath = mutableStateOf("")
    var clipLPath = mutableStateOf("")
    var clipGPath = mutableStateOf("")
    var t5xxlPath = mutableStateOf("")
    private var initModel = false
    // 0 default,1 loading,2 loading completely
    var loadingModelState = MutableStateFlow(0)
    var isDiffusionModelLoading = mutableStateOf(false)
    var isVaeModelLoading = mutableStateOf(false)
    var isLlmModelLoading = mutableStateOf(false)
    var isClipLModelLoading = mutableStateOf(false)
    var isClipGModelLoading = mutableStateOf(false)
    var isT5xxlModelLoading = mutableStateOf(false)
    
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

    /** Quantization Type - -1: Auto/Default, 0: F32, 1: F16, 2: Q4_0, etc. */
    var wtype = mutableStateOf(-1)

    /** Offload to CPU - offload model computations to CPU */
    var offloadToCpu = mutableStateOf(getPlatform().isIOS)

    /** Keep CLIP on CPU - keep CLIP model on CPU (enabled by default on macOS and iOS) */
    var keepClipOnCpu = mutableStateOf(getPlatform().isMacOS || getPlatform().isIOS)

    /** Keep VAE on CPU - keep VAE decoder on CPU */
    var keepVaeOnCpu = mutableStateOf(false)
    
    /** Enable MMAP - memory map the model weights */
    var enableMmap = mutableStateOf(false)


    /** Direct Convolution - optimize convolution in diffusion model */
    var diffusionConvDirect = mutableStateOf(false)

    // ========================================================================================
    //                              Video Generation Settings
    // ========================================================================================
    /** Video frames - number of frames to generate */
    var videoFrames = mutableStateOf(33)

    /** Flow Shift - controls temporal flow for video generation models (e.g. Wan2.1) */
    var flowShift = mutableStateOf(3.0f)

    // ========================================================================================
    //                              LoRA Settings
    // ========================================================================================
    val loraList = mutableStateListOf<LoraConfig>()

    fun addLora(path: String) {
        // Prevent duplicates
        if (loraList.any { it.path == path }) return
        
        // Extract filename for name
        val name = path.substringAfterLast('/').substringAfterLast('\\')
        loraList.add(LoraConfig(path = path, name = name))
    }

    fun removeLora(lora: LoraConfig) {
        loraList.remove(lora)
    }


    suspend fun selectLoraFile(): String {
        return diffusionLoader.getModelFilePath()
    }

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

    suspend fun selectClipLFile(): String{
        isClipLModelLoading.value = true
        val path = diffusionLoader.getModelFilePath()
        clipLPath.value = path
        isClipLModelLoading.value = false
        return path
    }

    suspend fun selectClipGFile(): String{
        isClipGModelLoading.value = true
        val path = diffusionLoader.getModelFilePath()
        clipGPath.value = path
        isClipGModelLoading.value = false
        return path
    }

    suspend fun selectT5xxlFile(): String{
        isT5xxlModelLoading.value = true
        val path = diffusionLoader.getModelFilePath()
        t5xxlPath.value = path
        isT5xxlModelLoading.value = false
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
                clipLPath = clipLPath.value,
                clipGPath = clipGPath.value,
                t5xxlPath = t5xxlPath.value,
                offloadToCpu = offloadToCpu.value,
                keepClipOnCpu = keepClipOnCpu.value,
                keepVaeOnCpu = keepVaeOnCpu.value,
                diffusionFlashAttn = diffusionFlashAttn.value,
                enableMmap = enableMmap.value,
                diffusionConvDirect = diffusionConvDirect.value,
                wtype = wtype.value,
                flowShift = flowShift.value
            )

            println("=== Model Path ===")
            println("Model Path: ${diffusionModelPath.value}")
            println("VAE Path: ${vaePath.value}")
            println("LLM Path: ${llmPath.value}")
            println("CLIP-L Path: ${clipLPath.value}")
            println("CLIP-G Path: ${clipGPath.value}")
            println("T5XXL Path: ${t5xxlPath.value}")
            loadingModelState.emit(2)
        }
    }

    private var responseGenerationJob: Job? = null
    private var isInferenceOn: Boolean = false
    private val defaultNegative = ""
    @OptIn(ExperimentalTime::class)
    fun getImageTalkerResponse(query: String, onCancelled: () -> Unit, onError: (Throwable) -> Unit){
        runCatching {
            responseGenerationJob = viewModelScope.launch(Dispatchers.Default) {
                isInferenceOn = true
                val duration = measureTime {
                    println("\n=== Image Generation Params===")
                    var negativeContent = ""
                    var promptContent = query
                    if(query.contains("|")){
                        val inputContent = query.split("|")
                        negativeContent = inputContent.last()
                        promptContent = inputContent.first()
                    }
                    println("Image prompt: $promptContent")
                    println("Image negative: $negativeContent")
                    // Call txt2Img to generate image from the query prompt
                    val startTime = Clock.System.now().toEpochMilliseconds()

                    val enabledLoras = loraList.filter { it.isEnabled }
                    val loraPaths = enabledLoras.map { it.path }.toTypedArray()
                    val loraStrengths = enabledLoras.map { it.strength }.toFloatArray()

                    val imageByteArray = diffusionLoader.txt2Img(
                        prompt = promptContent,
                        negative = negativeContent,
                        // 768×1024（竖版人像）/ 1024×768（横版场景）/ 1024×1344（高清竖版）
                        width = imageWidth.value,
                        height = imageHeight.value,
                        steps = generationSteps.value,//模型渲染细节的 “迭代次数”，步数越多细节越丰富，但耗时越长（20-30 步性价比最高）
                        cfg = cfgScale.value,// 控制模型 “遵守正向提示词” 的严格程度，数值越高越贴合提示词，越低越自由发挥（7.0-9.0 最常用）
                        seed = Clock.System.now().toEpochMilliseconds(),
                        loraPaths = loraPaths,
                        loraStrengths = loraStrengths
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
                        val generationDuration = Clock.System.now().toEpochMilliseconds() - startTime
                        val msg = getString(Res.string.image_generation_finished).replace("%s", formatDuration(generationDuration))
                        val metadata = mapOf(
                            "prompt" to promptContent,
                            "negative_prompt" to negativeContent,
                            "steps" to generationSteps.value.toString(),
                            "cfg_scale" to cfgScale.value.toString(),
                            "seed" to Clock.System.now().toEpochMilliseconds().toString(), // Note: verify if we should use the same seed as generation
                            "model" to diffusionModelPath.value.substringAfterLast("/"),
                             "loras" to enabledLoras.joinToString(",") { "${it.name}:${it.strength}" }
                        )

                        _currentChatMessages.add(lastIndex, ChatMessage(
                            message = msg,
                            isUser = false,
                            image = imageByteArray,
                            metadata = metadata
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

    @OptIn(ExperimentalTime::class)
    fun getVideoTalkerResponse(query: String, onCancelled: () -> Unit, onError: (Throwable) -> Unit){
        runCatching {
            responseGenerationJob = viewModelScope.launch(Dispatchers.Default) {
                isInferenceOn = true
                val duration = measureTime {
                    println("\n=== Video Generation Params===")
                    var negativeContent = defaultNegative
                    var promptContent = query
                    if(query.contains("|")){
                        val inputContent = query.split("|")
                        negativeContent = inputContent.last()
                        promptContent = inputContent.first()
                    }
                    println("Video prompt: $promptContent")
                    println("Video negative: $negativeContent")
                    println("Video frames: ${videoFrames.value}")
                    // Call videoGen to generate video frames
                    val startTime = Clock.System.now().toEpochMilliseconds()
                    
                    val enabledLoras = loraList.filter { it.isEnabled }
                    val loraPaths = enabledLoras.map { it.path }.toTypedArray()
                    val loraStrengths = enabledLoras.map { it.strength }.toFloatArray()
                    
                    val frames = diffusionLoader.videoGen(
                        prompt = promptContent,
                        negative = negativeContent,
                        width = imageWidth.value,
                        height = imageHeight.value,
                        videoFrames = videoFrames.value,
                        steps = generationSteps.value,
                        cfg = cfgScale.value,
                        seed = Clock.System.now().toEpochMilliseconds(),
                        sampleMethod = 0, // EULER_SAMPLE_METHOD
                        loraPaths = loraPaths,
                        loraStrengths = loraStrengths
                    )

                    println("=== Video Generation Debug ===")
                    println("Frames generated: ${frames?.size}")
                    frames?.forEachIndexed { index, frameData ->
                        println("Frame $index: ${frameData.size} bytes")
                    }
                    println("==============================")

                    // Update the last message in the chat with the generated video frames
                    if (_currentChatMessages.isNotEmpty()) {
                        val lastIndex = _currentChatMessages.lastIndex
                        _currentChatMessages.removeAt(lastIndex)
                        val generationDuration = Clock.System.now().toEpochMilliseconds() - startTime
                        val msg = getString(Res.string.video_generation_finished)
                            .replaceFirst("%s", formatDuration(generationDuration))
                            .replaceFirst("%s", "${frames?.size ?: 0}")
                        val metadata = mapOf(
                            "prompt" to promptContent,
                            "negative_prompt" to negativeContent,
                            "video_frames" to videoFrames.value.toString(),
                            "steps" to generationSteps.value.toString(),
                            "cfg_scale" to cfgScale.value.toString(),
                            "seed" to Clock.System.now().toEpochMilliseconds().toString(),
                            "model" to diffusionModelPath.value.substringAfterLast("/"),
                            "loras" to enabledLoras.joinToString(",") { "${it.name}:${it.strength}" }
                        )
                        _currentChatMessages.add(lastIndex, ChatMessage(
                            message = msg,
                            isUser = false,
                            videoFrames = frames,
                            metadata = metadata
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
            getImageTalkerResponse(message,{},{})
        }

    }

    fun stopGeneration() {
        isGenerating.value = false
        responseGenerationJob?.cancel()
        val lastIndex = _currentChatMessages.lastIndex
        _currentChatMessages.removeAt(lastIndex)
    }
}