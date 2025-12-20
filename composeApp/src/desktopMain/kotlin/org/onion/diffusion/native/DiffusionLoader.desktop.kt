package org.onion.diffusion.native

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import org.onion.diffusion.utils.NativeLibraryLoader

actual class DiffusionLoader actual constructor() {

    init {
        NativeLibraryLoader.loadFromResources("sdloader")
    }
    private var nativePtr = 0L

    actual suspend fun getModelFilePath(): String {
        return FileKit.openFilePicker()?.file?.absolutePath ?: ""
    }


    actual fun loadModel(modelPath: String) {
        nativePtr = nativeLoadModel(modelPath,true,true,true)
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

    private external fun nativeLoadModel(modelPath: String, offloadToCpu: Boolean, keepClipOnCpu: Boolean, keepVaeOnCpu: Boolean): Long
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