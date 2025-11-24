plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Project Dir
val dirProject: Directory = layout.projectDirectory
// Cpp-目录,CMake路径,库输出目录
val dirCpp by extra(dirProject.dir("cpp"))
val dirCppMakeFile by extra("${dirCpp}/diffusion-loader.cpp/CMakeLists.txt")
val cppLibsDir by extra(dirCpp.dir("libs"))
// composeApp Dir
val dirApp by extra(dirProject.dir(projects.composeApp.name))
val dirAppSrc by extra(dirApp.dir("src"))
// composeApp-jvmMain Dir
val jvmDir by extra(dirAppSrc.dir("jvmMain"))
val jvmResourceLibDir by extra("${jvmDir}/resources/libs")