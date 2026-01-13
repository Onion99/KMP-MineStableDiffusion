package org.onion.diffusion.native

import androidx.core.net.toUri
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import java.io.File
import java.io.FileOutputStream

actual class DiffusionLoader actual constructor() {

    init {
        System.loadLibrary("sdloader")
    }

    private var nativePtr = 0L

    actual suspend fun getModelFilePath(): String {
        val androidFile = FileKit.openFilePicker(type = FileKitType.File(listOf(
            "safetensors","ckpt","pt","bin","gguf"
        )))
        val file = File(FileKit.context.filesDir, androidFile!!.name)
        if(file.exists()) return file.absolutePath
        FileKit.context.contentResolver.openInputStream((androidFile?.absolutePath() ?: return "").toUri()).use { inputStream ->
            FileOutputStream(File(FileKit.context.filesDir, androidFile.name)).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
        }
        return File(FileKit.context.filesDir, androidFile.name).absolutePath
    }

    actual fun loadModel(
        modelPath: String,
        vaePath: String,
        llmPath: String,
        useFlashAttn: Boolean
    ) {
        nativePtr = nativeLoadModel(
            modelPath,
            vaePath,
            llmPath,
            false,
            false,
            false,
            useFlashAttn
        )
    }

    actual fun release() {
        nativeRelease(nativePtr)
    }

    actual fun txt2Img(
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        steps: Int,
        cfg: Float,
        seed: Long
    ): ByteArray? = nativeTxt2Img(nativePtr,prompt,negative,width,height,steps,cfg,seed)


    private external fun nativeLoadModel(
        modelPath: String,
        vaePath: String,
        llmPath: String,
        offloadToCpu: Boolean,
        keepClipOnCpu: Boolean,
        keepVaeOnCpu: Boolean,
        useFlashAttn: Boolean
    ): Long

    private external fun nativeTxt2Img(
        handle: Long,
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        steps: Int,
        cfg: Float,
        seed: Long
    ): ByteArray?
    private external fun nativeRelease(handle: Long)

}