package org.onion.diffusion.native

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.saveImageToGallery
import org.onion.diffusion.utils.PngMetadata
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
        androidFile ?: return ""
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
        clipLPath: String,
        clipGPath: String,
        t5xxlPath: String,
        offloadToCpu: Boolean,
        keepClipOnCpu: Boolean,
        keepVaeOnCpu: Boolean,
        diffusionFlashAttn: Boolean,
        enableMmap: Boolean,
        diffusionConvDirect: Boolean,
        wtype: Int,
        flowShift: Float
    ) {
        nativePtr = nativeLoadModel(
            modelPath,
            vaePath,
            llmPath,
            clipLPath,
            clipGPath,
            t5xxlPath,
            offloadToCpu,
            keepClipOnCpu,
            keepVaeOnCpu,
            diffusionFlashAttn,
            enableMmap,
            diffusionConvDirect,
            wtype,
            flowShift
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
        seed: Long,
        loraPaths: Array<String>?,
        loraStrengths: FloatArray?
    ): ByteArray? = nativeTxt2Img(nativePtr,prompt,negative,width,height,steps,cfg,seed,loraPaths,loraStrengths)

    actual fun videoGen(
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        videoFrames: Int,
        steps: Int,
        cfg: Float,
        seed: Long,
        sampleMethod: Int,
        loraPaths: Array<String>?,
        loraStrengths: FloatArray?
    ): List<ByteArray>? {
        val result = nativeVideoGen(nativePtr, prompt, negative, width, height, videoFrames, steps, cfg, seed, sampleMethod, loraPaths, loraStrengths)
        return result?.toList()
    }

    actual suspend fun saveImage(imageData: ByteArray, fileName: String, metadata: Map<String, String>?): Boolean {
        return try {
            val finalBytes = if (metadata != null) {
                PngMetadata.inject(imageData, metadata)
            } else {
                imageData
            }
            FileKit.saveImageToGallery(finalBytes,fileName)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private external fun nativeLoadModel(
        modelPath: String,
        vaePath: String,
        llmPath: String,
        clipLPath: String,
        clipGPath: String,
        t5xxlPath: String,
        offloadToCpu: Boolean,
        keepClipOnCpu: Boolean,
        keepVaeOnCpu: Boolean,
        diffusionFlashAttn: Boolean,
        enableMmap: Boolean,
        diffusionConvDirect: Boolean,
        wtype: Int,
        flowShift: Float
    ): Long

    private external fun nativeTxt2Img(
        handle: Long,
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        steps: Int,
        cfg: Float,
        seed: Long,
        loraPaths: Array<String>? = null,
        loraStrengths: FloatArray? = null
    ): ByteArray?

    private external fun nativeVideoGen(
        handle: Long,
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        videoFrames: Int,
        steps: Int,
        cfg: Float,
        seed: Long,
        sampleMethod: Int,
        loraPaths: Array<String>? = null,
        loraStrengths: FloatArray? = null
    ): Array<ByteArray>?

    private external fun nativeRelease(handle: Long)

}