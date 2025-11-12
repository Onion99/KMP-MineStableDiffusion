package org.onion.diffusion.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onion.theme.helper.verticalSafePadding
import com.onion.theme.state.AdaptiveLayoutType
import kotlinx.coroutines.delay
import mineread.composeapp.generated.resources.Res
import mineread.composeapp.generated.resources.dark_theme
import mineread.composeapp.generated.resources.ic_moon
import mineread.composeapp.generated.resources.ic_sun
import mineread.composeapp.generated.resources.light_theme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.onion.diffusion.ui.navigation.NavActions
import org.onion.diffusion.ui.navigation.graph.homeNavGraph
import org.onion.diffusion.ui.navigation.route.MainRoute
import org.onion.diffusion.ui.navigation.route.NAV_BOTTOM_ITEMS
import org.onion.diffusion.ui.navigation.route.RootRoute
import ui.theme.AppTheme


fun NavGraphBuilder.mainScreen(){
    composable(RootRoute.MainRoute.name) {
        val mainFunNavController = rememberNavController()
        val mainFunNavActions = remember(mainFunNavController) {
            NavActions(mainFunNavController)
        }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        // ---- 当前路由所在的位置,堆栈层应该是 -> Splash,Main,Home,当前位置 ------
        val currentRoute = mainFunNavController.currentBackStack.value.getOrNull(3)?.destination?.route
        val selectedTabRoute = NAV_BOTTOM_ITEMS.forEach { routePage ->
            routePage.name == currentRoute
        }
        ModalNavigationDrawer(
            drawerContent = {
                ModalNavigationContent()
            },
            drawerState = drawerState,
            gesturesEnabled = false
        ){
            MainContent(mainFunNavController,mainFunNavActions)
        }
    }
}

val navigationDrawerMinWidth = 240.dp
val navigationDrawerMaxWidth = 360.dp

// ------------------------------------------------------------------------
// 抽屉式导航栏
// ------------------------------------------------------------------------
@Composable
fun ModalNavigationContent(){
    Column(
        modifier = Modifier.fillMaxHeight().padding(AppTheme.spacing.s200)
            .clip(AppTheme.shape.r500)
            .widthIn(min = navigationDrawerMinWidth, max = navigationDrawerMaxWidth)
            .border(AppTheme.size.borderWidth, AppTheme.colors.border, AppTheme.shape.r500)
            .background(AppTheme.colors.surfaceContainer)
            .padding(horizontal = AppTheme.spacing.s400, vertical = AppTheme.spacing.s450)
    ){

    }
}

// ------------------------------------------------------------------------
// 主导航栏
// 1. route = MainFlow.Home.route•这是什么？ 这是整个导航子图（文件夹）的入口路由。•
// 作用是什么？ 当你想从应用的其他地方跳转到“主页”这个功能模块时，你必须导航到这个 route。
// 例如，如果你的应用有一个登录流程，登录成功后，你会调用 navController.navigate(MainFlow.Home.route)。
// 关键点： 你导航到的是整个子图的 route，而不是子图内部某个具体屏幕的 route。
// 导航组件接收到这个 route 后，就知道要进入这个 navigation 代码块定义的范围。
// 2. startDestination = MainFlow.Home.startScreen.route•这是什么？ 这是这个导航子图的起始目标（默认屏幕）。
// 作用是什么？ 一旦通过 MainFlow.Home.route 进入了这个导航子图，导航组件会立即、自动地为你显示 startDestination 所指向的那个屏幕。
// ------------------------------------------------------------------------
@Composable
fun MainContent(mainFunNavController: NavHostController, mainNavActions: NavActions){
    Column(Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            AnimatedVisibility(
                visible = AppTheme.adaptiveLayoutType == AdaptiveLayoutType.Medium || AppTheme.adaptiveLayoutType == AdaptiveLayoutType.Expanded
            ) {
                SlideNavigationBar(
                    modifier = Modifier.fillMaxHeight().padding(start = AppTheme.spacing.s300)
                        .padding(verticalSafePadding()),
                    onThemeChanged = {}
                )
            }
            NavHost(
                modifier = Modifier.widthIn(max = AppTheme.size.maxContainerWidth),
                navController = mainFunNavController,
                startDestination = MainRoute.HomeRoute.name
            ){
                // ------------------------------------------------------------------------
                // navigation(...) 函数用于创建嵌套的导航图（Nested Navigation Graph），或者可以理解为“导航子图”。
                // 想象一下你的应用导航结构像一个文件系统：•NavHost 是根目录 C:/。•composable("screen_route") 是根目录下的一个文件，比如 C:/readme.txt。•navigation(...) 则是在根目录下创建的一个文件夹，比如 C:/Users/。这个文件夹本身有自己的路径（route），并且当你进入这个文件夹时，它会自动打开一个默认的文件（startDestination）
                // ------------------------------------------------------------------------
                homeNavGraph(mainNavActions)
            }
        }
        AnimatedVisibility(visible = AppTheme.adaptiveLayoutType == AdaptiveLayoutType.Compact) {
            BottomNavigationBar(){}
        }
    }
}
@Composable
fun SlideNavigationBar(modifier: Modifier,onThemeChanged: () -> Unit){
    Column(
        modifier

            .border(AppTheme.size.borderWidth, AppTheme.colors.border, CircleShape)
            .background(AppTheme.colors.surfaceContainer, CircleShape).widthIn(min = 80.dp)
            .padding(vertical = AppTheme.spacing.s400).selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s200)
    ) {
        Spacer(Modifier.height(AppTheme.spacing.s300))
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s200)
        ) {
            NAV_BOTTOM_ITEMS.forEach { routePage ->
                NavigationRailItem(selected = false, onClick = {
                    }, icon = {
                        Icon(
                            imageVector = vectorResource(routePage.iconRes),
                            contentDescription = stringResource(routePage.textRes)
                        )
                    }, colors = navigationRailItemColors()
                )
            }

            Spacer(modifier = Modifier.weight(1f).heightIn(min = AppTheme.spacing.s400))
            val isDark by AppTheme.isDark
            NavigationRailItem(selected = false, onClick = {
                onThemeChanged()
            }, icon = {
                Icon(
                    imageVector = vectorResource(if (isDark) Res.drawable.ic_sun else Res.drawable.ic_moon),
                    contentDescription = stringResource(if (isDark) Res.string.light_theme else Res.string.dark_theme),
                    tint = AppTheme.colors.onSurfaceVariant
                )
            })
        }
    }
}
@Composable
fun BottomNavigationBar(onThemeChanged: () -> Unit){
    NavigationBar(modifier = Modifier.fillMaxWidth(), containerColor = AppTheme.colors.surface) {
        NAV_BOTTOM_ITEMS.forEach { routePage ->
            val isSelected = true
            NavigationBarItem(
                selected = false, onClick = {
                }, icon = {
                    Icon(
                        imageVector = vectorResource(routePage.iconRes),
                        contentDescription = stringResource(routePage.textRes)
                    )
                }, label = {
                    Text(
                        text = stringResource(routePage.textRes),
                        style = if (isSelected) AppTheme.typography.labelMedium else AppTheme.typography.labelSmall
                    )
                }, colors = navigationBarItemColors()
            )
        }
    }
}

@Composable
private fun navigationRailItemColors() = NavigationRailItemDefaults.colors(
    selectedIconColor = AppTheme.colors.onPrimaryContainer,
    selectedTextColor = AppTheme.colors.onSurfaceContainer,
    indicatorColor = AppTheme.colors.primaryContainer,
    unselectedIconColor = AppTheme.colors.onSurfaceVariant,
    unselectedTextColor = AppTheme.colors.onSurfaceVariant
)

@Composable
private fun navigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AppTheme.colors.onPrimaryContainer,
    selectedTextColor = AppTheme.colors.onSurface,
    indicatorColor = AppTheme.colors.primaryContainer,
    unselectedIconColor = AppTheme.colors.onSurfaceVariant,
    unselectedTextColor = AppTheme.colors.onSurfaceVariant
)