package com.dietagent.android.ui.diet

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dietagent.android.data.remote.dto.DietRecordResponse
import com.dietagent.android.data.repo.DietRepository
import com.dietagent.android.ui.theme.Background
import com.dietagent.android.ui.theme.Ink
import com.dietagent.android.ui.theme.Line
import com.dietagent.android.ui.theme.Panel
import com.dietagent.android.ui.theme.PanelSoft
import com.dietagent.android.ui.theme.TextSecondary
import com.dietagent.android.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val mealLabels = mapOf(
    "breakfast" to "早餐", "lunch" to "午餐", "dinner" to "晚餐", "snack" to "加餐",
    "早餐" to "早餐", "午餐" to "午餐", "晚餐" to "晚餐", "加餐" to "加餐",
)

private fun mealLabel(t: String?): String = mealLabels[t] ?: (t ?: "加餐")

/** 饮食记录页：按日列表 + 添加/编辑/删除 */
@Composable
fun DietScreen(app: DietAgentApp, modifier: Modifier = Modifier) {
    val repo: DietRepository = app.dietRepository
    val scope = rememberCoroutineScope()

    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var records by remember { mutableStateOf<List<DietRecordResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DietRecordResponse?>(null) }

    fun load() {
        scope.launch {
            loading = true
            runCatching { repo.getRecords(date) }.onSuccess { records = it }
            loading = false
        }
    }

    LaunchedEffect(date) { load() }

    Box(modifier = modifier.background(Background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("饮食记录", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                // 简单的日期前/后切换 + 今天
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { date = LocalDate.parse(date).minusDays(1).toString() }) { Text("‹", fontSize = 20.sp, color = Ink) }
                    Text(date, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.clickable {
                        date = LocalDate.now().toString()
                    })
                    TextButton(onClick = { date = LocalDate.parse(date).plusDays(1).toString() }) { Text("›", fontSize = 20.sp, color = Ink) }
                }
            }

            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("这一天还没有记录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Spacer(Modifier.height(6.dp))
                        Text("点击右下角添加，或在 AI 对话里说\"我吃了什么\"", fontSize = 12.sp, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(records) { r ->
                        RecordCard(r, onEdit = { editing = r; showDialog = true }, onDelete = {
                            scope.launch {
                                runCatching { repo.deleteRecord(r.id) }.onSuccess { load() }
                            }
                        })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = null; showDialog = true },
            containerColor = Ink,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加", tint = Color.White)
        }
    }

    if (showDialog) {
        RecordDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { food, meal, portion, cal, pro, fat, carb, time ->
                scope.launch {
                    runCatching {
                        if (editing == null) {
                            repo.addRecord(food, meal, portion, cal, pro, fat, carb, time)
                        } else {
                            repo.updateRecord(editing!!.id, food, meal, portion, cal, pro, fat, carb, time)
                        }
                    }.onSuccess { showDialog = false; load() }
                }
            },
        )
    }
}

@Composable
private fun RecordCard(
    r: DietRecordResponse,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(PanelSoft, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(mealLabel(r.mealType), fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.width(10.dp))
                Text(r.foodName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                Text("${r.calories ?: 0} kcal", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("蛋白 ${fmt(r.protein)}g", fontSize = 12.sp, color = TextSecondary)
                Text("脂肪 ${fmt(r.fat)}g", fontSize = 12.sp, color = TextSecondary)
                Text("碳水 ${fmt(r.carbohydrate)}g", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color(0xFFD54242), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RecordDialog(
    initial: DietRecordResponse?,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Int?, Double?, Double?, Double?, String) -> Unit,
) {
    var food by remember { mutableStateOf(initial?.foodName ?: "") }
    var meal by remember { mutableStateOf(initial?.mealType ?: "lunch") }
    var portion by remember { mutableStateOf(initial?.portionSize ?: "") }
    var cal by remember { mutableStateOf(initial?.calories?.toString() ?: "") }
    var pro by remember { mutableStateOf(initial?.protein?.toString() ?: "") }
    var fat by remember { mutableStateOf(initial?.fat?.toString() ?: "") }
    var carb by remember { mutableStateOf(initial?.carbohydrate?.toString() ?: "") }
    val time = (initial?.mealTime ?: LocalDateTime.now().toString()).take(19)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加饮食记录" else "编辑饮食记录", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MealSelector(meal) { meal = it }
                OutlinedTextField(food, { food = it }, label = { Text("食物名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(portion, { portion = it }, label = { Text("份量（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(cal, { cal = it }, label = { Text("热量") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(pro, { pro = it }, label = { Text("蛋白 g") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(fat, { fat = it }, label = { Text("脂肪 g") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(carb, { carb = it }, label = { Text("碳水 g") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (food.isBlank()) return@Button
                    onSave(
                        food.trim(),
                        meal,
                        portion.ifBlank { null },
                        cal.toIntOrNull(),
                        pro.toDoubleOrNull(),
                        fat.toDoubleOrNull(),
                        carb.toDoubleOrNull(),
                        time,
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                shape = RoundedCornerShape(999.dp),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun MealSelector(selected: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("breakfast" to "早餐", "lunch" to "午餐", "dinner" to "晚餐", "snack" to "加餐").forEach { (v, l) ->
            Box(
                modifier = Modifier
                    .background(if (selected == v) Ink else PanelSoft, RoundedCornerShape(999.dp))
                    .clickable { onChange(v) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(l, fontSize = 12.sp, color = if (selected == v) Color.White else TextSecondary)
            }
        }
    }
}

private fun fmt(v: Double?): String = if (v == null) "0" else if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
