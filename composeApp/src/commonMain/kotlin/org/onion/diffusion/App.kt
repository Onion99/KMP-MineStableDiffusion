package org.onion.diffusion

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.setSingletonImageLoaderFactory
import com.onion.network.di.networkModule
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.core.KoinApplication
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.onion.diffusion.di.viewModelModule
import org.onion.diffusion.ui.navigation.NavActions
import org.onion.diffusion.ui.navigation.route.MainRoute
import org.onion.diffusion.ui.navigation.route.RootRoute
import org.onion.diffusion.ui.screen.homeScreen
import org.onion.diffusion.ui.screen.mainScreen
import org.onion.diffusion.ui.screen.splashScreen
import org.onion.diffusion.utils.imageLoaderDiskCache
import ui.theme.AppTheme

@Composable
@Preview
fun App() {
    KoinApplication({
        modules(viewModelModule, networkModule)
    }){
        // Initialize the Coil3 image loader
        setSingletonImageLoaderFactory { context ->
            imageLoaderDiskCache(context)
        }
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
                //splashScreen(autoToMainPage = { rootNavActions.popAndNavigation(RootRoute.MainRoute) })
                splashScreen(autoToMainPage = { rootNavActions.popAndNavigation(MainRoute.HomeRoute) })
                // ---- 首页架构容器 ------
                // mainScreen()
                // ---- 首页架构容器,暂时不做多tab首页,先搭建起来再说 ------
                homeScreen()
            }
        }
    }
}