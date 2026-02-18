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


val headersDir = project.file("${rootProject.projectDir}/cpp/stable-diffusion.cpp")
val nativeDefFile = project.file("src/nativeInterop/cinterop/sdloader.def")
kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    // app icon iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png,https://convertany.net/zh-cn/app-icon-converter
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
                "-framework", "Accelerate",
                //"-lzip"
            )
            linkTaskProvider.configure { dependsOn("buildIosNativeLibs") }
        }
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            // isStatic = true
            // 优点:启动速度可能稍快，因为不需要在运行时加载动态库。
            // 缺点:如果多个扩展（如 WidgetKit）都使用了这个静态框架，每个扩展都会有一份代码拷贝，导致应用总体积增大。 可能会遇到符号冲突（duplicate symbols）问题，特别是当框架内部依赖的库与主应用或其他框架依赖的库版本不同时。
            // isStatic = false？
            // 正确链接原生依赖（C/C++ 库）: 当你构建一个动态框架时，linkerOpts 中指定的 -l... 和 -framework... 选项会记录为框架的运行时依赖。当你的应用启动并加载这个动态框架时，iOS 的动态链接器（dyld）会根据这些记录去寻找并加载所需的系统框架（如 Metal, Accelerate）和你提供的 C++ 库。如果 isStatic = true，链接器会尝试在构建静态库时就将所有原生库的代码合并进来，这非常复杂且极易出错，常常导致链接失败或运行时找不到符号（undefined symbols）。
            // 避免重复符号错误 (Duplicate Symbols): 如果你的 C++ 库或者它依赖的任何系统库，与你的主 App Target 或其他依赖的框架有重叠，使用静态链接会把这些符号复制多份到最终的可执行文件中，从而导致“duplicate symbols”链接错误。而动态框架则可以确保这些共享库只被加载一次。
            // 模块化和代码隔离: 使用动态框架可以将你的 Kotlin 和 C++ 封装成一个独立的模块。这使得依赖管理更加清晰。主应用只需要知道它依赖于 ComposeApp.framework，而不需要关心其内部复杂的原生链接细节。
            isStatic = false
        }
        iosTarget.compilations["main"].cinterops {
            val sdloader by creating {
                defFile(nativeDefFile)
                //compilerOpts("-I${headersDir.absolutePath}")
                includeDirs(headersDir)
                //extraOpts("-verbose")
            }
        }
    }
    
    // Ensure cinterop depends on native build to avoid race conditions and ensure headers/libs are ready
    /*tasks.withType<org.jetbrains.kotlin.gradle.tasks.CInteropProcess>().configureEach {
        if (name.contains("SdloaderIos")) {
            dependsOn("buildIosNativeLibs")
        }
    }*/
    
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
            
            // Fix: com/sun/security/auth/module/UnixSystem error on Linux
            // This module is required for user/group checking but often stripped by jlink
            modules("jdk.security.auth")


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

// ------------------------------------------------------------------------
// iOS Native Build Task
// ------------------------------------------------------------------------
tasks.register<Exec>("buildIosNativeLibs") {
    val script = rootProject.file("cpp/build_ios.sh")
    workingDir = rootProject.file("cpp")
    commandLine("bash", "./build_ios.sh")

    // Only run on macOS
    onlyIf {
        System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")
    }

    // Declare inputs and outputs for up-to-date checks
    val inputDir = rootProject.file("cpp/stable-diffusion.cpp")
    inputs.files(fileTree(inputDir) {
        exclude("**/.git/**")
        exclude("**/build/**")
    })
    inputs.file(script)
    
    val outputDeviceDir = rootProject.file("cpp/libs/ios-device")
    val outputSimDir = rootProject.file("cpp/libs/ios-simulator")
    outputs.dir(outputDeviceDir)
    outputs.dir(outputSimDir)

    // Custom up-to-date check to be safe
    outputs.upToDateWhen {
        outputDeviceDir.exists() && outputDeviceDir.listFiles()?.isNotEmpty() == true &&
        outputSimDir.exists() && outputSimDir.listFiles()?.isNotEmpty() == true
    }
}


@CacheableTask
abstract class BuildIpaTask : DefaultTask() {
    /* -------------------------------------------------------------
     * Inputs / outputs
     * ----------------------------------------------------------- */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val archiveDir: DirectoryProperty

    @get:OutputFile
    abstract val outputIpa: RegularFileProperty

    /* -------------------------------------------------------------
     * Services (injected)
     * ----------------------------------------------------------- */

    @get:Inject
    abstract val execOperations: ExecOperations

    /* -------------------------------------------------------------
     * Action
     * ----------------------------------------------------------- */

    @TaskAction
    fun buildIpa() {
        // 1. Locate the .app inside the .xcarchive
        val appDir = archiveDir.get().asFile.resolve("Products/Applications/MineStableDiffusion.app")
        if (!appDir.exists())
            throw GradleException("Could not find MineStableDiffusion.app in archive at: ${appDir.absolutePath}")

        // 2. Create temporary Payload directory and copy .app into it
        val payloadDir = File(temporaryDir, "Payload").apply { mkdirs() }
        val destApp = File(payloadDir, appDir.name)
        appDir.copyRecursively(destApp, overwrite = true)

        // 3. Inject placeholder (ad‑hoc) code signature so AltStore / SideStore accept it
        logger.lifecycle("[IPA] Ad‑hoc signing ${destApp.name} …")
        execOperations.exec {
            commandLine(
                "codesign", "--force", "--deep", "--sign", "-", "--timestamp=none",
                destApp.absolutePath,
            )
        }

        // 4. Zip Payload ⇒ .ipa using the system `zip` command
        //
        //    -r : recurse into directories
        //    -y : store symbolic links as the link instead of the referenced file
        //
        // The working directory is the temporary folder so the archive
        // has a top‑level "Payload/" directory (required for .ipa files).
        val zipFile = File(temporaryDir, "MineStableDiffusion.zip")
        execOperations.exec {
            workingDir(temporaryDir)
            commandLine("zip", "-r", "-y", zipFile.absolutePath, "Payload")
        }

        // 5. Move to final location (with .ipa extension)
        outputIpa.get().asFile.apply {
            parentFile.mkdirs()
            delete()
            zipFile.renameTo(this)
        }

        logger.lifecycle("[IPA] Created ad‑hoc‑signed IPA at: ${outputIpa.get().asFile.absolutePath}")
    }
}

fun ipaArguments(
    destination: String = "generic/platform=iOS",
    sdk: String = "iphoneos",
): Array<String> {
    return arrayOf(
        "xcodebuild",
        "-project", rootDir.resolve("iosApp/iosApp.xcodeproj").absolutePath,
        "-scheme", "iosApp",
        "-destination", destination,
        "-sdk", sdk,
        "CODE_SIGNING_ALLOWED=NO",
        "CODE_SIGNING_REQUIRED=NO",
    )
}
val buildReleaseArchive = tasks.register("buildReleaseArchive", Exec::class) {
    group = "build"
    description = "Builds the iOS framework for Release"
    workingDir(projectDir)

    val output = layout.buildDirectory.dir("archives/release/MineStableDiffusion.xcarchive")
    outputs.dir(output)
    commandLine(
        *ipaArguments(),
        "archive",
        "-configuration", "Release",
        "-archivePath", output.get().asFile.absolutePath,
    )
}
tasks.register("buildReleaseIpa", BuildIpaTask::class) {
    description = "Manually packages the .app from the .xcarchive into an unsigned .ipa"
    group = "build"

    // Adjust these paths as needed
    archiveDir = layout.buildDirectory.dir("archives/release/MineStableDiffusion.xcarchive")
    outputIpa = layout.buildDirectory.file("archives/release/MineStableDiffusion-${libs.versions.app.version.get()}.ipa")
    dependsOn(buildReleaseArchive)
}

// Ensure strict version resolution for kotlinx-datetime
configurations.all {
    resolutionStrategy {
        force(libs.kotlinx.datetime)
    }
}
