# ================= Aggressive Optimizations =================
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses
# 允许修改访问权限以优化（可选，若出现IllegalAccessError请注释掉）
-allowaccessmodification

# ================= Attributes & Annotations =================
# 保留协程、反射、序列化及Crash堆栈所需的属性
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable, Exceptions

# ================= Kotlin & Serialization =================
# 只保留序列化类的构造方法和生成的方法，而不是整个类
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <init>(...);
    @kotlinx.serialization.Serializable <methods>;
}
# 保留序列化伴生对象生成的 serializer() 方法
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# 保留生成的 Serializer 类
-keep class *$$serializer { *; }

# ================= Enum Optimizations =================
# 确保枚举的 values 和 valueOf 不被移除，防止反射调用崩溃
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================= OkHttp & Okio =================
# 移除宽泛的 keep 规则，OkHttp 现代版本支持 R8 自动分析
# 忽略 OkHttp 内部引用的可选平台依赖（Conscrypt, BouncyCastle 等）
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# 如果使用了 Okio 的 FakeFileSystem（通常用于测试），在 Release 中建议移除或只保留特定部分
# -keep class okio.fakefilesystem.** { *; }

# ================= JNA (Desktop/Native Access) =================
# 仅保留 JNA 必需的接口和结构体，而不是整个包
-keep interface * extends com.sun.jna.Library { public *; }
-keep interface * extends com.sun.jna.Callback { public *; }
-keep class * extends com.sun.jna.Structure { public *; }
-keep class com.sun.jna.Native { public *; }
# 忽略 JNA 可能引用的 AWT/Swing 类（如果在 Android 上构建）
-dontwarn com.sun.jna.**

# ================= Third Party Libraries =================
# 移除 io.github.alexzhirkevich.** 的全量 keep，R8 会自动保留代码中引用的 UI 组件
# 移除 com.jetbrains.** 的全量 keep，这会导致极大的包体积和 AWT 类缺失错误

# ================= KMP Common Suppressions =================
# 解决 "Class not found" 问题的关键：忽略跨平台库中未使用的平台类引用

# 1. 如果是 Android 构建，忽略 Desktop/JVM 相关类
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.naming.**
-dontwarn javax.imageio.**
-dontwarn sun.misc.Unsafe

# 2. 如果是 Desktop 构建，忽略 Android 相关类
-dontwarn android.**
-dontwarn androidx.**
-dontwarn com.google.android.gms.**

# 3. 忽略日志框架的可选依赖
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.log4j.**

# 4. 忽略 Kotlin 内部的一些元数据检查警告
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