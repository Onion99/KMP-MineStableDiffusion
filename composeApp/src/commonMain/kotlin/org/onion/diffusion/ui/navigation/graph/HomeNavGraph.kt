package org.onion.diffusion.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import org.onion.diffusion.ui.navigation.NavActions
import org.onion.diffusion.ui.navigation.route.DetailRoute
import org.onion.diffusion.ui.navigation.route.MainRoute
import org.onion.diffusion.ui.screen.homeScreen

fun NavGraphBuilder.homeNavGraph(navActions: NavActions) {
    navigation( DetailRoute.Home.name,MainRoute.HomeRoute.name){
        homeScreen()
    }
}