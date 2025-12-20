# ================= Fix: Disable Optimizations =================
# 【核心修复】禁用优化
# KMP Desktop 构建中，由于缺少 Android 类（虽然被 dontwarn 忽略），
# ProGuard 的优化步骤在分析类层次结构时会因找不到共同父类而崩溃 (IncompleteClassHierarchyException)。
# 禁用优化是解决此问题的标准做法，且不会影响混淆(Obfuscation)和缩减(Shrinking)。
-dontoptimize

# 【核心修复】禁用预校验
# ProGuard 的预校验步骤(Preverification)同样需要分析类层次结构以生成 StackMapTable。
# 当存在大量缺失的 Android 类引用时，会导致 IncompleteClassHierarchyException。
# 禁用预校验可跳过此步骤，且通常不会影响 JVM 运行（JVM 会在加载类时自行校验）。
-dontpreverify

# 移除会导致崩溃的激进优化选项
# -mergeinterfacesaggressively
# -overloadaggressively
# -repackageclasses
# -allowaccessmodification

# ================= Critical: Ignore Warnings for KMP =================
# 必须忽略警告，否则会报 "unresolved references"
-ignorewarnings

# ================= Attributes & Annotations =================
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable, Exceptions

# ================= Kotlin & Serialization =================
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <init>(...);
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class *$$serializer { *; }

# ================= Enum Optimizations =================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================= JNA & FileKit (Desktop Critical) =================
# 必须全量保留，防止 Native 调用签名不匹配
-keep class com.sun.jna.** { *; }
-keep class io.github.vinceglb.filekit.** { *; }

# ================= Ktor & Network =================
-keep class io.ktor.** { *; }

# ================= OkHttp & Okio =================
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ================= KMP Common Suppressions =================
# 1. Android classes
-dontwarn android.**
-dontwarn androidx.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.crypto.tink.**

# 2. Desktop classes
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.naming.**
-dontwarn javax.imageio.**
-dontwarn sun.misc.Unsafe

# 3. Logging & Others
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.log4j.**
-dontwarn kotlin.reflect.jvm.internal.**

#•记录v1
#移除所有宽泛的 -keep class package.** 规则，改用 R8 的静态分析能力（Tree Shaking）。
#精细化 JNA 等反射库的规则，只保留接口和结构体。
#增加 dontwarn 规则，屏蔽 KMP 跨平台产生的“类找不到”噪音
#1.移除全量 Keep：
#删除了 -keep class okhttp3.** { *; }、-keep class okio.** { *; } 等宽泛规则。OkHttp 和 Okio 等库现在都包含了消费者规则（Consumer Rules），R8/ProGuard 能自动识别并保留实际用到的代码（Tree Shaking），无需手动保留所有类。
#删除了 io.github.alexzhirkevich.** 和 com.jetbrains.** 的宽泛规则，这能显著减少包体积并避免因引用未使用类而导致的错误。
#2.精细化规则：
#JNA：只保留了继承自 Library、Callback 和 Structure 的必要部分，而不是整个包，这通常是 JNA 反射所需的最小集合。
#Serialization：只保留序列化相关的构造函数和方法，而不是整个类。
#3.解决类访问异常 (Class not found)：
#KMP 项目中，Android 平台打包时常会扫描到 Desktop/JVM 平台的代码引用（如 java.awt.*、javax.swing.*）。通过添加 java.awt.**、javax.swing.** 等的 -dontwarn，告诉编译器忽略这些在 Android 上不存在且不会被执行到的路径。
#反之，为 Desktop 构建保留了忽略 android.** 的规则。
#4.增加必要属性：
#添加了 -allowaccessmodification 允许 R8 优化访问权限（如果后续遇到 IllegalAccessError 可以移除）。
#添加了 Exceptions 属性保留，对协程异常传播有帮助
#.记录v2
#JNA/FileKit 崩溃：您看到的 Note: ... but not the descriptor class 警告意味着 ProGuard 保留了 Shell32 接口，但混淆了它方法签名中必须使用的 WString, Pointer 等 JNA 基础类。JNA 依赖这些类的原始名称进行 Native 映射，必须全量保留。
#2.
#构建中断：26万+ 个 unresolved references 是因为 KMP Desktop 打包时 Classpath 里没有 Android SDK，但您的公共代码引用了 Android 类。如果不加 -ignorewarnings，ProGuard 会认为这是代码缺陷并强行停止构建。
#•记录v3
#您开启了激进优化（Aggressive Optimizations）。ProGuard 的优化步骤需要对字节码进行深度分析（部分求值/Partial Evaluation），以计算变量的类型和栈帧。当它试图计算两个类型的“共同父类”时，如果涉及到的类在当前平台（Desktop）缺失（例如 android.* 类，虽然用 -dontwarn 忽略了警告，但类本身确实不存在），ProGuard 无法构建完整的继承树，从而导致崩溃。
#解决方案： 在 KMP Desktop 构建中，必须禁用优化 (-dontoptimize)。
#•
#这不会影响代码缩减（Tree Shaking/Shrinking，即删除无用代码）。
#•
#这不会影响混淆（Obfuscation，即类名/方法名重命名）。
#•
#它只是跳过了字节码级别的微优化（如内联、死代码消除等），这对于桌面应用体积和性能的影响微乎其微，但能确保构建通过