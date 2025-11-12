package org.onion.diffusion.ui.navigation.route

import mineread.composeapp.generated.resources.Res
import mineread.composeapp.generated.resources.home
import mineread.composeapp.generated.resources.ic_home


sealed interface DetailRoute {
    data object Home : RoutePage(name = "Home",Res.drawable.ic_home, Res.string.home)
    data object Setting : RoutePage(name = "Setting",Res.drawable.ic_home, Res.string.home)
    data object Mine : RoutePage(name = "Mine",Res.drawable.ic_home, Res.string.home)
    data object BookSource : RoutePage(name = "BookSource",Res.drawable.ic_home, Res.string.home)
}