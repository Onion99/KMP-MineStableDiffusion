@file:OptIn(ExperimentalForeignApi::class)

package org.onion.diffusion.native

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.saveImageToGallery
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.Foundation.getBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.free
import sdloader.*

actual class DiffusionLoader actual constructor() {

    private var ctx: CPointer<cnames.structs.sd_ctx_t>? = null

    actual suspend fun getModelFilePath(): String {
        return FileKit.openFilePicker()?.path ?: ""
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
        memScoped {
            val params = alloc<sd_ctx_params_t>()
            sd_ctx_params_init(params.ptr)

            // 与 JNI 逻辑一致：如果提供了额外路径，使用 diffusion_model_path，否则使用 model_path
            val hasExtraPaths = vaePath.isNotEmpty() || llmPath.isNotEmpty() ||
                    clipLPath.isNotEmpty() || clipGPath.isNotEmpty() || t5xxlPath.isNotEmpty()

            if (hasExtraPaths) {
                params.diffusion_model_path = modelPath.cstr.ptr
            } else {
                params.model_path = modelPath.cstr.ptr
            }

            if (vaePath.isNotEmpty()) params.vae_path = vaePath.cstr.ptr
            if (llmPath.isNotEmpty()) params.llm_path = llmPath.cstr.ptr
            if (clipLPath.isNotEmpty()) params.clip_l_path = clipLPath.cstr.ptr
            if (clipGPath.isNotEmpty()) params.clip_g_path = clipGPath.cstr.ptr
            if (t5xxlPath.isNotEmpty()) params.t5xxl_path = t5xxlPath.cstr.ptr

            params.free_params_immediately = false
            params.n_threads = sd_get_num_physical_cores()
            params.offload_params_to_cpu = offloadToCpu
            params.keep_clip_on_cpu = keepClipOnCpu
            params.keep_vae_on_cpu = keepVaeOnCpu
            params.diffusion_flash_attn = diffusionFlashAttn
            params.enable_mmap = enableMmap
            params.diffusion_conv_direct = diffusionConvDirect

            if (wtype != -1) {
                params.wtype = wtype.toUInt()
            }
            if (flowShift.isFinite()) {
                params.flow_shift = flowShift
            }

            ctx = new_sd_ctx(params.ptr)
        }
    }

    actual fun release() {
        ctx?.let { sdCtx ->
            free_sd_ctx(sdCtx)
            ctx = null
        }
    }

    actual fun txt2Img(
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        steps: Int,
        cfg: Float,
        seed: Long
    ): ByteArray? {
        val sdCtx = ctx ?: return null

        return memScoped {
            val genParams = alloc<sd_img_gen_params_t>()
            sd_img_gen_params_init(genParams.ptr)
            // 初始化内嵌的采样参数
            sd_sample_params_init(genParams.sample_params.ptr)
            if (steps > 0) genParams.sample_params.sample_steps = steps
            genParams.sample_params.guidance.txt_cfg = if (cfg > 0) cfg else 7.0f

            genParams.prompt = prompt.cstr.ptr
            genParams.negative_prompt = negative.cstr.ptr
            genParams.width = width
            genParams.height = height
            genParams.seed = seed
            genParams.batch_count = 1

            val out = generate_image(sdCtx, genParams.ptr) ?: return null
            val image = out.pointed

            if (image.data == null) {
                free(out)
                return null
            }

            // data mapped to uint8_t* in C, which is usually UByteVar in Kotlin/Native
            val pngData = encodeImageToPng(
                image.data!!.reinterpret(), image.width.toInt(), image.height.toInt(), image.channel.toInt()
            )

            free(image.data)
            free(out)
            pngData
        }
    }

    actual fun videoGen(
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        videoFrames: Int,
        steps: Int,
        cfg: Float,
        seed: Long,
        sampleMethod: Int
    ): List<ByteArray>? {
        val sdCtx = ctx ?: return null

        return memScoped {
            val genParams = alloc<sd_vid_gen_params_t>()
            sd_vid_gen_params_init(genParams.ptr)

            genParams.prompt = prompt.cstr.ptr
            genParams.negative_prompt = negative.cstr.ptr
            genParams.width = width
            genParams.height = height
            genParams.video_frames = videoFrames
            genParams.seed = seed

            // 初始化内嵌的采样参数
            sd_sample_params_init(genParams.sample_params.ptr)
            if (steps > 0) genParams.sample_params.sample_steps = steps
            genParams.sample_params.guidance.txt_cfg = if (cfg > 0) cfg else 7.0f

            val numFrames = alloc<IntVar>()
            val frames = generate_video(sdCtx, genParams.ptr, numFrames.ptr) ?: return null
            val frameCount = numFrames.value

            if (frameCount <= 0) {
                free(frames)
                return null
            }

            val result = mutableListOf<ByteArray>()
            for (i in 0 until frameCount) {
                val frame = frames[i]
                if (frame.data != null) {
                    encodeImageToPng(
                        frame.data!!.reinterpret(), frame.width.toInt(), frame.height.toInt(), frame.channel.toInt()
                    )?.let { result.add(it) }
                    free(frame.data)
                }
            }
            free(frames)
            result.ifEmpty { null }
        }
    }

    actual suspend fun saveImage(imageData: ByteArray, fileName: String): Boolean {
        return try {
            FileKit.saveImageToGallery(imageData, fileName)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 使用 iOS 原生 CoreGraphics/UIKit API 将原始 RGB/RGBA 像素数据编码为 PNG ByteArray
     */
    private fun encodeImageToPng(
        data: CPointer<UByteVar>,
        width: Int,
        height: Int,
        channels: Int
    ): ByteArray? {
        val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return null
        val bytesPerRow = width * channels

        // CGImageCreate 支持 24bpp (RGB) 和 32bpp (RGBA)
        val bitmapInfo: UInt = if (channels == 4) {
            CGImageAlphaInfo.kCGImageAlphaLast.value
        } else {
            CGImageAlphaInfo.kCGImageAlphaNone.value
        }

        val dataProvider = CGDataProviderCreateWithData(
            info = null,
            data = data,
            size = (bytesPerRow * height).toULong(),
            releaseData = null
        )

        if (dataProvider == null) {
            CGColorSpaceRelease(colorSpace)
            return null
        }

        val cgImage = CGImageCreate(
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bitsPerPixel = (8 * channels).toULong(),
            bytesPerRow = bytesPerRow.toULong(),
            space = colorSpace,
            bitmapInfo = bitmapInfo,
            provider = dataProvider,
            decode = null,
            shouldInterpolate = false,
            intent = CGColorRenderingIntent.kCGRenderingIntentDefault
        )

        CGDataProviderRelease(dataProvider)
        CGColorSpaceRelease(colorSpace)

        if (cgImage == null) return null

        val uiImage = UIImage.imageWithCGImage(cgImage)
        CGImageRelease(cgImage)

        val nsData = UIImagePNGRepresentation(uiImage) ?: return null
        val byteArray = ByteArray(nsData.length.toInt())

        // Correct way to access bytes from NSData in Kotlin/Native
        byteArray.usePinned { pinned ->
            nsData.getBytes(pinned.addressOf(0), nsData.length)
        }
        return byteArray
    }
}
