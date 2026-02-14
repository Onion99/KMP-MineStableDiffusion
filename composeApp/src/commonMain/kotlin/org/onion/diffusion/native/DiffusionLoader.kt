package org.onion.diffusion.native

expect class DiffusionLoader(){
    suspend fun getModelFilePath():String
    fun loadModel(
        modelPath: String,
        vaePath: String = "",
        llmPath: String = "",
        clipLPath: String = "",
        clipGPath: String = "",
        t5xxlPath: String = "",
        offloadToCpu: Boolean = false,
        keepClipOnCpu: Boolean = false,
        keepVaeOnCpu: Boolean = false,
        diffusionFlashAttn: Boolean = false,
        enableMmap: Boolean = false,
        diffusionConvDirect: Boolean = false,
        wtype: Int = -1
    )
    fun txt2Img(
        prompt: String, negative: String,
        width: Int, height: Int,
        steps: Int, cfg: Float, seed: Long,
    ): ByteArray?

    fun release()

    suspend fun saveImage(imageData: ByteArray, fileName: String): Boolean
}