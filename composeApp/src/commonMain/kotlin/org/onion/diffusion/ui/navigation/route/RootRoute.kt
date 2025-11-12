package org.onion.diffusion.ui.navigation.route

import androidx.navigation.NamedNavArgument
import mineread.composeapp.generated.resources.Res
import mineread.composeapp.generated.resources.ic_help
import mineread.composeapp.generated.resources.unknown
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource


sealed interface RootRoute {
    data object Splash : RoutePage(name = "Splash")
    data object MainRoute : RoutePage(name = "MainRoute")
}

open class RoutePage(
    val name: String,
    val iconRes: DrawableResource = Res.drawable.ic_help,
    val textRes: StringResource = Res.string.unknown,
    val navArguments: List<NamedNavArgument> = emptyList()
)