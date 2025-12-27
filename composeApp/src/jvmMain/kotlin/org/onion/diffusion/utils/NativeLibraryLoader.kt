package org.onion.diffusion.utils

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object NativeLibraryLoader {

    private val loadedLibraries = mutableSetOf<String>()

    @Synchronized // Ensure thread safety
    fun loadFromResources(baseName: String) {
        if (baseName in loadedLibraries) {
            println("Native library '$baseName' already loaded.")
            return
        }

        val osName = System.getProperty("os.name").lowercase()
        val libFileName: String
        val resourcePath: String

        // Determine library file name and resource path based on OS
        // Assumes the library files are directly in "libs/" within resources
        when {
            osName.contains("win") -> {
                libFileName = "lib$baseName.dll"
                resourcePath = "/libs/$libFileName" // Path relative to resources root
            }
            osName.contains("mac") -> {
                libFileName = "lib$baseName.dylib"
                resourcePath = "/libs/$libFileName"
            }
            osName.contains("nix") || osName.contains("nux") -> {
                libFileName = "lib$baseName.so"
                resourcePath = "/libs/$libFileName"
            }
            else -> {
                throw UnsatisfiedLinkError("Unsupported OS: $osName for library '$baseName'")
            }
        }

        println("Attempting to load '$libFileName' from resources path: '$resourcePath'")

        val libFileStream: InputStream = NativeLibraryLoader::class.java.getResourceAsStream(resourcePath)
            ?: throw UnsatisfiedLinkError(
                "Native library '$libFileName' not found in resources at path '$resourcePath'. " +
                        "Ensure it's in 'src/jvmMain/resources/libs/'."
            )
        val libFileLibraryStream: InputStream? = NativeLibraryLoader::class.java.getResourceAsStream("$resourcePath.a")

        val tempLibFile: File
        val tempLibLibraryFile: File
        try {
            // Create a temporary directory to hold the library files
            val tempDir = java.nio.file.Files.createTempDirectory("native_libs_${baseName}_").toFile()
            tempDir.deleteOnExit() // Clean up directory on exit

            tempLibFile = File(tempDir, libFileName)
            tempLibFile.deleteOnExit() // Important for cleanup
            println("tempFile Name  ${tempLibFile.absolutePath}")
            FileOutputStream(tempLibFile).use { outputStream ->
                libFileStream.use { input ->
                    input.copyTo(outputStream)
                }
            }
            if(libFileLibraryStream != null){
                tempLibLibraryFile = File(tempDir, "$libFileName.a")
                tempLibLibraryFile.deleteOnExit()
                FileOutputStream(tempLibLibraryFile).use { outputStream ->
                    libFileLibraryStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }
            }
        } catch (e: Exception) {
            throw UnsatisfiedLinkError("Failed to create temporary file for library '$libFileName': ${e.message}").initCause(e) as UnsatisfiedLinkError
        } finally {
            try {
                libFileStream.close()
                libFileLibraryStream?.close()
            } catch (e: Exception) {
                // Log or ignore
            }
        }

        try {
            System.load(tempLibFile.absolutePath)
            loadedLibraries.add(baseName)
            println("Successfully loaded native library '$baseName' ('$libFileName') from temporary file: ${tempLibFile.absolutePath}")
        } catch (e: UnsatisfiedLinkError) {
            println("ERROR: Failed to load native library '$baseName' from ${tempLibFile.absolutePath}: ${e.message}")
            // Add more debug info if needed, e.g., if the DLL has other dependencies not found
            if (osName.contains("win") && e.message?.contains("Can't find dependent libraries") == true) {
                println("This error on Windows might indicate that '$libFileName' has other DLL dependencies that are not in the system PATH or alongside the loaded DLL.")
            }
            throw e // Re-throw the original error
        }
    }
}