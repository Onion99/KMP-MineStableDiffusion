package org.onion.diffusion

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview

import org.onion.diffusion.ui.navigation.NavActions
import org.onion.diffusion.ui.navigation.route.RootRoute
import org.onion.diffusion.ui.screen.mainScreen
import org.onion.diffusion.ui.screen.splashScreen
import ui.theme.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        val rootNavController = rememberNavController()
        val rootNavActions = remember(rootNavController) {
            NavActions(rootNavController)
        }
        NavHost(
            navController = rootNavController,
            startDestination = RootRoute.Splash.name
        ) {
            // ---- 开屏页,CPM启动会有一阵子白屏,还是依据具体平台定制化比较合理 ------
            splashScreen(autoToMainPage = { rootNavActions.popAndNavigation(RootRoute.MainRoute) })
            // ---- 首页架构容器 ------
            mainScreen()
        }
    }
}