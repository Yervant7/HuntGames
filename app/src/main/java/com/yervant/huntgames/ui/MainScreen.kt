package com.yervant.huntgames.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yervant.huntgames.R
import com.yervant.huntgames.ui.bottomnav.BottomBar
import com.yervant.huntgames.ui.bottomnav.BottomBarMenu
import com.yervant.huntgames.ui.bottomnav.BottomNavGraph
import com.yervant.huntgames.ui.menu.HomeMenu


@Composable
fun MainScreen(askForOverlayPermission: () -> Unit, openFilePicker: () -> Unit) {

    val navController = rememberNavController()
    // ============================ each menu in bottom nav ===================
    val menus = listOf(
        BottomBarMenu(
            route = "Home",
            title = "Home",
            iconId = R.drawable.ic_home,
            content = { HomeMenu(askForOverlayPermission = askForOverlayPermission, openFilePicker = openFilePicker) },
        ),
    )
    // =====================================================
    Scaffold(
        bottomBar = {
            BottomBar(
                navController = navController,
                menus = menus
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            BottomNavGraph(navController = navController, menus = menus, startDestinationIndex = 0)
        }
    }
}
