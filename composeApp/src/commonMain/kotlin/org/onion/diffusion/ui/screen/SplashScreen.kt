package org.onion.diffusion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import org.onion.diffusion.ui.navigation.route.RootRoute


fun NavGraphBuilder.splashScreen(autoToMainPage: () -> Unit){
    composable(RootRoute.Splash.name) {
        LaunchedEffect(true) {
            delay(1000)
            autoToMainPage()
        }
        Box(modifier = Modifier.background(Color.Red).fillMaxSize())
    }
}