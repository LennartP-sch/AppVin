package com.appvin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appvin.ui.AddWineScreen
import com.appvin.ui.CellarViewModel
import com.appvin.ui.ChatScreen
import com.appvin.ui.CollectionScreen
import com.appvin.ui.EditWineScreen
import com.appvin.ui.HomeScreen
import com.appvin.ui.LocationsScreen
import com.appvin.ui.MoreScreen
import com.appvin.ui.PastBottlesScreen
import com.appvin.ui.ScanState
import com.appvin.ui.WineDetailScreen
import com.appvin.ui.theme.AppVinTheme

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("cellar", "Cellar", Icons.Filled.WineBar),
    Tab("chat", "Sommelier", Icons.AutoMirrored.Filled.Chat),
    Tab("more", "More", Icons.Filled.Menu),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppVinTheme { AppNavigation() } }
    }
}

@Composable
private fun AppNavigation() {
    val vm: CellarViewModel = viewModel()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val openWine = { id: Long -> nav.navigate("wine/$id") }
        val addWine = {
            if (vm.scan.value !is ScanState.Working) vm.resetScan()
            nav.navigate("add")
        }
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable("home") {
                HomeScreen(vm, onOpenWine = openWine, onAdd = addWine, onAsk = { nav.navigate("chat") })
            }
            composable("cellar") { CollectionScreen(vm, onOpenWine = openWine, onAdd = addWine) }
            composable("chat") { ChatScreen(vm) }
            composable("more") {
                MoreScreen(vm, onLocations = { nav.navigate("locations") }, onPast = { nav.navigate("past") })
            }
            composable("add") {
                AddWineScreen(vm, onBack = { nav.popBackStack() }, onSaved = { id ->
                    nav.navigate("wine/$id") { popUpTo("add") { inclusive = true } }
                })
            }
            composable("wine/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments!!.getLong("id")
                WineDetailScreen(
                    vm, id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("edit/$id") },
                    onAsk = { question ->
                        vm.sendChat(question)
                        nav.navigate("chat")
                    },
                )
            }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                EditWineScreen(vm, entry.arguments!!.getLong("id"), onDone = { nav.popBackStack() })
            }
            composable("locations") { LocationsScreen(vm, onBack = { nav.popBackStack() }) }
            composable("past") { PastBottlesScreen(vm, onBack = { nav.popBackStack() }, onOpenWine = openWine) }
        }
    }
}
