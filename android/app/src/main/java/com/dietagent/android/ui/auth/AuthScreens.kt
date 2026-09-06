package com.dietagent.android.ui.auth

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dietagent.android.DietAgentApp
import com.dietagent.android.data.repo.AuthRepository
import com.dietagent.android.ui.theme.Ink
import com.dietagent.android.ui.theme.Line
import com.dietagent.android.ui.theme.Panel
import com.dietagent.android.ui.theme.TextSecondary
import com.dietagent.android.ui.theme.TextTertiary
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(20.dp)
private val PillShape = RoundedCornerShape(999.dp)

@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Ink, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🥗", fontSize = 28.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("膳灵", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = Line,
            focusedLabelColor = Ink,
        ),
    )
}

@Composable
fun LoginScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DietAgentApp
    val repo: AuthRepository = app.authRepository
    val scope = rememberCoroutineScope()

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Panel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader()
            Spacer(Modifier.height(28.dp))
            Text("欢迎回来", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("登录后继续你的饮食管理", fontSize = 13.sp, color = TextTertiary)
            Spacer(Modifier.height(26.dp))

            AuthField(username, { username = it }, "用户名")
            Spacer(Modifier.height(12.dp))
            AuthField(password, { password = it }, "密码", isPassword = true)

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 12.sp, color = Color(0xFFD54242))
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        error = "请输入用户名和密码"
                        return@Button
                    }
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            repo.login(username, password)
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "登录失败"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("登录", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("还没有账号？", fontSize = 13.sp, color = TextTertiary)
                TextButton(onClick = { navController.navigate("register") }) {
                    Text("立即注册", color = Ink, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DietAgentApp
    val repo: AuthRepository = app.authRepository
    val scope = rememberCoroutineScope()

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Panel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader()
            Spacer(Modifier.height(28.dp))
            Text("创建账号", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("一分钟开始你的 AI 饮食管理", fontSize = 13.sp, color = TextTertiary)
            Spacer(Modifier.height(26.dp))

            AuthField(username, { username = it }, "用户名（3-50字符）")
            Spacer(Modifier.height(12.dp))
            AuthField(password, { password = it }, "密码（至少 6 位）", isPassword = true)
            Spacer(Modifier.height(12.dp))
            AuthField(nickname, { nickname = it }, "昵称（可选）")

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 12.sp, color = Color(0xFFD54242))
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (username.length < 3 || password.length < 6) {
                        error = "用户名至少 3 位，密码至少 6 位"
                        return@Button
                    }
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            repo.register(username, password, nickname.ifBlank { null })
                            navController.navigate("home") {
                                popUpTo("register") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "注册失败"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("注册", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("已有账号？", fontSize = 13.sp, color = TextTertiary)
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("立即登录", color = Ink, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
