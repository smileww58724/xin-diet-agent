package com.dietagent.android.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dietagent.android.DietAgentApp
import com.dietagent.android.MainActivity
import com.dietagent.android.ui.theme.Background
import com.dietagent.android.ui.theme.Ink
import com.dietagent.android.ui.theme.Line
import com.dietagent.android.ui.theme.Panel
import com.dietagent.android.ui.theme.PanelSoft
import com.dietagent.android.ui.theme.TextSecondary
import com.dietagent.android.ui.theme.TextTertiary
import kotlin.math.roundToInt

/** AI 对话页：气泡列表 + 输入栏 + 今日概览 */
@Composable
fun ChatScreen(app: DietAgentApp, modifier: Modifier = Modifier) {
    val vm: ChatViewModel = viewModel(factory = MainActivity.chatViewModelFactory(app))
    val messages by vm.messages.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val summary by vm.summary.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = modifier
            .background(Background)
            .imePadding()
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("AI 对话", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            IconButton(onClick = { vm.clear() }) {
                Text("清空", color = TextSecondary, fontSize = 13.sp)
            }
        }

        // 今日概览（窄屏卡片）
        summary?.let { s ->
            TodaySummaryCard(s)
        }

        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
            if (loading) {
                item {
                    Text(
                        "● 正在思考…",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(start = 44.dp, top = 8.dp),
                    )
                }
            }
        }

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        // 输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("说说你吃了什么，或问任何营养问题…", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ink,
                    unfocusedBorderColor = Line,
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (loading) vm.stop() else { vm.send(input); input = "" }
                }),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (loading) Color(0xFFD54242) else Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { if (loading) vm.stop() else { vm.send(input); input = "" } }) {
                    Icon(
                        imageVector = if (loading) Icons.Filled.Stop else Icons.Filled.Send,
                        contentDescription = if (loading) "停止" else "发送",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(s: com.dietagent.android.data.remote.dto.NutritionSummaryResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(PanelSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text("今日概览", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${s.totalCalories ?: 0}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(" / ${s.calorieGoal ?: 0} kcal", fontSize = 12.sp, color = TextTertiary)
        }
        ProgressBar(s.calorieProgress)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroLabel("蛋白质", s.proteinProgress)
            MacroLabel("脂肪", s.fatProgress)
            MacroLabel("碳水", s.carbProgress)
        }
    }
}

@Composable
private fun MacroLabel(label: String, progress: Double?) {
    Column {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text("${(progress ?: 0.0).roundToInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

@Composable
private fun ProgressBar(progress: Double?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color(0xFFE8E8EA), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((progress ?: 0.0).toFloat().coerceIn(0f, 1f) / 100f)
                .height(6.dp)
                .background(Ink, CircleShape)
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (isUser) Ink else Panel,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = if (msg.pending && msg.content.isEmpty()) "…" else msg.content,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}
