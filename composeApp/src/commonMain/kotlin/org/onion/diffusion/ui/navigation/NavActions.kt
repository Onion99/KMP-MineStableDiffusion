/*
 * Copyright 2024 The ZZZ Archive Open Source Project by mrfatworm
 * License: MIT License
 */

package org.onion.diffusion.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import org.onion.diffusion.ui.navigation.route.RoutePage

class NavActions(private val navController: NavHostController) {

    fun navigationTo(destination: RoutePage) {
        navController.navigate(destination.name)
    }

    fun navigationToRoute(route: String) {
        navController.navigate(route)
    }

    fun back() {
        navController.popBackStack()
    }

    // ------------------------------------------------------------------------
    // 退出当前和进入新页面
    // ------------------------------------------------------------------------
    fun popAndNavigation(destination: RoutePage) {
        navController.navigate(destination.name) {
            popUpTo(navController.graph.findStartDestination().route!!) {
                this.inclusive = true
            }
        }
    }
}