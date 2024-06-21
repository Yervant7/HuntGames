package com.yervant.huntgames.ui.bottomnav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun BottomNavGraph(
    navController: NavHostController,
    menus: List<BottomBarMenu>,
    startDestinationIndex: Int,
) {
    NavHost(
        navController = navController,
        startDestination = menus[startDestinationIndex].route
    ) {
        menus.forEach { menu ->
            composable(route = menu.route) {
                menu.content()
            }
        }
    }
}