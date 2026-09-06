package com.dietagent.android.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.dietagent.android.DietAgentApp
import com.dietagent.android.ui.analysis.AnalysisScreen
import com.dietagent.android.ui.chat.ChatScreen
import com.dietagent.android.ui.diet.DietScreen
import com.dietagent.android.ui.favorites.FavoritesScreen
import com.dietagent.android.ui.profile.ProfileScreen

private data class TabItem(
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabItem("对话", Icons.Filled.ChatBubble),
    TabItem("记录", Icons.Filled.FoodBank),
    TabItem("分析", Icons.Filled.Insights),
    TabItem("偏好", Icons.Filled.Favorite),
    TabItem("我的", Icons.Filled.Person),
)

/** 主界面：底部导航 5 tab（对话/记录/分析/偏好/我的） */
@Composable
fun HomeScreen(app: DietAgentApp, navController: NavController) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (selected) {
            0 -> ChatScreen(app, contentModifier)
            1 -> DietScreen(app, contentModifier)
            2 -> AnalysisScreen(app, contentModifier)
            3 -> FavoritesScreen(app, contentModifier)
            else -> ProfileScreen(app, contentModifier, navController)
        }
    }
}
