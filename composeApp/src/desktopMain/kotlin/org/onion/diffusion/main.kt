package org.onion.diffusion

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1080.dp, height = 800.dp),
        title = "MineDiffusion",
    ) {
        App()
    }
}