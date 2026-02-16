import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.util.Locale
import java.lang.System.getenv

plugins {
    id("android-application-convention")
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        val libDir = if (iosTarget.name == "iosArm64") "ios-device" else "ios-simulator"
        iosTarget.binaries.all {
            linkerOpts += listOf(
                "-L${project.file("${rootProject.projectDir}/cpp/libs/$libDir")}",
                "-lstable-diffusion",
                "-lggml",
                "-lggml-base",
                "-lggml-cpu",
                "-lggml-blas",
                "-lggml-metal",
                "-framework", "Metal",
                "-framework", "MetalPerformanceShaders",
                "-framework", "Foundation",
                "-framework", "Accelerate"
            )
        }
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            // ---- Resource,KMP目前无法跨模块获取Res ------
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            // ---- DI ------
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // ---- App Runtime ------
            implementation(libs.runtime.shapes)
            implementation(libs.runtime.navigation)
            implementation(libs.runtime.savedstate)
            implementation(libs.runtime.viewmodel)
            implementation(libs.runtime.lifecycle)
            // ---- IO ------
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)
            implementation(libs.coil.compose)
            // ---- Project Module ------
            implementation(projects.shared)
            implementation(projects.uiTheme)
            implementation(projects.dataNetwork)
            implementation(libs.quickjs.kt)
        }
        commonTest.dependencies {
            implementation(projects.dataNetwork)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.quickjs.kt)
            implementation("org.drewcarlson:ktsoup-core:0.6.0")
            implementation("org.drewcarlson:ktsoup-fs:0.6.0")
            implementation("org.drewcarlson:ktsoup-ktor:0.6.0")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }

        val jvmMain by creating {
            dependencies {
                //implementation(fileTree(mapOf("dir" to "path/path", "include" to listOf("*.jar"))))
            }
        }
        desktopMain.dependsOn(jvmMain)
    }
}

android {
    namespace = "org.onion.diffusion"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.onion.diffusion"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = libs.versions.app.version.get()
        ndk {
            abiFilters.clear()
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = getenv("RELEASE_KEY_STORE_PASSWORD")
            keyAlias = "nova"
            keyPassword = getenv("RELEASE_KEY_STORE_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        if (getenv("RELEASE_KEY_EXISTS") == "true") {
            getByName("release") {
                isShrinkResources = true
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    android.applicationVariants.all {
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                this.outputFileName = "MineStableDiffusion-$versionName.apk"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        cmake {
            path = file("${rootProject.extra["dirCppMakeFile"]}")
            version = "3.22.1"
        }
    }
    lint {
        disable += "NullSafeMutableLiveData"
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.onion.diffusion.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.onion.diffusion"
            packageVersion = "1.0.0"

            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm
            )

            packageName = "MineStableDiffusion"
            packageVersion = libs.versions.app.version.get()
            vendor = "Onion"
            licenseFile.set(rootProject.rootDir.resolve("LICENSE"))


            linux {
                iconFile.set(rootProject.file("docs/AppIcon.png"))
                jvmArgs += listOf(
                    "-Xmx4g"
                )
            }
            windows {
                iconFile.set(rootProject.file("docs/AppIcon.ico"))
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
            }
            macOS {
                iconFile.set(rootProject.file("docs/AppIcon.icns"))
                bundleID = "org.onion.diffusion"
                appCategory = "public.app-category.productivity"
                jvmArgs += listOf(
                    "-Dapple.awt.application.name=MineStableDiffusion",
                    "-Dsun.java2d.metal=true",
                    "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
                    "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
                )
            }
        }

        jvmArgs += listOf(
            //"-XX:+UseZGC",
            "-XX:SoftMaxHeapSize=2048m",
            "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "-Xverify:none" // 错误 java.lang.VerifyError: Expecting a stackmap frame 表明 JVM 在加载类时，发现 Compose 编译器生成的字节码与 Java 21 严格的验证机制不兼容。 你需要禁用 JVM 的字节码验证，或者降级 Java 版本。最快的修复方法是在 build.gradle.kts 的 jvmArgs 中添加 -Xverify:none。
        )

        buildTypes.release.proguard {
            isEnabled = false
            version.set("7.7.0")
            configurationFiles.from("proguard-rules.pro")
        }
    }
}

// ------------------------------------------------------------------------
// 配置Run脚本
// ------------------------------------------------------------------------
afterEvaluate {
    val run = tasks.named("run")
    // 运行 桌面程序 Debug
    val desktopRunDebug by tasks.registering {
        dependsOn(run)
    }
}


// 在使用 Gradle 配置缓存（Configuration Cache）时，最核心的规则是：在 Execution 阶段（即 doLast 内部），绝对不能引用 Project 实例
// ExecOperations 作为构造函数参数注入 到一个自定义的 Task 类中，而不是在 doLast 里动态获取。这是 Gradle 官方推荐且唯一完全支持配置缓存的做法
// 定义一个自定义 Task，Gradle 会自动注入 execOps 和 fs (FileSystemOperations)
abstract class BuildNativeLibTask : DefaultTask() {
    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val fs: FileSystemOperations

    // 定义输入参数，Gradle 需要知道这些才能处理缓存
    @get:Input
    abstract val platformName: Property<String>

    @get:Input
    abstract val cmakeGenerator: Property<String>

    @get:Input
    abstract val cmakeOptions: ListProperty<String>

    @get:Internal // 标记为 Internal 因为这不是构建的输入/输出文件，而是工作目录
    abstract val targetWorkingDir: Property<File>

    @TaskAction
    fun execute() {
        val platform = platformName.get()
        println("正在为当前平台 $platform 构建原生库 (TaskAction)")

        val workDir = targetWorkingDir.get()
        val generator = cmakeGenerator.get()
        val options = cmakeOptions.get()

        // 1. Configure CMake
        execOps.exec {
            workingDir = workDir
            commandLine("cmake", "-S", ".", "-B", "build-$platform", "-G", generator)
            args(options)
            isIgnoreExitValue = false
        }

        // 2. Build CMake
        execOps.exec {
            workingDir = workDir
            commandLine("cmake", "--build", "build-$platform", "--config", "Release")
            isIgnoreExitValue = false
        }
    }
}
// 捕获 Configuration Phase 的变量，供 Execution Phase 使用，避免 configuration cache 问题
val rootDirVal = rootDir
val desktopPlatforms = listOf("windows", "macos", "linux")
desktopPlatforms.forEach { platform ->
    tasks.register<BuildNativeLibTask>("buildNativeLibFor${platform.capitalize()}") {
        println("配置 buildNativeLibFor${platform.capitalize()} 任务")

        // --- 配置阶段 (Configuration Phase) ---
        // 在这里赋值给 Task 的 Property，此时可以使用 project 上下文
        // 注意：cmake -S . 通常需要指向包含 CMakeLists.txt 的目录，而不是具体cpp文件。假设是上一级目录：
        this.targetWorkingDir.set(file("$rootDirVal/cpp/diffusion-loader.cpp"))
        this.platformName.set(platform)

        val currentPlatformName = platform // 捕获循环变量
        val generator = when(currentPlatformName) {
            "windows" -> {
                // GitHub Actions 和 CI 环境使用 Visual Studio
                // 本地开发可以通过环境变量 USE_MINGW=true 切换到 MinGW
                if (System.getenv("USE_MINGW") == "true") {
                    "MinGW Makefiles"
                } else {
                    "Visual Studio 17 2022"
                }
            }
            "macos" -> "Xcode"
            "linux" -> "Unix Makefiles"
            else -> "Unix Makefiles"
        }
        this.cmakeGenerator.set(generator)


        val options = when(platform) {
            "windows" -> {
                // 为 Visual Studio generator 明确指定 x64 架构
                if (System.getenv("USE_MINGW") != "true") {
                    listOf("-A", "x64")
                } else {
                    listOf()
                }
            }
            "macos" -> listOf("-DCMAKE_OSX_ARCHITECTURES=arm64;x86_64")
            else -> listOf()
        }
        // 注意: 使用 Visual Studio generator 时，不需要手动指定编译器路径
        // MinGW 本地开发备注:
        // 如需使用 MinGW，设置环境变量 USE_MINGW=true 并确保以下路径正确:
        // cmakeOptions.add("-DCMAKE_C_COMPILER=D:/MyApp/Code/mingw64/bin/x86_64-w64-mingw32-gcc.exe")
        // cmakeOptions.add("-DCMAKE_CXX_COMPILER=D:/MyApp/Code/mingw64/bin/x86_64-w64-mingw32-g++.exe")
        // cmakeOptions.add("-DCMAKE_MAKE_PROGRAM=C:/msys64/mingw64/bin/mingw32-make.exe")

        this.cmakeOptions.set(options)

        // 检查是否为当前平台，只有当前平台才执行 TaskAction
        // 注意：TaskAction 无法被动态跳过（除了 onlyIf），但我们可以用 onlyIf
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        val isCurrentPlatform = when(platform) {
            "windows" -> osName.contains("windows")
            "macos" -> osName.contains("mac")
            "linux" -> osName.contains("linux")
            else -> false
        }

        onlyIf { isCurrentPlatform }

        // 捕获需要的路径字符串，供 doLast 使用
        val cppLibsDirStr = cppLibsDirVal
        val jvmResourceLibDirStr = jvmResourceLibDirVal
        doLast {
            // 这里只能使用局部变量 cppLibsDirStr, jvmResourceLibDirStr
            // 绝对不能用 project.file 或 rootDirVal,也就是全局变量,也不能使用全局方法
            val srcDir = File(cppLibsDirStr)
            val destDir = File(jvmResourceLibDirStr)
            // 迁移到JVM资源目录
            if (!destDir.exists()) destDir.mkdirs()
            if (srcDir.exists() && srcDir.isDirectory) {
                srcDir.listFiles { _, name ->
                    name.endsWith(".dll") || name.endsWith(".dll.a") ||
                            name.endsWith(".so") || name.endsWith(".dylib")
                }?.forEach { f ->
                    f.copyTo(File(destDir, f.name), overwrite = true)
                }
                println("第一次SO迁移到JVM资源目录")
                println("cppLibsDirVal:$cppLibsDirStr")
                println("jvmResourceLibDirStr:$jvmResourceLibDirStr")
                println("${destDir.listFiles().map { it.name }}")
            }
        }
    }
}
val cppLibsDirVal = rootProject.extra["cppLibsDir"].toString()
val jvmResourceLibDirVal = rootProject.extra["jvmResourceLibDir"].toString()
tasks.register("buildNativeLibsIfNeeded") {
    println("JVM Architecture: ${System.getProperty("os.arch")}")
    println("Java Vendor: ${System.getProperty("java.vendor")}")
    println("Java Version: ${System.getProperty("java.version")}")
    println("Java VM Name: ${System.getProperty("java.vm.name")}")
    println("Sun Arch Data Model: ${System.getProperty("sun.arch.data.model")}")

    // Configuration time logic
    val currentOs = System.getProperty("os.name").lowercase(Locale.getDefault())
    val currentPlatform = when {
        currentOs.contains("windows") -> "Windows"
        currentOs.contains("mac") -> "Macos"
        currentOs.contains("linux") -> "Linux"
        else -> ""
    }

    val libName = when {
        currentOs.contains("windows") -> "libsdloader.dll"
        currentOs.contains("mac") -> "libsdloader.dylib"
        else -> "libsdloader.so"
    }

    val libFile = file("$cppLibsDirVal/$libName")

    // 如果库不存在，则添加构建任务依赖
    if (!libFile.exists() && currentPlatform.isNotEmpty()) {
         println("原生库不存在，配置构建任务依赖...")
         dependsOn("buildNativeLibFor$currentPlatform")
    } else {
         println("原生库已存在 (配置阶段检查)")
    }
    // 捕获需要的路径字符串，供 doLast 使用
    val cppLibsDirStr = cppLibsDirVal
    val jvmResourceLibDirStr = jvmResourceLibDirVal
    doLast {
        // 这里只能使用局部变量 cppLibsDirStr, jvmResourceLibDirStr
        // 绝对不能用 project.file 或 rootDirVal,也就是全局变量,也不能使用全局方法
        if(libFile.exists()) return@doLast
        val srcDir = File(cppLibsDirStr+File.separator+"Release")
        val destDir = File(jvmResourceLibDirStr)
        // 迁移到JVM资源目录
        if (!destDir.exists()) destDir.mkdirs()
        if (srcDir.exists() && srcDir.isDirectory) {
            srcDir.listFiles { _, name ->
                name.endsWith(".dll") || name.endsWith(".dll.a")
                        || name.endsWith(".so") || name.endsWith(".dylib")
            }?.forEach { f ->
                f.copyTo(File(destDir, if(f.name.startsWith("lib")) f.name else "lib${f.name}"), overwrite = true)
            }
            println("兜底第二次SO迁移到JVM资源目录")
            println("cppLibsDirVal:$cppLibsDirStr")
            println("jvmResourceLibDirStr:$jvmResourceLibDirStr")
            println("${destDir.listFiles().map { it.name }}")
        }
    }
}


tasks.matching { it.name.contains("packageReleaseDmg")
        || it.name.contains("createReleaseDistributable") }.configureEach {
    dependsOn("buildNativeLibsIfNeeded")
}

// 关键修复：确保 DLL 在资源处理之前就已复制到位
// desktopProcessResources 必须在 buildNativeLibsIfNeeded 之后运行
tasks.matching { it.name.contains("desktopProcessResources") }.configureEach {
    dependsOn("buildNativeLibsIfNeeded")
}