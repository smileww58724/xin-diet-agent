package com.dietagent.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dietagent.android.DietAgentApp
import com.dietagent.android.data.remote.dto.UsageStatsResponse
import com.dietagent.android.data.remote.dto.UserProfile
import com.dietagent.android.data.repo.AgentRepository
import com.dietagent.android.data.repo.GoalsRepository
import com.dietagent.android.ui.theme.Background
import com.dietagent.android.ui.theme.Ink
import com.dietagent.android.ui.theme.Panel
import com.dietagent.android.ui.theme.PanelSoft
import com.dietagent.android.ui.theme.TextSecondary
import com.dietagent.android.ui.theme.TextTertiary
import kotlinx.coroutines.launch

/** "我的"页：目标设定 + 个人资料 + AI 用量统计 + 退出 */
@Composable
fun ProfileScreen(app: DietAgentApp, modifier: Modifier = Modifier, navController: NavController? = null) {
    val goals: GoalsRepository = app.goalsRepository
    val agent: AgentRepository = app.agentRepository
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var usage by remember { mutableStateOf<UsageStatsResponse?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            runCatching { goals.getProfile() }.onSuccess { profile = it }
            runCatching { agent.getUsage() }.onSuccess { usage = it }
        }
    }

    Column(
        modifier = modifier
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("我的", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("目标 · 资料 · 用量", fontSize = 12.sp, color = TextTertiary)
            }
            IconButton(onClick = {
                scope.launch { app.authRepository.logout() }
                navController?.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }) {
                Icon(Icons.Filled.Logout, contentDescription = "退出登录", tint = TextSecondary)
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard("每日目标") {
            var cal by remember { mutableStateOf(profile?.dailyCalorieGoal?.toString() ?: "2000") }
            var pro by remember { mutableStateOf(profile?.proteinGoal?.toString() ?: "60") }
            var fat by remember { mutableStateOf(profile?.fatGoal?.toString() ?: "65") }
            var carb by remember { mutableStateOf(profile?.carbGoal?.toString() ?: "300") }
            // profile 加载后同步一次
            LaunchedEffect(profile?.dailyCalorieGoal) {
                profile?.let {
                    cal = (it.dailyCalorieGoal ?: 2000).toString()
                    pro = (it.proteinGoal ?: 60).toString()
                    fat = (it.fatGoal ?: 65).toString()
                    carb = (it.carbGoal ?: 300).toString()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("热量", cal) { cal = it }
                NumField("蛋白", pro) { pro = it }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("脂肪", fat) { fat = it }
                NumField("碳水", carb) { carb = it }
            }
            Spacer(Modifier.height(12.dp))
            BlackButton("保存目标") {
                scope.launch {
                    runCatching {
                        goals.updateProfile(
                            nickname = profile?.nickname,
                            gender = profile?.gender,
                            age = profile?.age,
                            height = profile?.height,
                            weight = profile?.weight,
                            activityLevel = profile?.activityLevel,
                            goalType = profile?.goalType,
                            dailyCalorieGoal = cal.toIntOrNull(),
                            proteinGoal = pro.toIntOrNull(),
                            fatGoal = fat.toIntOrNull(),
                            carbGoal = carb.toIntOrNull(),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard("个人资料") {
            val p = profile ?: UserProfile()
            var nickname by remember { mutableStateOf(p.nickname ?: "") }
            var age by remember { mutableStateOf(p.age?.toString() ?: "") }
            var height by remember { mutableStateOf(p.height?.toString() ?: "") }
            var weight by remember { mutableStateOf(p.weight?.toString() ?: "") }
            var gender by remember { mutableStateOf(p.gender ?: "") }
            var activity by remember { mutableStateOf(p.activityLevel ?: "") }
            var goalType by remember { mutableStateOf(p.goalType ?: "") }
            LaunchedEffect(profile) {
                profile?.let {
                    nickname = it.nickname ?: ""
                    age = it.age?.toString() ?: ""
                    height = it.height?.toString() ?: ""
                    weight = it.weight?.toString() ?: ""
                    gender = it.gender ?: ""
                    activity = it.activityLevel ?: ""
                    goalType = it.goalType ?: ""
                }
            }
            OutlinedTextField(nickname, { nickname = it }, label = { Text("昵称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("年龄", age) { age = it }
                NumField("身高cm", height) { height = it }
                NumField("体重kg", weight) { weight = it }
            }
            Spacer(Modifier.height(12.dp))
            BlackButton("保存资料") {
                scope.launch {
                    runCatching {
                        goals.updateProfile(
                            nickname = nickname.ifBlank { null },
                            gender = gender.ifBlank { null },
                            age = age.toIntOrNull(),
                            height = height.toDoubleOrNull(),
                            weight = weight.toDoubleOrNull(),
                            activityLevel = activity.ifBlank { null },
                            goalType = goalType.ifBlank { null },
                            dailyCalorieGoal = profile?.dailyCalorieGoal,
                            proteinGoal = profile?.proteinGoal,
                            fatGoal = profile?.fatGoal,
                            carbGoal = profile?.carbGoal,
                        )
                    }.onSuccess { saved ->
                        profile = saved
                        app.tokenStore.saveNickname(saved.nickname ?: "")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard("AI 用量统计") {
            val u = usage
            if (u == null || (u.totalCalls ?: 0) == 0L) {
                Text("还没有 AI 对话，去聊两句试试～", fontSize = 13.sp, color = TextTertiary)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${u.totalCalls}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(" 次对话", fontSize = 12.sp, color = TextTertiary)
                }
                Spacer(Modifier.height(8.dp))
                UsageLine("总 tokens", "${u.totalTokens ?: 0}")
                UsageLine("提示 / 生成", "${u.totalPromptTokens ?: 0} / ${u.totalCompletionTokens ?: 0}")
                if (!u.daily.isNullOrEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("近 7 天", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    u.daily.forEach { d ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(d.date, fontSize = 12.sp, color = TextSecondary)
                            Text("${d.calls} 次 · ${d.tokens} tokens", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun BlackButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Ink),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(text) }
}

@Composable
private fun UsageLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}
