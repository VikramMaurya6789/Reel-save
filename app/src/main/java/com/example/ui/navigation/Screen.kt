package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Downloads : Screen("downloads")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Preview : Screen("preview?url={url}") {
        fun createRoute(url: String): String {
            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
            return "preview?url=$encoded"
        }
    }
}
