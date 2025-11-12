package org.onion.diffusion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.onion.diffusion.ui.navigation.route.DetailRoute
import org.onion.diffusion.ui.navigation.route.RootRoute


fun NavGraphBuilder.bookSourceScreen(){
    composable(DetailRoute.Home.name) {
        Box(modifier = Modifier.background(Color.Blue).fillMaxSize())
    }
}