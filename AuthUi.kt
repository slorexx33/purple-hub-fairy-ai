package com.purplehub.app

import android.app.Activity
import android.util.Patterns
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
internal fun AccountGateScreen(
    auth: HubAuth?,
    onContinueLocal: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var createAccount by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(76.dp).background(Brush.linearGradient(listOf(Color(0xFFBE9CFF), Color(0xFF7146A8))), RoundedCornerShape(25.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            Spacer(Modifier.height(13.dp))
            Text("PURPLE HUB", fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, letterSpacing = 2.sp)
            Text("Твоё фиолетовое пространство", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onContinueLocal, modifier = Modifier.fillMaxWidth()) {
                Text("Открыть Purple Hub без аккаунта")
            }
            Text("Аккаунт необязателен: FAIRY API и остальные модули можно настроить локально.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(18.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(if (createAccount) "Создать аккаунт" else "С возвращением", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (createAccount) "Зарегистрируйся с любой действующей почтой — например Proton Mail, Gmail или Outlook." else "Войди через Google или используй любую электронную почту.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = email, onValueChange = { email = it.trim().take(254); message = "" },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Электронная почта") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        placeholder = { Text("user123@proton.me") }, singleLine = true
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it.take(128); message = "" },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Пароль") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { TextButton(onClick = { passwordVisible = !passwordVisible }) { Text(if (passwordVisible) "Скрыть" else "Показать") } },
                        singleLine = true
                    )
                    if (createAccount) {
                        OutlinedTextField(
                            value = confirmPassword, onValueChange = { confirmPassword = it.take(128); message = "" },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Повтори пароль") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            val cleanEmail = email.trim()
                            when {
                                !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> message = "Введи корректный адрес электронной почты."
                                password.length < 8 -> message = "Пароль должен содержать не менее 8 символов."
                                createAccount && password != confirmPassword -> message = "Пароли не совпадают."
                                auth == null -> message = "Firebase ещё не подключён. Инструкция находится в README_RU.txt."
                                else -> scope.launch {
                                    busy = true; message = ""
                                    try {
                                        if (createAccount) {
                                            auth.register(cleanEmail, password)
                                            message = "Аккаунт создан. Проверь почту и перейди по ссылке подтверждения."
                                        } else {
                                            auth.signIn(cleanEmail, password)
                                            onAuthenticated()
                                        }
                                    } catch (e: Exception) { message = authErrorMessage(e) }
                                    finally { busy = false }
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(if (createAccount) "Зарегистрироваться" else "Войти")
                    }
                    if (!createAccount) {
                        TextButton(
                            onClick = {
                                if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) message = "Сначала введи почту, на которую отправить ссылку."
                                else if (auth == null) message = "Сначала подключи Firebase."
                                else scope.launch {
                                    busy = true
                                    try { auth.sendPasswordReset(email.trim()); message = "Ссылка для сброса пароля отправлена на почту." }
                                    catch (e: Exception) { message = authErrorMessage(e) }
                                    finally { busy = false }
                                }
                            }, enabled = !busy, modifier = Modifier.align(Alignment.End)
                        ) { Text("Забыли пароль?") }
                    }
                    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text("ИЛИ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    OutlinedButton(
                        onClick = {
                            if (activity == null) message = "Не удалось открыть Google Sign-In из этого контекста."
                            else if (auth == null) message = "Firebase ещё не подключён."
                            else scope.launch {
                                busy = true; message = ""
                                try { auth.signInWithGoogle(activity); onAuthenticated() }
                                catch (e: Exception) { message = authErrorMessage(e) }
                                finally { busy = false }
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !busy
                    ) {
                        Text("G", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text("  Продолжить через Google")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (createAccount) "Уже есть аккаунт?" else "Впервые в Purple Hub?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { createAccount = !createAccount; message = "" }) { Text(if (createAccount) "Войти" else "Создать аккаунт") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Аккаунт используется для входа. Данные модулей пока остаются на этом устройстве и не синхронизируются автоматически.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun VerifyEmailScreen(
    auth: HubAuth,
    user: FirebaseUser,
    onVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Письмо отправлено на ${user.email.orEmpty()}.") }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(16.dp))
        Text("Подтверди почту", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Открой письмо от Firebase и нажми ссылку подтверждения. После этого вернись сюда.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Button(onClick = {
            scope.launch {
                busy = true
                try {
                    val refreshed = auth.refreshCurrentUser()
                    if (refreshed?.isEmailVerified == true) onVerified() else message = "Пока не вижу подтверждения. Открой письмо и попробуй ещё раз."
                } catch (e: Exception) { message = authErrorMessage(e) }
                finally { busy = false }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Я подтвердил почту")
        }
        OutlinedButton(onClick = {
            scope.launch {
                busy = true
                try { auth.resendVerification(); message = "Новое письмо отправлено." }
                catch (e: Exception) { message = authErrorMessage(e) }
                finally { busy = false }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Отправить письмо ещё раз") }
        Text(message, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onSignOut) { Text("Выйти из аккаунта") }
    }
}

@Composable
internal fun AccountSettingsCard(
    auth: HubAuth?,
    user: FirebaseUser?,
    onOpenAuth: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("АККАУНТ PURPLE HUB", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (user != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(11.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(user.displayName?.takeIf { it.isNotBlank() } ?: "Пользователь Purple Hub", fontWeight = FontWeight.SemiBold)
                        Text(user.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(if (user.providerData.any { it.providerId == "google.com" }) "Вход через Google" else "Вход по электронной почте", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("Авторизация подключена. Заметки, парольница и данные ZZZ пока хранятся локально — вход не включает облачную синхронизацию.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Выйти из аккаунта") }
            } else if (auth != null) {
                Text("Сейчас используется локальный режим. Войди или создай аккаунт, чтобы включить авторизацию.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Button(onClick = onOpenAuth, modifier = Modifier.fillMaxWidth()) { Text("Войти / зарегистрироваться") }
            } else {
                Text("Firebase ещё не подключён к проекту. После добавления google-services.json и включения провайдеров здесь появятся вход по почте и Google.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
