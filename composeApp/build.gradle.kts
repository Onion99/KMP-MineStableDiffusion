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
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm("desktop")
    
    /*@OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }*/
    
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

val desktopPlatforms = listOf("windows", "macos", "linux")
desktopPlatforms.forEach { platform ->
    tasks.register("buildNativeLibFor${platform.capitalize()}") {
        println("配置 buildNativeLibFor${platform.capitalize()} 任务")

        doFirst {
            println("开始构建 $platform 平台的原生库")
        }
        // ---- Mine Window System Environment Path------
        // Mingw64 D:\MyApp\Code\mingw64\bin
        // CMAKE C:\Program Files\CMake\bin
        // KDK -> D:\MyApp\Code\KDK\kotlinc\bin
        // JAVA -> HOME C:\Users\Administrator\.jdks\corretto-17.0.11
        doLast {
            val cmakeGenerator = when(platform) {
                "windows" -> "MinGW Makefiles" /*没装VisualStudio 不支持 "Visual Studio 17 2022"*/
                "macos" -> "Xcode"
                "linux" -> "Unix Makefiles"
                else -> "Unix Makefiles"
            }

            // 平台特定配置
            val cmakeOptions = when(platform) {
                // ---- 生成器 "MinGW Makefiles" 不支持"-A" 参数 ------
                //"windows" -> mutableListOf("-A", "x64")
                "macos" -> mutableListOf("-DCMAKE_OSX_ARCHITECTURES=arm64;x86_64")
                else -> mutableListOf()
            }
            // window 平台显式指定编译器 (如果它们不在 PATH 中，或者你想确保使用特定的编译器)
            // cmakeOptions.add("-DCMAKE_C_COMPILER=D:/MyApp/Code/mingw64/bin/x86_64-w64-mingw32-gcc.exe")
            // cmakeOptions.add("-DCMAKE_CXX_COMPILER=D:/MyApp/Code/mingw64/bin/x86_64-w64-mingw32-g++.exe")
            // 如果 mingw32-make 也不在 PATH 中，可能还需要指定
            // opts.add("-DCMAKE_MAKE_PROGRAM=C:/msys64/mingw64/bin/mingw32-make.exe")
            // 检查当前平台
            val isCurrentPlatform = when(platform) {
                "windows" -> System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")
                "macos" -> System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")
                "linux" -> System.getProperty("os.name").lowercase(Locale.getDefault()).contains("linux")
                else -> false
            }

            if (isCurrentPlatform) {
                println("正在为当前平台 $platform 构建原生库")
                // 这个目录可能需要调整，取决于 CMakeLists.txt 的位置
                val tagetWorkingDir = file("$rootDir/cpp/diffusion-loader.cpp")
                exec {
                    workingDir = tagetWorkingDir
                    val cmd = mutableListOf("cmake",  "-S", ".", "-B", "build-$platform", "-G", cmakeGenerator)
                    cmd.addAll(cmakeOptions)
                    commandLine(cmd)
                }

                exec {
                    workingDir = tagetWorkingDir
                    commandLine("cmake", "--build", "build-$platform", "--config", "Release")
                }

                // 迁移到JVM资源目录
                copy {
                    from("${rootProject.extra["cppLibsDir"]}")
                    include("*.dll","*.dll.a", "*.so", "*.dylib")
                    into("${rootProject.extra["jvmResourceLibDir"]}")
                }

                println("$platform 平台原生库构建完成")
            } else {
                println("跳过非当前平台 $platform 的构建")
            }
        }
    }
}

tasks.register("buildNativeLibsIfNeeded") {
    println("JVM Architecture: ${System.getProperty("os.arch")}") // amd64 means 64-bit, x86 means 32-bit
    println("Java Vendor: ${System.getProperty("java.vendor")}")
    println("Java Version: ${System.getProperty("java.version")}")
    println("Java VM Name: ${System.getProperty("java.vm.name")}")
    // For more detailed sun.arch.data.model (usually 32 or 64)
    println("Sun Arch Data Model: ${System.getProperty("sun.arch.data.model")}")
    doFirst {
        val libFile = when {
            System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows") -> file("${rootProject.extra["cppLibsDir"]}/libsmollm.dll")
            System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac") -> file("${rootProject.extra["cppLibsDir"]}/libsmollm.dylib")
            else -> file("${rootProject.extra["cppLibsDir"]}/libsmollm.so")
        }

        if (!libFile.exists()) {
            println("原生库不存在，开始构建...")
            // 触发当前平台的构建任务
            val currentPlatform = when {
                System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows") -> "Windows"
                System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac") -> "Macos"
                System.getProperty("os.name").lowercase(Locale.getDefault()).contains("linux") -> "Linux"
                else -> ""
            }
            if (currentPlatform.isNotEmpty()) {
                tasks.getByName("buildNativeLibFor$currentPlatform").actions.forEach { it.execute(this) }
            }
        } else {
            println("原生库已存在，跳过构建")
            val jvmLibFile = when {
                System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows") -> file("${rootProject.extra["jvmResourceLibDir"]}/libsmollm.dll")
                System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac") -> file("${rootProject.extra["jvmResourceLibDir"]}/libsmollm.dylib")
                else -> file("${rootProject.extra["jvmResourceLibDir"]}/libsmollm.so")
            }
            if (!jvmLibFile.exists()){
                println("原生库还没有迁移JVM资源目录,现在迁移")
                copy {
                    from("${rootProject.extra["cppLibsDir"]}")
                    include("*.dll","*.dll.a", "*.so", "*.dylib")
                    into("${rootProject.extra["jvmResourceLibDir"]}")
                }
            }
        }
    }
}

// 让desktopRun依赖这个CMake构建任务,来执行桌面JVM平台构建
tasks.matching { it.name.contains("desktopRun") }.configureEach {
    dependsOn("buildNativeLibsIfNeeded")
}
tasks.matching { it.name.contains("compileDesktopMainJava") }.configureEach {
    dependsOn("buildNativeLibsIfNeeded")
}
