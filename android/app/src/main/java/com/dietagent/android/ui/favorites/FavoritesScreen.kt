package com.dietagent.android.ui.favorites

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.dietagent.android.data.remote.dto.FavoriteFoodResponse
import com.dietagent.android.data.repo.FavoritesRepository
import com.dietagent.android.ui.theme.Background
import com.dietagent.android.ui.theme.Ink
import com.dietagent.android.ui.theme.Line
import com.dietagent.android.ui.theme.Panel
import com.dietagent.android.ui.theme.PanelSoft
import com.dietagent.android.ui.theme.TextSecondary
import com.dietagent.android.ui.theme.TextTertiary
import kotlinx.coroutines.launch

/** 偏好食物页：卡片列表 + 添加/编辑/删除 */
@Composable
fun FavoritesScreen(app: DietAgentApp, modifier: Modifier = Modifier) {
    val repo: FavoritesRepository = app.favoritesRepository
    val scope = rememberCoroutineScope()

    var foods by remember { mutableStateOf<List<FavoriteFoodResponse>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FavoriteFoodResponse?>(null) }

    fun load() {
        scope.launch { runCatching { repo.list() }.onSuccess { foods = it } }
    }

    LaunchedEffect(Unit) { load() }

    Box(modifier = modifier.background(Background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("偏好食物", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("AI 的推荐与食谱会优先围绕这些食物", fontSize = 11.sp, color = TextTertiary)
                }
            }

            if (foods.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("还没有标记偏好食物", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Spacer(Modifier.height(6.dp))
                        Text("告诉系统你爱吃什么，推荐更合口味", fontSize = 12.sp, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(foods) { f ->
                        FavoriteCard(f, onEdit = { editing = f; showDialog = true }, onDelete = {
                            scope.launch { runCatching { repo.delete(f.id) }.onSuccess { load() } }
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
        FavoriteDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { name, category, note, cal, pro, fat, carb ->
                scope.launch {
                    runCatching {
                        if (editing == null) {
                            repo.add(name, category, note, cal, pro, fat, carb)
                        } else {
                            repo.update(editing!!.id, name, category, note, cal, pro, fat, carb)
                        }
                    }.onSuccess { showDialog = false; load() }
                }
            },
        )
    }
}

@Composable
private fun FavoriteCard(f: FavoriteFoodResponse, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.foodName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                if (!f.category.isNullOrBlank()) {
                    Box(
                        Modifier
                            .background(PanelSoft, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(f.category, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
            if (!f.note.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(f.note, fontSize = 12.sp, color = TextTertiary)
            }
            if (f.caloriesPer100g != null || f.proteinPer100g != null || f.fatPer100g != null || f.carbPer100g != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    f.caloriesPer100g?.let { Text("$it kcal", fontSize = 12.sp, color = TextSecondary) }
                    f.proteinPer100g?.let { Text("蛋白 ${fmt(it)}g", fontSize = 12.sp, color = TextSecondary) }
                    f.fatPer100g?.let { Text("脂肪 ${fmt(it)}g", fontSize = 12.sp, color = TextSecondary) }
                    f.carbPer100g?.let { Text("碳水 ${fmt(it)}g", fontSize = 12.sp, color = TextSecondary) }
                }
                Text("/ 100g", fontSize = 10.sp, color = TextTertiary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
private fun FavoriteDialog(
    initial: FavoriteFoodResponse?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, Int?, Double?, Double?, Double?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.foodName ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var cal by remember { mutableStateOf(initial?.caloriesPer100g?.toString() ?: "") }
    var pro by remember { mutableStateOf(initial?.proteinPer100g?.toString() ?: "") }
    var fat by remember { mutableStateOf(initial?.fatPer100g?.toString() ?: "") }
    var carb by remember { mutableStateOf(initial?.carbPer100g?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加偏好" else "编辑偏好", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("食物名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("分类（如：高蛋白）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(cal, { cal = it }, label = { Text("热量/100g") }, singleLine = true, modifier = Modifier.weight(1f))
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
                    if (name.isBlank()) return@Button
                    onSave(
                        name.trim(),
                        category.ifBlank { null },
                        note.ifBlank { null },
                        cal.toIntOrNull(),
                        pro.toDoubleOrNull(),
                        fat.toDoubleOrNull(),
                        carb.toDoubleOrNull(),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                shape = RoundedCornerShape(999.dp),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
