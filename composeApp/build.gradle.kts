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

// 捕获 Configuration Phase 的变量，供 Execution Phase 使用，避免 configuration cache 问题
val cppLibsDirVal = rootProject.extra["cppLibsDir"].toString()
val jvmResourceLibDirVal = rootProject.extra["jvmResourceLibDir"].toString()
val rootDirVal = rootDir

val desktopPlatforms = listOf("windows", "macos", "linux")
desktopPlatforms.forEach { platform ->
    tasks.register("buildNativeLibFor${platform.capitalize()}") {
        println("配置 buildNativeLibFor${platform.capitalize()} 任务")

        doFirst {
            println("开始构建 $platform 平台的原生库")
        }

        // 捕获任务需要的配置
        val targetWorkingDir = file("$rootDirVal/cpp/diffusion-loader.cpp")

        doLast {
            val cmakeGenerator = when(platform) {
                "windows" -> "MinGW Makefiles"
                "linux" -> "Unix Makefiles"
                else -> "Unix Makefiles"
            }

            // 平台特定配置
            val cmakeOptions = when(platform) {
                "windows" -> mutableListOf(
                    "-DCMAKE_CXX_USE_RESPONSE_FILE_FOR_OBJECTS=1",
                    "-DCMAKE_CXX_USE_RESPONSE_FILE_FOR_LIBRARIES=1",
                    "-DCMAKE_CXX_RESPONSE_FILE_LINK_FLAG=\"@\"",
                    "-DCMAKE_NINJA_FORCE_RESPONSE_FILE=1"
                )
                "macos" -> mutableListOf("-DCMAKE_OSX_ARCHITECTURES=arm64;x86_64")
                else -> mutableListOf()
            }

            // 检查当前平台
            val isCurrentPlatform = when(platform) {
                "windows" -> System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")
                "macos" -> System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")
                "linux" -> System.getProperty("os.name").lowercase(Locale.getDefault()).contains("linux")
                else -> false
            }

            if (isCurrentPlatform) {
                println("正在为当前平台 $platform 构建原生库")

                exec {
                    workingDir = targetWorkingDir
                    val cmd = mutableListOf("cmake",  "-S", ".", "-B", "build-$platform", "-G", cmakeGenerator)
                    cmd.addAll(cmakeOptions)
                    commandLine(cmd)
                }

                exec {
                    workingDir = targetWorkingDir
                    commandLine("cmake", "--build", "build-$platform", "--config", "Release")
                }

                // 迁移到JVM资源目录
                copy {
                    from(cppLibsDirVal)
                    include("*.dll","*.dll.a", "*.so", "*.dylib")
                    into(jvmResourceLibDirVal)
                }

                println("$platform 平台原生库构建完成")
            } else {
                println("跳过非当前平台 $platform 的构建")
            }
        }
    }
}

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
    val jvmLibFile = file("$jvmResourceLibDirVal/$libName")

    // 如果库不存在，则添加构建任务依赖
    if (!libFile.exists() && currentPlatform.isNotEmpty()) {
         println("原生库不存在，配置构建任务依赖...")
         dependsOn("buildNativeLibFor$currentPlatform")
    } else {
         println("原生库已存在 (配置阶段检查)")
    }

    doLast {
        // 如果我们没有运行构建任务（因为 libFile 存在），我们仍需确保它被复制到 jvmResourceLibDir
        if (libFile.exists() && !jvmLibFile.exists()) {
            println("原生库还没有迁移JVM资源目录,现在迁移")
            // Manually copy to avoid capturing 'project' in doLast which breaks configuration cache
            val srcDir = File(cppLibsDirVal)
            val destDir = File(jvmResourceLibDirVal)
            
            if (!destDir.exists()) destDir.mkdirs()

            if (srcDir.exists() && srcDir.isDirectory) {
                srcDir.listFiles { _, name -> 
                    name.endsWith(".dll") || name.endsWith(".dll.a")
                            || name.endsWith(".so") || name.endsWith(".dylib")
                }?.forEach { f ->
                    val target = File(destDir, f.name)
                    // Simple file copy
                    f.copyTo(target, overwrite = true)
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