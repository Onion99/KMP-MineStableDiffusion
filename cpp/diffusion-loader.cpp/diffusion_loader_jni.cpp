
#include <jni.h>
#include "stable-diffusion.h"
#include <iostream>
#include <vector>

#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "../stable-diffusion.cpp/thirdparty/stb_image_write.h"

///////////////////////////////////////////////////////////////////////////
// 一个自定义的结构体,包装了真正的 sd_ctx_t* 指针。这样做可以方便未来扩展，比如在 SdHandle 中增加更多的状态信息
///////////////////////////////////////////////////////////////////////////
struct SdHandle{
    sd_ctx_t* ctx = nullptr;
    int last_width = 0;
    int last_height = 0;
};

// 用于收集 PNG 数据的回调函数上下文
struct PngWriteContext {
    std::vector<unsigned char> data;
};

// PNG 写入回调函数 - 将数据追加到 vector 中
static void png_write_callback(void* context, void* data, int size) {
    PngWriteContext* ctx = static_cast<PngWriteContext*>(context);
    unsigned char* bytes = static_cast<unsigned char*>(data);
    ctx->data.insert(ctx->data.end(), bytes, bytes + size);
}

///////////////////////////////////////////////////////////////////////////
// •extern "C"：告诉 C++ 编译器，这个函数要使用 C 语言的调用约定，以避免 C++ 的名称修饰（name mangling），确保 Java 虚拟机（JVM）能够通过函数名找到它。
// •JNIEXPORT 和 JNICALL：是 JNI 的宏，用于确保函数在动态链接库（.so 文件）中是可见的，并且遵循正确的调用约定。
// •jlong：这是函数的返回类型。jlong 对应 Java 中的 long 类型。这里它将被用来返回一个内存地址（指针），作为 C++ 对象的句柄。
// •JNIEnv* env：一个指向 JNI 环境的指针。它提供了大多数 JNI 函数的接口，比如下面会用到的字符串转换、对象操作等。
// •jclass  clazz：被调用是时static 方法,就用jclass,拿到调用者静态类
// •jobject clazz：被调用是时类方法,就用jobject,拿到调用者实例类
///////////////////////////////////////////////////////////////////////////
extern "C" JNIEXPORT jlong JNICALL
Java_org_onion_diffusion_native_DiffusionLoader_nativeLoadModel(
        JNIEnv *env,jobject clazz,
        jstring jModelPath,
        jboolean offloadToCpu,jboolean keepClipOnCpu,jboolean keepVaeOnCpu){
    //(void)clazz这是一个常见的技巧，用来告诉编译器 clazz 这个参数我们在此函数中没有使用，以避免编译器发出 "unused parameter" (未使用参数) 的警告
    (void) clazz;

    const char* modelPath = jModelPath ? env->GetStringUTFChars(jModelPath, nullptr) : nullptr;

    printf("Initializing Stable Diffusion with:");

    // 创建并初始化参数结构体
    sd_ctx_params_t p{};
    sd_ctx_params_init(&p);
    // 配置参数,将从 Java 传来的参数赋值给 C++ 结构体
    p.model_path = modelPath ? modelPath : "";
    // 不要立即释放资源,以方便下次使用
    p.free_params_immediately = false;
    p.n_threads = get_num_physical_cores();
    p.offload_params_to_cpu = offloadToCpu;
    p.keep_clip_on_cpu = keepClipOnCpu;
    p.keep_vae_on_cpu = keepVaeOnCpu;

    // 创建 Stable Diffusion 上下文（核心步骤
    sd_ctx_t* SdContext = new_sd_ctx(&p);

    // 释放资源,GetStringUTFChars 在内存中创建了 C 字符串的副本。当 C++ 代码不再需要这些字符串时，必须调用 ReleaseStringUTFChars 来释放它们，否则会造成内存泄漏
    if (jModelPath) env->ReleaseStringUTFChars(jModelPath, modelPath);

    // 处理创建失败的情况
    if (!SdContext) {
        printf("Failed to create sd_ctx");
        return 0;
    }

    // 创建句柄并返回
    auto* handle = new SdHandle();
    // 代码创建了一个 SdHandle 对象，并将刚刚成功创建的 ctx 存入其中
    handle->ctx = SdContext;
    // 另一个 JNI 的关键技巧。C++ 的指针（如 handle）不能直接传递给 Java，因为 Java 没有指针的概念。所以，这里将指针的内存地址强制转换为一个 jlong（64位长整型）
    // Java 代码会接收到这个 long 类型的数字。虽然 Java 不理解这个数字的含义，但它可以安全地存储它。当 Java 之后需要调用其他 native 方法（如 nativeTxt2Img 或 nativeDestroy）时，会把这个 long 值再传回 C++。C++ 代码再通过 reinterpret_cast<SdHandle*> 将其转回原来的指针，从而找到正确的 Stable Diffusion 实例进行操作
    return reinterpret_cast<jlong>(handle);
}


///////////////////////////////////////////////////////////////////////////
// •jbyteArray: 返回类型。它对应 Java 中的 byte[]。一开始传递原始图像数据（比如 RGB 像素序列）最理想的格式,现在返回 PNG 格式的图像数据
// •jjlong handlePtr: 之前 nativeLoadModel 返回的句柄，用于找到正确的 C++ 端的 StableDiffusion 句柄指针（以 long 形式从 Java 传入）
// •jPrompt/jNegative: 正向和负向提示词
// •width/height: 生成图像的尺寸
// •steps: 采样步数
// •cfg: Classifier-Free Guidance 强度（控制提示词的影响力）
// •seed: 随机种子
///////////////////////////////////////////////////////////////////////////
extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_onion_diffusion_native_DiffusionLoader_nativeTxt2Img(JNIEnv* env, jobject thiz, jlong handlePtr,
                                                              jstring jPrompt, jstring jNegative,
                                                              jint width, jint height,
                                                              jint steps, jfloat cfg, jlong seed){
    (void)thiz;
    // 如果句柄是 0，说明初始化失败或从未初始化
    if (handlePtr == 0) {
        printf("StableDiffusion not initialized");
        return nullptr;
    }

    // 将 long类型的数字转译回它本来的面目——一个指向 SdHandle 结构体的 C++ 指针
    auto* handle = reinterpret_cast<SdHandle*>(handlePtr);
    // 将 JVM 的 jstring 转换为 C++ 可以使用的 const char* 字符串
    const char* prompt = jPrompt ? env->GetStringUTFChars(jPrompt, nullptr) : "";
    const char* negative = jNegative ? env->GetStringUTFChars(jNegative, nullptr) : "";

    // 初始化采样参数结构体
    sd_sample_params_t sample{};
    sd_sample_params_init(&sample);
    if (steps > 0) sample.sample_steps = steps;
    sample.guidance.txt_cfg = cfg > 0 ? cfg : 7.0f;
    // 初始化生成参数结构体
    sd_img_gen_params_t gen{};
    sd_img_gen_params_init(&gen);
    gen.prompt = prompt;
    gen.negative_prompt = negative;
    gen.width = width;
    gen.height = height;
    gen.sample_params = sample;
    gen.seed = seed;
    gen.batch_count = 1;

    // 生成图像,执行完毕后，它返回一个指向 sd_image_t 结构体的指针 out。这个结构体包含了生成图像的所有信息（宽度、高度、通道数，以及一个指向原始像素数据内存的指针 data
    sd_image_t* out = generate_image(handle->ctx, &gen);

    // 释放资源,清理输入参数的 C++ 副本
    if (jPrompt) env->ReleaseStringUTFChars(jPrompt, prompt);
    if (jNegative) env->ReleaseStringUTFChars(jNegative, negative);

    // 检查图像是否生成成功。如果失败，generate_image 会返回 nullptr 或者 data 成员是 nullptr
    if (!out || !out[0].data) {
        printf("generate_image failed");
        return nullptr;
    }

    // 使用回调函数将生成的原始 RGB 数据编码为 PNG 格式
    PngWriteContext png_ctx;
    int result = stbi_write_png_to_func(
        png_write_callback,                   // 写入回调函数
        &png_ctx,                             // 回调上下文
        out[0].width,                         // 图像宽度
        out[0].height,                        // 图像高度
        out[0].channel,                       // 通道数 (RGB=3, RGBA=4)
        out[0].data,                          // 原始 RGB 数据
        out[0].width * out[0].channel         // stride (每行字节数)
    );

    // 检查 PNG 编码是否成功
    if (!result || png_ctx.data.empty()) {
        printf("PNG encoding failed");
        free(out[0].data);
        free(out);
        return nullptr;
    }

    // 创建 Java 字节数组来存储 PNG 数据
    jbyteArray jbytes = env->NewByteArray((jsize)png_ctx.data.size());
    if (!jbytes) {
        free(out[0].data);
        free(out);
        return nullptr;
    }

    // 将 PNG 数据复制到 Java 字节数组
    env->SetByteArrayRegion(jbytes, 0, (jsize)png_ctx.data.size(),reinterpret_cast<jbyte*>(png_ctx.data.data()));
    // 清理 C++ 分配的内存 (PNG 数据在 vector 中会自动释放)
    free(out[0].data);   // 释放原始 RGB 数据
    free(out);           // 释放图像结构体

    return jbytes;
}


///////////////////////////////////////////////////////////////////////////
// •JNIEnv*, jobject: 这两个参数在这里没有被使用，所以连名字都没写。
// •jlong handlePtr: 这是最重要的参数。它就是 loadModel 函数最后返回的那个 long 型数字。这个数字本质上是 C++ 对象的内存地址，是连接 Java 世界和特定 C++ 实例的唯一"钥匙"
///////////////////////////////////////////////////////////////////////////
extern "C" JNIEXPORT void JNICALL
Java_org_onion_diffusion_native_DiffusionLoader_nativeRelease(JNIEnv*, jobject, jlong handlePtr){
    // loadModel 在失败时会返回 0。如果 Java 代码持有一个为 0 的句柄并尝试调用 destroy，这个检查可以防止后续代码尝试操作一个空地址，从而避免程序崩溃。它直接 return，什么也不做，这是正确的行为
    if (handlePtr == 0) return;
    // 是 loadModel 中 reinterpret_cast<jlong>(handle) 的逆向操作。
    // •它告诉编译器:"把这个 long 类型的整数 handlePtr 重新解释（cast）为它本来的样子——一个指向 SdHandle 结构体的指针"。•执行完这行代码后，handle 就成了一个有效的 C++ 指针，我们可以通过它访问到之前创建的 SdHandle 对象以及它内部的 ctx（Stable Diffusion 引擎实例）
    auto* handle = reinterpret_cast<SdHandle*>(handlePtr);
    // 安全检查。它确保 ctx 指针不是空的,指针是不是指向ctx。这可以防止在已经销毁过一次的对象上再次调用 free_sd_ctx（这会导致"double free"错误，通常会使程序崩溃)
    if (handle->ctx) {
        // 清理工作,它调用了 stable-diffusion.cpp 库提供的函数，来释放 sd_ctx_t 对象所占用的所有资源。这包括加载到内存中的模型权重、各种计算缓冲区等等，通常会释放几百MB甚至上GB的内存
        free_sd_ctx(handle->ctx);
        // 好的编程习惯,在释放了指针指向的内存后，立即将指针本身设为 nullptr。这样，如果代码不小心第二次进入这个函数，上面的 if (handle->ctx) 检查就会失败，从而安全地阻止了"double free"错误
        handle->ctx = nullptr;
    }
    // 释放句柄本身的内存,在 loadModel 中，我们使用了 new SdHandle() 来分配 SdHandle 这个包装结构体。在 C++ 中，任何通过 new 分配的内存都必须通过 delete 来释放
    delete handle;
}