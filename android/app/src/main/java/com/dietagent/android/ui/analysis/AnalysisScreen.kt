package com.dietagent.android.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.dietagent.android.DietAgentApp
import com.dietagent.android.data.remote.dto.NutritionSummaryResponse
import com.dietagent.android.data.repo.AnalysisRepository
import com.dietagent.android.ui.theme.Background
import com.dietagent.android.ui.theme.Ink
import com.dietagent.android.ui.theme.Panel
import com.dietagent.android.ui.theme.PanelSoft
import com.dietagent.android.ui.theme.TextSecondary
import com.dietagent.android.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 营养分析页：日报/周报切换 + 四张统计卡 */
@Composable
fun AnalysisScreen(app: DietAgentApp, modifier: Modifier = Modifier) {
    val repo: AnalysisRepository = app.analysisRepository
    val scope = rememberCoroutineScope()

    var isWeekly by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<NutritionSummaryResponse?>(null) }

    LaunchedEffect(isWeekly) {
        scope.launch {
            runCatching { if (isWeekly) repo.getWeekly() else repo.getDaily() }
                .onSuccess { summary = it }
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
                Text("营养分析", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("基于你的真实饮食记录与目标", fontSize = 12.sp, color = TextTertiary)
            }
            SegmentedControl(isWeekly, onToggle = { isWeekly = it })
        }

        Spacer(Modifier.height(16.dp))

        val s = summary
        if (s == null) {
            Text("加载中…", color = TextTertiary)
        } else {
            StatCard("热量", "${s.totalCalories ?: 0}", "/ ${s.calorieGoal ?: 0} kcal", s.calorieProgress)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("蛋白质", fmt(s.totalProtein), "/ ${s.proteinGoal ?: 0}g", s.proteinProgress, Modifier.weight(1f))
                StatCard("脂肪", fmt(s.totalFat), "/ ${s.fatGoal ?: 0}g", s.fatProgress, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            StatCard("碳水", fmt(s.totalCarbohydrate), "/ ${s.carbGoal ?: 0}g", s.carbProgress)

            Spacer(Modifier.height(16.dp))
            InsightCard(s)
        }
    }
}

@Composable
private fun SegmentedControl(isWeekly: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .background(PanelSoft, RoundedCornerShape(999.dp))
            .padding(4.dp),
    ) {
        SegButton("日报", !isWeekly) { onToggle(false) }
        SegButton("周报", isWeekly) { onToggle(true) }
    }
}

@Composable
private fun SegButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Ink else Color.Transparent, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else TextSecondary)
    }
}

@Composable
private fun StatCard(label: String, value: String, goal: String, progress: Double?, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(" $goal", fontSize = 12.sp, color = TextTertiary)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE8E8EA), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(((progress ?: 0.0).toFloat() / 100f).coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(Ink, CircleShape)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("${(progress ?: 0.0).roundToInt()}% of 目标", fontSize = 11.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun InsightCard(s: NutritionSummaryResponse) {
    val cal = s.calorieProgress ?: 0.0
    val pro = s.proteinProgress ?: 0.0
    val text = when {
        s.totalCalories == 0 -> "今天还没有记录，去记录第一餐，或让 AI 帮你记录。"
        cal >= 100 -> "已达到热量目标，注意不要超量哦。"
        cal >= 60 -> "进度良好，晚餐记得均衡搭配。"
        else -> "摄入还有空间，别忘记补充优质蛋白。"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("今日小结", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text("热量完成 ${cal.roundToInt()}%，蛋白质完成 ${pro.roundToInt()}%。$text", fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
    }
}

private fun fmt(v: Double?): String = if (v == null) "0" else if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
