package com.purplehub.app

import android.Manifest
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.PersistableBundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.Calendar
import java.util.Date
import java.io.File
import kotlinx.coroutines.launch

private enum class HubScreen { HOME, FAIRY, ZZZ, VAULT, SETTINGS, API_SETTINGS }

private data class FairyLocalReply(val answer: String, val destination: HubScreen? = null)

/** Локальные команды проверяются до любого обращения к API. */
private fun handleFairyLocalCommand(raw: String, prefs: HubPrefs): FairyLocalReply? {
    val text = raw.trim()
    val lower = text.lowercase(Locale.ROOT)
    if (lower in setOf("помощь", "help", "команды", "список команд", "что ты умеешь")) {
        return FairyLocalReply(
            "Я FAIRY в Purple Hub. Простые команды выполняю прямо на телефоне, без API.\n\n" +
                "• «заметка: текст» — сохранить заметку\n" +
                "• «мои заметки» / «найди заметку …» — поиск по заметкам\n" +
                "• «покажи память» — локальная память\n" +
                "• «покажи pity» / «мой отряд» — данные ZZZ Companion\n" +
                "• «открой ZZZ», «открой пароли», «домой» — переход между разделами\n" +
                "• «время», «какая дата», «API статистика» — локальная информация\n" +
                "Для веб-поиска включи переключатель «Веб-поиск» или напиши «найди в интернете …»."
        )
    }
    if (lower.matches(Regex("^(привет|приветики|здравствуй|доброе утро|добрый день|добрый вечер)[!. ]*$"))) {
        return FairyLocalReply("Привет! ✨ Я на связи. Локальные команды работают даже при ограничении API.")
    }
    val noteCommand = Regex("(?iu)^(?:заметка|создай заметку|сохрани заметку|запиши заметку)\\s*[:：]?\\s*(.*)$").matchEntire(text)
    if (noteCommand != null) {
        val noteText = noteCommand.groupValues[1].trim()
        if (noteText.isBlank()) return FairyLocalReply("После «заметка:» напиши текст, который нужно сохранить.")
        val notes = prefs.loadNotes().toMutableList()
        notes.add(0, FairyNote(System.currentTimeMillis().toString(), noteText.take(500)))
        prefs.saveNotes(notes)
        return FairyLocalReply("Сохранила заметку локально на этом устройстве. Она не отправлялась в AI.")
    }
    if (lower in setOf("мои заметки", "покажи заметки", "список заметок", "открой заметки")) {
        val notes = prefs.loadNotes()
        return FairyLocalReply(if (notes.isEmpty()) "Пока заметок нет." else "Твои заметки:\n" + notes.take(15).mapIndexed { i, n -> "${i + 1}. ${n.text}" }.joinToString("\n"))
    }
    if (lower.startsWith("найди заметку ")) {
        val query = text.substringAfter(' ').substringAfter(' ').trim()
        val found = prefs.loadNotes().filter { it.text.contains(query, ignoreCase = true) }.take(10)
        return FairyLocalReply(if (query.isBlank()) "Напиши, что искать: «найди заметку …»." else if (found.isEmpty()) "Не нашла заметок по запросу «$query»." else found.mapIndexed { i, n -> "${i + 1}. ${n.text}" }.joinToString("\n"))
    }
    if (lower in setOf("память", "покажи память", "мои воспоминания", "что ты помнишь")) {
        val memories = prefs.loadMemories()
        return FairyLocalReply(if (memories.isEmpty()) "Сохранённая память пока пуста. Она хранится только на устройстве." else "Локальная память:\n" + memories.take(15).mapIndexed { i, n -> "${i + 1}. ${n.text}" }.joinToString("\n"))
    }
    if (lower.contains("api статистика") || lower == "api статус" || lower == "статистика api") {
        val usage = prefs.loadUsage()
        return FairyLocalReply("Статистика API: ${usage.first} успешных запросов, ${usage.second} учтённых токенов. Секретный ключ и содержимое паролей не читаются.\nОценка контекста следующего запроса появится перед отправкой.")
    }
    if (lower in setOf("время", "который час", "сколько времени", "время сейчас")) {
        return FairyLocalReply("Сейчас ${java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())}.")
    }
    if (lower in setOf("какая дата", "какое сегодня число", "сегодняшняя дата", "дата")) {
        return FairyLocalReply("Сегодня ${java.text.SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(java.util.Date())}.")
    }
    if (lower.contains("pity") || lower.contains("гарант") || lower.contains("сколько круток") || lower.contains("покажи крутки") || lower.contains("мой отряд") || lower.contains("покажи отряд")) {
        val z = prefs.loadZzz()
        return FairyLocalReply("Локальные данные ZZZ Companion:\nКрутки: ${z.pulls}\nPity: ${z.pity}/${z.pityLimit}\nГарант: ${if (z.guarantee) "есть" else "нет"}\nЦель: ${z.target.ifBlank { "не указана" }}\nОтряд: ${z.team.joinToString(", ")}")
    }
    if (lower.matches(Regex("^(открой|запусти|перейди в )?\\s*(zzz|zzz companion|компаньон zzz)$"))) {
        return FairyLocalReply("Открываю ZZZ Companion. Данные остаются локально.", HubScreen.ZZZ)
    }
    if (lower.matches(Regex("^(открой|запусти|перейди в )?\\s*(пароли|парольницу|password vault)$")) || lower == "покажи пароли") {
        return FairyLocalReply("Открываю Password Vault. Для просмотра записей потребуется мастер-пароль.", HubScreen.VAULT)
    }
    if (lower in setOf("домой", "главная", "открой главную", "открой purple hub")) return FairyLocalReply("Возвращаю на главную.", HubScreen.HOME)
    if (lower in setOf("настройки", "открой настройки")) return FairyLocalReply("Открываю настройки Purple Hub.", HubScreen.SETTINGS)
    return null
}

private fun isFairySearchIntent(text: String): Boolean = Regex(
    "(?iu)^(?:эй\\s+)?(?:fairy[,! ]*)?(?:найди(?:\\s+в\\s+интернете)?|поищи(?:\\s+в\\s+интернете)?|загугли|проверь\\s+актуальную\\s+информацию\\s+о|расскажи\\s+последние\\s+новости\\s+о|что\\s+нового\\s+по)\\s+.+$"
).containsMatchIn(text.trim())

private fun cleanFairySearchQuery(text: String): String = text.trim()
    .replace(Regex("(?iu)^(?:эй\\s+)?(?:fairy[,! ]*)?(?:найди(?:\\s+в\\s+интернете)?|поищи(?:\\s+в\\s+интернете)?|загугли|проверь\\s+актуальную\\s+информацию\\s+о|расскажи\\s+последние\\s+новости\\s+о|что\\s+нового\\s+по)\\s*"), "")
    .trim().trimEnd('.', '!', '?')

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(25, 20, 36)
        window.navigationBarColor = android.graphics.Color.rgb(18, 15, 27)
        setContent { PurpleHubApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurpleHubApp() {
    val context = LocalContext.current
    val prefs = remember { HubPrefs(context) }
    val hubAuth = remember { HubAuth.create(context) }
    var authUser by remember(hubAuth) { mutableStateOf(hubAuth?.currentUser) }
    var authRefreshVersion by remember { mutableStateOf(0) }
    val authUiPrefs = remember { context.getSharedPreferences("purple_hub_auth_ui_v1", Context.MODE_PRIVATE) }
    // Firebase account is optional. If no Firebase user is currently signed in,
    // do not block the app behind an auth gate (including stale false values from older builds).
    var localMode by remember { mutableStateOf(authUiPrefs.getBoolean("continue_locally", true) || hubAuth?.currentUser == null) }
    DisposableEffect(hubAuth) {
        if (hubAuth == null) onDispose { }
        else {
            val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { authUser = it.currentUser }
            hubAuth.firebaseAuth.addAuthStateListener(listener)
            onDispose { hubAuth.firebaseAuth.removeAuthStateListener(listener) }
        }
    }
    val signedInUser = authUser
    val emailVerified = remember(signedInUser, authRefreshVersion) { signedInUser?.isEmailVerified == true }
    val googleUser = signedInUser?.providerData?.any { it.providerId == "google.com" } == true
    val showAuthGate = hubAuth != null && !localMode && signedInUser == null
    val showEmailVerification = hubAuth != null && !localMode && signedInUser != null && !emailVerified && !googleUser
    var dark by remember { mutableStateOf(prefs.isDark()) }
    var screen by rememberSaveable { mutableStateOf(HubScreen.HOME) }
    var apiSettingsReturn by remember { mutableStateOf(HubScreen.FAIRY) }
    var pendingVaultEnvelope by remember { mutableStateOf<String?>(null) }
    var pendingZzzImport by remember { mutableStateOf<ZzzState?>(null) }
    var pendingFairyNotes by remember { mutableStateOf<List<FairyNote>?>(null) }
    var pendingFairyMemories by remember { mutableStateOf<List<FairyNote>?>(null) }
    var pendingFairyChat by remember { mutableStateOf<List<ChatMessage>?>(null) }
    var pendingFairyReminders by remember { mutableStateOf<List<HubReminder>?>(null) }

    val exportVaultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val envelope = prefs.vaultEnvelope()
            if (envelope.isNullOrBlank()) toast(context, "В хранилище пока нет резервной копии")
            else try {
                val backup = JSONObject().put("app", "PurpleHubPasswordVaultBackup").put("version", 1)
                    .put("encryptedVault", JSONObject(envelope)).toString(2)
                context.contentResolver.openOutputStream(uri)?.use { it.write(backup.toByteArray(Charsets.UTF_8)) }
                toast(context, "Зашифрованная копия сохранена")
            } catch (_: Exception) { toast(context, "Не удалось сохранить копию") }
        }
    }
    val importVaultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val root = JSONObject(raw)
            val envelope = root.optJSONObject("encryptedVault") ?: root
            if (envelope.optString("format") != "PurpleHubPasswordVault" || envelope.optInt("version", -1) != 1)
                throw IllegalArgumentException("Неизвестный формат резервной копии")
            pendingVaultEnvelope = envelope.toString()
            screen = HubScreen.VAULT
            toast(context, "Копия выбрана. Введи её мастер-пароль.")
        } catch (e: Exception) { toast(context, e.message ?: "Не удалось прочитать копию") }
    }
    val exportFairyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) try {
            fun toArray(items: List<FairyNote>): JSONArray {
                val a = JSONArray()
                items.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("at", it.at)) }
                return a
            }
            val chat = JSONArray()
            prefs.loadChat().forEach { item ->
                val sources = JSONArray()
                item.sources.forEach { source -> sources.put(JSONObject().put("title", source.title).put("url", source.url)) }
                val attachments = JSONArray()
                item.attachments.forEach { attachment ->
                    attachments.put(JSONObject().put("name", attachment.name).put("mimeType", attachment.mimeType).put("analysis", attachment.analysis))
                }
                chat.put(JSONObject().put("role", item.role).put("text", item.text).put("at", item.at)
                    .put("localOnly", item.localOnly).put("sources", sources).put("attachments", attachments))
            }
            val reminders = JSONArray()
            prefs.loadReminders().forEach { item -> reminders.put(JSONObject().put("id", item.id).put("title", item.title).put("at", item.at)) }
            val backup = JSONObject().put("app", "PurpleHubFairyNotes").put("version", 6)
                .put("notes", toArray(prefs.loadNotes())).put("reminders", reminders)
                .put("memories", toArray(prefs.loadMemories())).put("chat", chat)
                .put("aiUsage", JSONObject().put("requests", prefs.loadUsage().first).put("tokens", prefs.loadUsage().second))
                .toString(2)
            context.contentResolver.openOutputStream(uri)?.use { it.write(backup.toByteArray(Charsets.UTF_8)) }
            toast(context, "Заметки FAIRY экспортированы")
        } catch (_: Exception) { toast(context, "Не удалось экспортировать данные FAIRY") }
    }
    val importFairyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val root = JSONObject(raw)
            if (root.optString("app") != "PurpleHubFairyNotes" || root.optInt("version", -1) !in 1..6)
                throw IllegalArgumentException("Формат резервной копии FAIRY не распознан")
            fun readArray(name: String): List<FairyNote> {
                val a = root.optJSONArray(name) ?: return emptyList()
                return (0 until a.length()).mapNotNull { i ->
                    val o = a.optJSONObject(i) ?: return@mapNotNull null
                    val text = o.optString("text", "").trim()
                    if (text.isBlank() || text.length > 500) null else FairyNote(o.optString("id", System.currentTimeMillis().toString() + i), text, o.optLong("at", System.currentTimeMillis()))
                }.take(100)
            }
            pendingFairyNotes = readArray("notes")
            pendingFairyMemories = root.optJSONArray("memories")?.let { readArray("memories") } ?: prefs.loadMemories()
            pendingFairyChat = run {
                val a = root.optJSONArray("chat") ?: return@run null
                (0 until a.length()).mapNotNull { i ->
                    val o = a.optJSONObject(i) ?: return@mapNotNull null
                    val role = o.optString("role", "")
                    val text = o.optString("text", "").trim()
                    if (role !in setOf("user", "assistant") || text.isBlank()) return@mapNotNull null
                    val src = o.optJSONArray("sources") ?: JSONArray()
                    val sources = (0 until src.length()).mapNotNull { j ->
                        val so = src.optJSONObject(j) ?: return@mapNotNull null
                        val url = so.optString("url", "")
                        if (url.startsWith("https://")) FairySource(so.optString("title", "Источник").take(120), url.take(1000)) else null
                    }.take(8)
                    val attachmentsArray = o.optJSONArray("attachments") ?: JSONArray()
                    val attachments = (0 until minOf(attachmentsArray.length(), 4)).mapNotNull { k ->
                        val attachment = attachmentsArray.optJSONObject(k) ?: return@mapNotNull null
                        val name = attachment.optString("name", "Вложение").take(120)
                        val mime = attachment.optString("mimeType", "application/octet-stream").take(120)
                        val analysis = attachment.optString("analysis", "").take(7000)
                        if (analysis.isBlank()) null else ChatAttachment(name, mime, analysis)
                    }
                    ChatMessage(role, text.take(6000), o.optLong("at", System.currentTimeMillis()), sources, o.optBoolean("localOnly", false), attachments)
                }.takeLast(80)
            }
            pendingFairyReminders = root.optJSONArray("reminders")?.let { a ->
                (0 until a.length()).mapNotNull { i ->
                    val o = a.optJSONObject(i) ?: return@mapNotNull null
                    val id = o.optLong("id", System.currentTimeMillis() + i)
                    val title = o.optString("title", "").trim()
                    val at = o.optLong("at", 0L)
                    if (title.isBlank() || at <= System.currentTimeMillis()) null else HubReminder(id, title.take(120), at)
                }.take(100)
            }
            screen = HubScreen.FAIRY
            toast(context, "Резервная копия FAIRY загружена")
        } catch (e: Exception) { toast(context, e.message ?: "Не удалось прочитать копию FAIRY") }
    }
    val exportZzzLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) try {
            val state = prefs.loadZzz()
            val data = JSONObject()
                .put("pulls", state.pulls).put("pity", state.pity).put("pityLimit", state.pityLimit)
                .put("guarantee", state.guarantee).put("target", state.target)
                .put("teamName", state.teamName).put("team", JSONArray(state.team))
                .put("materials", JSONArray(state.materials)).put("history", JSONArray(state.history))
            val backup = JSONObject().put("app", "ZZZ Companion").put("schema", 2).put("data", data).toString(2)
            context.contentResolver.openOutputStream(uri)?.use { it.write(backup.toByteArray(Charsets.UTF_8)) }
            toast(context, "Резервная копия ZZZ сохранена")
        } catch (_: Exception) { toast(context, "Не удалось сохранить копию ZZZ") }
    }
    val importZzzLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val root = JSONObject(raw)
            val data = root.optJSONObject("data") ?: root
            fun stringArray(name: String, fallback: List<String>): List<String> {
                val a = data.optJSONArray(name) ?: return fallback
                return (0 until a.length()).mapNotNull { i ->
                    val item = a.opt(i)
                    when (item) {
                        is String -> item.takeIf { it.isNotBlank() }
                        is JSONObject -> {
                            val noteText = item.optString("text", "")
                            if (noteText.isNotBlank()) noteText.take(110) else item.optString("name", "").takeIf { it.isNotBlank() }?.let { n ->
                                val agent = item.optString("agent", "")
                                val have = item.optInt("have", 0)
                                val need = item.optInt("need", 0)
                                if (need > 0) "$n · $have/$need" else if (agent.isNotBlank()) "$n · $agent" else n
                            }
                        }
                        else -> null
                    }
                }.take(30)
            }
            val defaultTeam = listOf("Солдат 0 Энби", "Грейс Ховард", "Нэкомата")
            val importedTeam = stringArray("team", defaultTeam).take(3).ifEmpty { defaultTeam }
            val importedHistory = stringArray("history", emptyList()).take(20)
            pendingZzzImport = ZzzState(
                pulls = data.optInt("pulls", 0).coerceIn(0, 99999),
                pity = data.optInt("pity", 0).coerceIn(0, 200),
                pityLimit = data.optInt("pityLimit", 90).coerceIn(1, 200),
                guarantee = data.optBoolean("guarantee", false),
                target = data.optString("target", "").take(80),
                teamName = data.optString("teamName", "Основной отряд").take(60),
                team = importedTeam,
                materials = stringArray("materials", emptyList()),
                history = importedHistory
            )
            screen = HubScreen.ZZZ
            toast(context, "Резервная копия ZZZ загружена")
        } catch (_: Exception) { toast(context, "Не удалось прочитать резервную копию ZZZ") }
    }

    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFFC6A8FF), onPrimary = Color(0xFF2B1946),
        secondary = Color(0xFFE0B8F5), background = Color(0xFF130F1D),
        surface = Color(0xFF211A2D), surfaceVariant = Color(0xFF30263F),
        onSurface = Color(0xFFF2ECFA), onSurfaceVariant = Color(0xFFC9BBD8),
        outline = Color(0xFF594A6D)
    ) else lightColorScheme(
        primary = Color(0xFF7650B5), onPrimary = Color.White,
        secondary = Color(0xFF875B9D), background = Color(0xFFF7F3FC),
        surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFEDE5F6),
        onSurface = Color(0xFF251D30), onSurfaceVariant = Color(0xFF655A70),
        outline = Color(0xFFD3C5E0)
    )

    MaterialTheme(colorScheme = scheme) {
        SideEffect {
            windowColors(context, dark, scheme.surface.toArgb())
        }
        if (showAuthGate) {
            AccountGateScreen(
                auth = hubAuth,
                onContinueLocal = {
                    localMode = true
                    authUiPrefs.edit().putBoolean("continue_locally", true).apply()
                },
                onAuthenticated = {
                    localMode = false
                    authUiPrefs.edit().putBoolean("continue_locally", false).apply()
                }
            )
        } else if (showEmailVerification && signedInUser != null && hubAuth != null) {
            VerifyEmailScreen(
                auth = hubAuth,
                user = signedInUser,
                onVerified = { authUser = hubAuth.currentUser; authRefreshVersion += 1 },
                onSignOut = {
                    hubAuth.signOut(context)
                    localMode = false
                    authUiPrefs.edit().putBoolean("continue_locally", false).apply()
                }
            )
        } else Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(if (screen == HubScreen.HOME) "PURPLE HUB" else when (screen) {
                                HubScreen.FAIRY -> "FAIRY"
                                HubScreen.ZZZ -> "ZZZ COMPANION"
                                HubScreen.VAULT -> "PASSWORD VAULT"
                                HubScreen.SETTINGS -> "НАСТРОЙКИ"
                                HubScreen.API_SETTINGS -> "AI API"
                                else -> "PURPLE HUB"
                            }, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, fontSize = 16.sp)
                            if (screen == HubScreen.HOME) Text("NATIVE EDITION · 5.6.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        if (screen == HubScreen.SETTINGS || screen == HubScreen.API_SETTINGS) IconButton(onClick = {
                            screen = if (screen == HubScreen.API_SETTINGS) apiSettingsReturn else HubScreen.HOME
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        } else {
                            Box(Modifier.padding(start = 14.dp).size(34.dp).clip(RoundedCornerShape(12.dp)).background(
                                Brush.linearGradient(listOf(Color(0xFFB78BFF), Color(0xFF7751A8)))
                            ), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    actions = {
                        if (screen != HubScreen.SETTINGS && screen != HubScreen.API_SETTINGS) IconButton(onClick = { screen = HubScreen.SETTINGS }) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                if (screen != HubScreen.SETTINGS && screen != HubScreen.API_SETTINGS) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(selected = screen == HubScreen.HOME, onClick = { screen = HubScreen.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Домой") })
                    NavigationBarItem(selected = screen == HubScreen.FAIRY, onClick = { screen = HubScreen.FAIRY }, icon = { Icon(Icons.Default.AutoAwesome, null) }, label = { Text("FAIRY") })
                    NavigationBarItem(selected = screen == HubScreen.ZZZ, onClick = { screen = HubScreen.ZZZ }, icon = { Icon(Icons.Default.Gamepad, null) }, label = { Text("ZZZ") })
                    NavigationBarItem(selected = screen == HubScreen.VAULT, onClick = { screen = HubScreen.VAULT }, icon = { Icon(Icons.Default.Key, null) }, label = { Text("Пароли") })
                }
            }
        ) { inner ->
            AnimatedContent(targetState = screen, label = "purple-hub-navigation", modifier = Modifier.padding(inner)) { target ->
                when (target) {
                    HubScreen.HOME -> HomeScreen(onOpen = {
                        if (it == HubScreen.API_SETTINGS) apiSettingsReturn = HubScreen.HOME
                        screen = it
                    }, dark = dark)
                    HubScreen.FAIRY -> FairyScreen(
                        prefs = prefs,
                        onOpenSettings = { apiSettingsReturn = HubScreen.FAIRY; screen = HubScreen.API_SETTINGS },
                        onNavigate = { screen = it },
                        importedNotes = pendingFairyNotes,
                        importedMemories = pendingFairyMemories,
                        importedChat = pendingFairyChat,
                        importedReminders = pendingFairyReminders,
                        onImportHandled = { pendingFairyNotes = null; pendingFairyMemories = null },
                        onImportChatHandled = { pendingFairyChat = null },
                        onImportRemindersHandled = { pendingFairyReminders = null },
                        onExport = { exportFairyLauncher.launch("purple-hub-fairy-backup.json") },
                        onImport = { importFairyLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }
                    )
                    HubScreen.ZZZ -> ZzzScreen(
                        prefs = prefs,
                        importedState = pendingZzzImport,
                        onImportHandled = { pendingZzzImport = null },
                        onExport = { exportZzzLauncher.launch("zzz-companion-backup.json") },
                        onImport = { importZzzLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }
                    )
                    HubScreen.VAULT -> VaultScreen(
                        prefs = prefs,
                        importedEnvelope = pendingVaultEnvelope,
                        onImportHandled = { pendingVaultEnvelope = null },
                        onExport = { exportVaultLauncher.launch("purple-password-vault-backup.json") },
                        onImport = { importVaultLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }
                    )
                    HubScreen.API_SETTINGS -> ApiMonitorSection(prefs)
                    HubScreen.SETTINGS -> SettingsScreen(
                        prefs = prefs,
                        onExportFairy = { exportFairyLauncher.launch("purple-hub-fairy-backup.json") },
                        onImportFairy = { importFairyLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                        dark = dark,
                        onDarkChange = { dark = it; prefs.setDark(it) },
                        auth = hubAuth,
                        user = signedInUser,
                        onOpenAuth = {
                            localMode = false
                            authUiPrefs.edit().putBoolean("continue_locally", false).apply()
                        },
                        onSignOut = {
                            hubAuth?.signOut(context)
                            localMode = false
                            authUiPrefs.edit().putBoolean("continue_locally", false).apply()
                        },
                        onBack = { screen = HubScreen.HOME }
                    )
                }
            }
        }
    }
}

private fun windowColors(context: Context, dark: Boolean, lightSurface: Int) {
    val activity = context as? ComponentActivity ?: return
    val window = activity.window
    window.statusBarColor = if (dark) android.graphics.Color.rgb(25, 20, 36) else lightSurface
    window.navigationBarColor = if (dark) android.graphics.Color.rgb(18, 15, 27) else lightSurface
    @Suppress("DEPRECATION")
    run {
        window.decorView.systemUiVisibility = if (dark) 0 else
            (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }
}

@Composable
private fun HomeScreen(onOpen: (HubScreen) -> Unit, dark: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF3A2855), Color(0xFF7450A6), Color(0xFF9A70C7)))).padding(22.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Text("NATIVE · ANDROID", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFEAD9FF), modifier = Modifier.size(34.dp))
                        }
                        Text("Твой центр управления", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
                        Text("Всё важное — в одном фиолетовом пространстве.", color = Color(0xFFE7DDF2), fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatPill("3", "модуля")
                            StatPill("5.6.0", "native")
                        }
                    }
                }
            }
        }
        item { Text("ТВОИ МОДУЛИ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold) }
        item { ModuleCard("FAIRY", "Твой AI-помощник", "Диалог, локальные заметки и голосовой ввод", Icons.Default.AutoAwesome, Color(0xFF9D78DA)) { onOpen(HubScreen.FAIRY) } }
        item { ModuleCard("ZZZ Companion", "Zenless Zone Zero", "Крутки, pity, гарантия, отряд и материалы", Icons.Default.Gamepad, Color(0xFFDA9BC9)) { onOpen(HubScreen.ZZZ) } }
        item { ModuleCard("Password Vault", "Генератор и хранилище", "Случайные пароли и AES-GCM зашифрованные записи", Icons.Default.Key, Color(0xFF8F9CDD)) { onOpen(HubScreen.VAULT) } }
        item {
            OutlinedButton(onClick = { onOpen(HubScreen.API_SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Настроить AI API")
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Переход на Native", fontWeight = FontWeight.Bold)
                    Text("Интерфейс больше не использует WebView, HTML или JavaScript. Экраны построены на Kotlin и Jetpack Compose.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label, color = Color(0xFFE5D9F2), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ModuleCard(title: String, subtitle: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).animateContentSize(spring()),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(27.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, lineHeight = 17.sp)
            }
            Icon(Icons.Default.MoreHoriz, contentDescription = "Открыть", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FairyScreen(
    prefs: HubPrefs,
    onOpenSettings: () -> Unit,
    onNavigate: (HubScreen) -> Unit,
    importedNotes: List<FairyNote>?,
    importedMemories: List<FairyNote>?,
    importedChat: List<ChatMessage>?,
    importedReminders: List<HubReminder>?,
    onImportHandled: () -> Unit,
    onImportChatHandled: () -> Unit,
    onImportRemindersHandled: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(importedNotes, importedMemories) {
        if (importedNotes != null && importedMemories != null) {
            prefs.saveNotes(importedNotes)
            prefs.saveMemories(importedMemories)
            onImportHandled()
            toast(context, "Заметки и память восстановлены")
        }
    }
    LaunchedEffect(importedReminders) {
        if (importedReminders != null) {
            prefs.loadReminders().forEach { ReminderScheduler.cancel(context, it) }
            prefs.saveReminders(importedReminders)
            importedReminders.forEach { ReminderScheduler.schedule(context, it) }
            onImportRemindersHandled()
            toast(context, "Напоминания восстановлены")
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterTab("Диалог", tab == 0) { tab = 0 }
            FilterTab("Заметки", tab == 1) { tab = 1 }
            Spacer(Modifier.weight(1f))
            AssistChip(onClick = onOpenSettings, label = { Text("API") }, leadingIcon = { Icon(Icons.Default.Settings, null, Modifier.size(16.dp)) })
        }
        if (tab == 0) FairyChat(prefs, onNavigate, importedChat, onImportChatHandled)
        else FairyNotes(prefs, importedNotes, importedMemories, onImportHandled, onExport, onImport)
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.filledTonalButtonColors(
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 8.dp)) { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
private fun FairyChat(
    prefs: HubPrefs,
    onNavigate: (HubScreen) -> Unit,
    importedChat: List<ChatMessage>?,
    onImportChatHandled: () -> Unit
) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(prefs.loadChat()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var webSearchMode by rememberSaveable { mutableStateOf(false) }
    var lastEstimate by remember { mutableStateOf(0) }
    var selectedAttachments by remember { mutableStateOf<List<ChatAttachment>>(emptyList()) }
    var processingAttachments by remember { mutableStateOf(false) }
    var attachmentMenuExpanded by remember { mutableStateOf(false) }
    var showVisionConsentDialog by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun processUris(uris: List<Uri>) {
        if (processingAttachments || uris.isEmpty()) return
        val room = (4 - selectedAttachments.size).coerceAtLeast(0)
        if (room == 0) { toast(context, "Можно прикрепить не больше 4 файлов к одному сообщению"); return }
        processingAttachments = true
        coroutineScope.launch {
            val successful = mutableListOf<ChatAttachment>()
            val errors = mutableListOf<String>()
            uris.take(room).forEach { uri ->
                try { successful += LocalAttachmentAnalyzer.analyzeUri(context, uri) }
                catch (error: Exception) { errors += (error.message ?: "Не удалось обработать файл") }
            }
            selectedAttachments = (selectedAttachments + successful).take(4)
            processingAttachments = false
            if (successful.isNotEmpty()) toast(context, "Добавлено вложений: ${successful.size}. Анализ выполнен на устройстве.")
            if (errors.isNotEmpty()) toast(context, errors.first().take(150))
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> processUris(uris) }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> processUris(uris) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val captured = pendingCameraFile
        pendingCameraFile = null
        if (saved && captured != null) {
            if (selectedAttachments.size >= 4) { captured.delete(); toast(context, "Можно прикрепить не больше 4 файлов к одному сообщению") }
            else {
                processingAttachments = true
                coroutineScope.launch {
                    try {
                        val attachment = LocalAttachmentAnalyzer.analyzeCapturedFile(context, captured)
                        selectedAttachments = (selectedAttachments + attachment).take(4)
                        toast(context, "Снимок добавлен и обработан локально.")
                    } catch (error: Exception) { toast(context, error.message ?: "Не удалось обработать снимок") }
                    finally { captured.delete(); processingAttachments = false }
                }
            }
        } else captured?.delete()
    }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    val voiceSettings = prefs.loadVoiceSettings()
    DisposableEffect(context, voiceSettings.languageTag, voiceSettings.speechRate, voiceSettings.speechPitch, voiceSettings.voiceName) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine.language = Locale.forLanguageTag(voiceSettings.languageTag)
                engine.setSpeechRate(voiceSettings.speechRate)
                engine.setPitch(voiceSettings.speechPitch)
                engine.voices?.firstOrNull { it.name == voiceSettings.voiceName }?.let { engine.voice = it }
            }
        }
        tts.value = engine
        onDispose { engine.stop(); engine.shutdown(); tts.value = null }
    }
    LaunchedEffect(importedChat) {
        if (importedChat != null) {
            messages = importedChat.takeLast(80)
            prefs.saveChat(messages)
            onImportChatHandled()
            toast(context, "История FAIRY восстановлена")
        }
    }
    LaunchedEffect(messages.size, busy) {
        val target = (messages.size + if (busy) 1 else 0).coerceAtLeast(0)
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    fun append(message: ChatMessage) {
        messages = (messages + message).takeLast(80)
        prefs.saveChat(messages)
        val speech = prefs.loadVoiceSettings()
        if (message.role == "assistant" && speech.speechEnabled && speech.autoSpeak && message.text.isNotBlank()) {
            tts.value?.speak(message.text.take(1800), TextToSpeech.QUEUE_FLUSH, null, "fairy-auto-${message.at}")
        }
    }
    fun send(visionUploadConfirmed: Boolean = false) {
        val text = draft.trim()
        val attachmentsToSend = selectedAttachments
        if ((text.isBlank() && attachmentsToSend.isEmpty()) || busy || processingAttachments) return
        val containsVisionImages = attachmentsToSend.any { !it.imageDataUrl.isNullOrBlank() }
        if (containsVisionImages && !visionUploadConfirmed) {
            showVisionConsentDialog = true
            return
        }
        draft = ""
        selectedAttachments = emptyList()
        val messageText = text.ifBlank { "Проанализируй, пожалуйста, приложенные материалы." }
        val localReply = if (attachmentsToSend.isEmpty() && text.isNotBlank()) handleFairyLocalCommand(text, prefs) else null
        if (localReply != null) {
            append(ChatMessage("user", text, localOnly = true))
            localReply.destination?.let(onNavigate)
            append(ChatMessage("assistant", localReply.answer, localOnly = true))
            return
        }
        val privacy = prefs.loadPrivacySettings()
        val explicitSearch = attachmentsToSend.isEmpty() && isFairySearchIntent(text)
        val useSearch = attachmentsToSend.isEmpty() && (webSearchMode || explicitSearch)
        webSearchMode = false
        if (useSearch && !privacy.allowWebSearch) {
            append(ChatMessage("user", text, localOnly = true))
            append(ChatMessage("assistant", "Веб-поиск отключён в разделе «Приватность». Включи его в Control Center, чтобы отправлять запросы на поиск.", localOnly = true))
            return
        }
        if (useSearch) {
            append(ChatMessage("user", text, localOnly = true))
            busy = true
            val query = cleanFairySearchQuery(text)
            lastEstimate = ((query.length + 220) / 4).coerceAtLeast(1)
            val startedAt = SystemClock.elapsedRealtime()
            FairyApi.search(context, query) { result ->
                busy = false
                val success = result.error.isBlank()
                prefs.recordApiCall(SystemClock.elapsedRealtime() - startedAt, success,
                    result.error.contains("429") || result.error.contains("Лимит API"),
                    result.promptTokens + result.completionTokens, "web", result.error)
                if (!success) append(ChatMessage("assistant", "⚠ ${result.error}", localOnly = true))
                else append(ChatMessage("assistant", result.answer, sources = result.sources, localOnly = true))
            }
        } else {
            // Persist only the local analysis and filename. The compressed photo bytes stay
            // transient and are attached to this one request after explicit user consent.
            val historyAttachments = attachmentsToSend.map { it.copy(imageDataUrl = null) }
            append(ChatMessage("user", messageText, attachments = historyAttachments))
            busy = true
            val outgoingMessages = if (privacy.includeChatHistory) messages else messages.takeLast(1)
            val requestMessages = if (attachmentsToSend.isNotEmpty() && outgoingMessages.isNotEmpty()) {
                outgoingMessages.dropLast(1) + outgoingMessages.last().copy(attachments = attachmentsToSend)
            } else outgoingMessages
            val memoryContext = if (privacy.shareMemories) prefs.loadMemories().take(10).map { it.text } else emptyList()
            lastEstimate = FairyApi.estimatePromptTokens(outgoingMessages) + (memoryContext.sumOf { it.length } / 4)
            val startedAt = SystemClock.elapsedRealtime()
            FairyApi.send(context, requestMessages, memoryContext) { result ->
                busy = false
                val success = result.error.isBlank()
                prefs.recordApiCall(SystemClock.elapsedRealtime() - startedAt, success,
                    result.error.contains("429") || result.error.contains("Лимит API"),
                    result.promptTokens + result.completionTokens, "chat", result.error)
                val reply = if (!success) "⚠ ${result.error}" else result.answer
                append(ChatMessage("assistant", reply))
            }
        }
    }

    if (showVisionConsentDialog) {
        AlertDialog(
            onDismissRequest = { showVisionConsentDialog = false },
            title = { Text("Отправить фото в FAIRY Vision?") },
            text = { Text(
                "Чтобы FAIRY действительно поняла изображение, сжатая копия фото будет отправлена через настроенный AI API. Фото не отправляется при выборе файла — только после этого подтверждения. Применяются тарифы, лимиты и правила хранения данных твоего AI-провайдера. Для работы выбранная модель должна поддерживать изображения в формате OpenAI Chat Completions."
            ) },
            confirmButton = {
                Button(onClick = { showVisionConsentDialog = false; send(true) }) { Text("Отправить фото") }
            },
            dismissButton = { TextButton(onClick = { showVisionConsentDialog = false }) { Text("Не отправлять") } }
        )
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFC39CFF), Color(0xFF7951AF)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("FAIRY", fontWeight = FontWeight.Bold)
                Text(if (busy) "думает…" else "готова к диалогу", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val usage = prefs.loadUsage()
            Text("${usage.first} запросов · ${usage.second} токенов", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = {
                    if (prefs.loadPrivacySettings().allowWebSearch) webSearchMode = !webSearchMode
                    else toast(context, "Веб-поиск отключён в разделе «Приватность»")
                },
                label = { Text(if (!prefs.loadPrivacySettings().allowWebSearch) "Веб-поиск запрещён" else if (webSearchMode) "Веб-поиск: ВКЛ" else "Веб-поиск: выкл") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) }
            )
            Text("Контекст ~${if (lastEstimate > 0) lastEstimate else FairyApi.estimatePromptTokens(messages)} ток.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val cooldown = FairyApi.cooldownRemainingMillis(context)
        if (cooldown > 0L) {
            val seconds = (cooldown + 999L) / 1000L
            Text("API на паузе: ${seconds / 60} мин ${seconds % 60} сек. Локальные команды доступны.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            if (messages.isEmpty()) item {
                MessageBubble(ChatMessage("assistant", "Привет! Я FAIRY в нативном Purple Hub. ✨\n\nМожешь написать мне или начать с «помощь». Для свободного AI-диалога сначала подключи API в настройках."))
            }
            items(messages) { message ->
                AnimatedVisibility(visible = true, enter = fadeIn()) {
                    MessageBubble(message, if (prefs.loadVoiceSettings().speechEnabled) ({ text ->
                        tts.value?.speak(text.take(1800), TextToSpeech.QUEUE_FLUSH, null, "fairy-${message.at}")
                    }) else null)
                }
            }
            if (busy) item { Text("FAIRY печатает…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp)) }
        }
        AnimatedVisibility(visible = selectedAttachments.isNotEmpty() || processingAttachments, enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxWidth().animateContentSize(spring()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                selectedAttachments.forEachIndexed { index, attachment ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 7.dp)) {
                            Text(attachment.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            val isVisionImage = !attachment.imageDataUrl.isNullOrBlank()
                            Text(
                                if (isVisionImage) "Фото подготовлено для FAIRY Vision" else "Текст обработан локально · ${(attachment.analysis.length / 4).coerceAtLeast(1)} ток. примерно",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                            )
                        }
                        IconButton(onClick = { selectedAttachments = selectedAttachments.filterIndexed { itemIndex, _ -> itemIndex != index } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Удалить вложение", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (processingAttachments) Text("FAIRY анализирует файлы на устройстве…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (selectedAttachments.any { !it.imageDataUrl.isNullOrBlank() })
                        "Фото остаются на устройстве, пока ты не подтвердишь отправку. Для Vision будет передана сжатая копия текущему AI-провайдеру; его тариф и правила обработки данных применяются."
                    else "Файлы анализируются на устройстве. При отправке сообщения FAIRY получит извлечённый текст через настроенный AI API.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (!spoken.isNullOrBlank()) draft = if (draft.isBlank()) spoken else "$draft $spoken"
            }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) launchSpeech(context, prefs.loadVoiceSettings().languageTag) { micLauncher.launch(it) } else toast(context, "Для голосового ввода нужен доступ к микрофону")
            }
            Box {
                IconButton(onClick = { attachmentMenuExpanded = true }, enabled = !busy && !processingAttachments) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить вложение", tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = attachmentMenuExpanded, onDismissRequest = { attachmentMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Фото из галереи") }, onClick = { attachmentMenuExpanded = false; imagePickerLauncher.launch(arrayOf("image/*")) })
                    DropdownMenuItem(text = { Text("Файл или документ") }, onClick = { attachmentMenuExpanded = false; filePickerLauncher.launch(arrayOf("*/*")) })
                    DropdownMenuItem(text = { Text("Сфотографировать") }, onClick = {
                        attachmentMenuExpanded = false
                        if (selectedAttachments.size >= 4) toast(context, "Можно прикрепить не больше 4 файлов к одному сообщению")
                        else {
                            val photo = File.createTempFile("purplehub_camera_", ".jpg", context.cacheDir)
                            try {
                                pendingCameraFile = photo
                                val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
                                cameraLauncher.launch(photoUri)
                            } catch (_: Exception) {
                                pendingCameraFile = null
                                photo.delete()
                                toast(context, "Не удалось открыть камеру")
                            }
                        }
                    })
                }
            }
            OutlinedTextField(
                value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Напиши FAIRY…") }, shape = RoundedCornerShape(18.dp), maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() })
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !busy && prefs.loadVoiceSettings().voiceInputEnabled) {
                    Icon(Icons.Default.Mic, contentDescription = "Голосовой ввод", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { send() }, enabled = (draft.isNotBlank() || selectedAttachments.isNotEmpty()) && !busy && !processingAttachments,
                    modifier = Modifier.clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

private fun launchSpeech(context: Context, languageTag: String = "ru-RU", launch: (Intent) -> Unit) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        toast(context, "На устройстве нет службы распознавания речи"); return
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи что-нибудь для FAIRY")
    }
    try { launch(intent) } catch (_: Exception) { toast(context, "Не удалось открыть голосовой ввод") }
}

@Composable
private fun MessageBubble(message: ChatMessage, onSpeak: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val user = message.role == "user"
    Row(Modifier.fillMaxWidth().animateContentSize(spring()), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.fillMaxWidth(if (user) 0.88f else 0.80f).clip(RoundedCornerShape(20.dp)).background(
            if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ).padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(if (user) "ТЫ" else "FAIRY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = if (user) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(message.text, color = if (user) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, lineHeight = 21.sp)
            if (message.attachments.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                message.attachments.forEach { attachment ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                        Column(Modifier.weight(1f).padding(start = 7.dp)) {
                            Text(attachment.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Text("Материал обработан на устройстве", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            if (message.sources.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Источники", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                message.sources.forEach { source ->
                    Text(
                        source.title.ifBlank { source.url },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().clickable {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                            catch (_: Exception) { toast(context, "Не удалось открыть ссылку") }
                        }.padding(vertical = 3.dp)
                    )
                }
            }
        }
        if (!user && onSpeak != null) IconButton(onClick = { onSpeak(message.text) }) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Озвучить ответ", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FairyNotes(
    prefs: HubPrefs,
    importedNotes: List<FairyNote>?,
    importedMemories: List<FairyNote>?,
    onImportHandled: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val context = LocalContext.current
    var type by rememberSaveable { mutableStateOf(0) }
    var notes by remember { mutableStateOf(prefs.loadNotes()) }
    var memories by remember { mutableStateOf(prefs.loadMemories()) }
    var draft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(importedNotes, importedMemories) {
        if (importedNotes != null && importedMemories != null) {
            notes = importedNotes
            memories = importedMemories
            prefs.saveNotes(notes)
            prefs.saveMemories(memories)
            onImportHandled()
            toast(context, "Заметки и память восстановлены")
        }
    }
    val active = if (type == 0) notes else memories
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterTab("Заметки", type == 0) { type = 0 }
            FilterTab("Память", type == 1) { type = 1 }
        }
        Text(if (type == 0) "Локальные заметки" else "Сохранённая память", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            if (type == 0) "Остаются на этом устройстве и не отправляются в AI автоматически."
            else "Эти сведения хранятся локально. В этой версии они не добавляются к запросам AI автоматически.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(draft, { draft = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(if (type == 0) "Новая заметка…" else "Что FAIRY должна помнить локально…") }, shape = RoundedCornerShape(16.dp), maxLines = 3)
        Button(onClick = {
            if (draft.isNotBlank()) {
                val item = FairyNote(System.currentTimeMillis().toString(), draft.trim().take(500))
                if (type == 0) { notes = (listOf(item) + notes).take(100); prefs.saveNotes(notes) }
                else { memories = (listOf(item) + memories).take(100); prefs.saveMemories(memories) }
                draft = ""; toast(context, if (type == 0) "Заметка сохранена" else "Память сохранена")
            }
        }, enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Сохранить") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Экспорт") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Импорт 4.1.1") }
        }
        if (active.isEmpty()) EmptyState("Пока пусто", "Добавь первый элемент выше или импортируй резервную копию.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(active, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.text, Modifier.weight(1f))
                        IconButton(onClick = {
                            if (type == 0) { notes = notes.filterNot { it.id == item.id }; prefs.saveNotes(notes) }
                            else { memories = memories.filterNot { it.id == item.id }; prefs.saveMemories(memories) }
                        }) { Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZzzScreen(
    prefs: HubPrefs,
    importedState: ZzzState?,
    onImportHandled: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(importedState ?: prefs.loadZzz()) }
    var pityLimitInput by rememberSaveable { mutableStateOf((importedState ?: prefs.loadZzz()).pityLimit.toString()) }
    LaunchedEffect(importedState) {
        if (importedState != null) {
            state = importedState
            pityLimitInput = importedState.pityLimit.toString()
            prefs.saveZzz(importedState)
            onImportHandled()
        }
    }
    var newMaterial by rememberSaveable { mutableStateOf("") }
    fun update(next: ZzzState) { state = next; prefs.saveZzz(next) }
    val toNext = (state.pityLimit - state.pity).coerceAtLeast(0)
    val toTarget = toNext + if (state.guarantee) 0 else state.pityLimit
    val progress = (state.pity.toFloat() / state.pityLimit.toFloat()).coerceIn(0f, 1f)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2A50))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("ТВОЯ ЦЕЛЬ", color = Color(0xFFDCC8F1), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
                    Text(state.target.ifBlank { "Цель пока не выбрана" }, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Pity ${state.pity} / ${state.pityLimit} · ${if (state.guarantee) "Гарант активен" else "50/50"}", color = Color(0xFFE8DDF3))
                }
            }
        }
        item {
            SectionTitle("Трекер круток")
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Запас круток", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${state.pulls}", fontSize = 32.sp, fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Pity", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${state.pity}/${state.pityLimit}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                    }
                    androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { update(state.copy(pulls = (state.pulls - 1).coerceAtLeast(0))) }, modifier = Modifier.weight(1f)) { Text("− 1 запас") }
                        Button(onClick = { update(state.copy(pulls = (state.pulls + 1).coerceAtMost(99999))) }, modifier = Modifier.weight(1f)) { Text("+ 1 запас") }
                    }
                    Button(onClick = {
                        if (state.pulls > 0) update(state.copy(
                            pulls = state.pulls - 1,
                            pity = (state.pity + 1).coerceAtMost(state.pityLimit),
                            history = (listOf("Использована 1 крутка") + state.history).take(20)
                        ))
                        else toast(context, "Сначала добавь крутки в запас")
                    }, modifier = Modifier.fillMaxWidth(), enabled = state.pulls > 0) { Text("Потратить 1 крутку · +1 pity") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { update(state.copy(pity = (state.pity + 10).coerceAtMost(state.pityLimit))) }, modifier = Modifier.weight(1f)) { Text("+10 pity") }
                        OutlinedButton(onClick = { update(state.copy(pity = (state.pity + 1).coerceAtMost(state.pityLimit))) }, modifier = Modifier.weight(1f)) { Text("+1 pity") }
                    }
                    Text("До следующего S-ранга: $toNext · до целевого: $toTarget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionTitle("Баннер и гарантия")
            OutlinedTextField(state.target, { update(state.copy(target = it.take(80))) }, modifier = Modifier.fillMaxWidth(), label = { Text("Целевой агент / баннер") }, shape = RoundedCornerShape(16.dp), singleLine = true)
            OutlinedTextField(
                pityLimitInput,
                { value -> if (value.length <= 3 && value.all { it.isDigit() }) pityLimitInput = value },
                modifier = Modifier.fillMaxWidth(), label = { Text("Лимит pity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val limit = pityLimitInput.toIntOrNull()?.coerceIn(1, 200)
                    if (limit != null) update(state.copy(pityLimit = limit, pity = state.pity.coerceAtMost(limit)))
                    pityLimitInput = state.pityLimit.toString()
                }), shape = RoundedCornerShape(16.dp), singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { update(state.copy(pity = 0, guarantee = false, history = (listOf("Получен S-ранг · pity сброшен") + state.history).take(20))) }, modifier = Modifier.weight(1f)) { Text("Получил S") }
                OutlinedButton(onClick = { update(state.copy(pity = 0, guarantee = true, history = (listOf("Проигрыш 50/50 · гарант включён") + state.history).take(20))) }, modifier = Modifier.weight(1f)) { Text("Проиграл 50/50") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Гарант активен", Modifier.weight(1f))
                Switch(checked = state.guarantee, onCheckedChange = { update(state.copy(guarantee = it)) })
            }
        }
        item {
            SectionTitle("Твой отряд")
            OutlinedTextField(state.teamName, { update(state.copy(teamName = it.take(60))) }, modifier = Modifier.fillMaxWidth(), label = { Text("Название отряда") }, shape = RoundedCornerShape(16.dp), singleLine = true)
            state.team.take(3).forEachIndexed { i, name ->
                OutlinedTextField(name, { value ->
                    val team = state.team.toMutableList()
                    while (team.size <= i) team.add("")
                    team[i] = value.take(60)
                    update(state.copy(team = team.take(3)))
                }, modifier = Modifier.fillMaxWidth(), label = { Text("Агент ${i + 1}") }, shape = RoundedCornerShape(16.dp), singleLine = true)
            }
        }
        item {
            SectionTitle("Материалы")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newMaterial, { newMaterial = it }, modifier = Modifier.weight(1f), label = { Text("Добавить ресурс") }, shape = RoundedCornerShape(16.dp), singleLine = true)
                IconButton(onClick = { if (newMaterial.isNotBlank()) { update(state.copy(materials = (state.materials + newMaterial.trim()).distinct().take(30))); newMaterial = "" } }) { Icon(Icons.Default.Add, "Добавить") }
            }
            state.materials.forEach { material ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(material, Modifier.weight(1f))
                    IconButton(onClick = { update(state.copy(materials = state.materials - material)) }) { Icon(Icons.Default.Check, "Убрать") }
                }
            }
        }
        item {
            SectionTitle("История действий")
            if (state.history.isEmpty()) Text("История пока пуста.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            else state.history.take(10).forEach { line -> Text("• $line", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
            if (state.history.isNotEmpty()) TextButton(onClick = { update(state.copy(history = emptyList())) }) { Text("Очистить историю") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Экспорт копии") }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Импорт копии") }
            }
        }
        item { Text("Это личный трекер. Данные не считываются из игры; актуальные правила баннеров сверяй с самой игрой.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun VaultScreen(
    prefs: HubPrefs,
    importedEnvelope: String?,
    onImportHandled: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val context = LocalContext.current
    val storedEnvelope = prefs.vaultEnvelope()
    val envelope = importedEnvelope ?: storedEnvelope
    var master by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    LaunchedEffect(importedEnvelope) {
        if (importedEnvelope != null) {
            unlocked = false
            master = ""
        }
    }
    var entries by remember { mutableStateOf(emptyList<VaultEntry>()) }
    var error by remember { mutableStateOf("") }
    var showEntry by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var service by rememberSaveable { mutableStateOf("") }
    var login by rememberSaveable { mutableStateOf("") }
    var entryPassword by remember { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var generated by remember { mutableStateOf("") }
    var length by rememberSaveable { mutableStateOf(20f) }
    var lower by rememberSaveable { mutableStateOf(true) }
    var upper by rememberSaveable { mutableStateOf(true) }
    var digits by rememberSaveable { mutableStateOf(true) }
    var symbols by rememberSaveable { mutableStateOf(true) }
    var exclude by rememberSaveable { mutableStateOf(true) }
    var showGenerated by rememberSaveable { mutableStateOf(false) }

    fun decodeEntries(json: String): List<VaultEntry> {
        val a = JSONArray(json)
        require(a.length() <= 1000) { "Слишком много записей в хранилище." }
        return (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id", "")
            val site = o.optString("service", "")
            val pass = o.optString("password", "")
            if (id.isBlank() || site.isBlank() || pass.isBlank()) null
            else VaultEntry(id, site.take(100), o.optString("login", "").take(200), pass.take(2000), o.optString("note", "").take(500))
        }
    }
    fun encodeEntries(items: List<VaultEntry>): String {
        val a = JSONArray()
        items.forEach { a.put(JSONObject().put("id", it.id).put("service", it.service).put("login", it.login).put("password", it.password).put("note", it.note)) }
        return a.toString()
    }
    fun persist(next: List<VaultEntry>) {
        try {
            val encrypted = VaultCrypto.encrypt(master, encodeEntries(next))
            prefs.saveVaultEnvelope(encrypted)
            entries = next
            error = "Изменения зашифрованы и сохранены."
        } catch (e: Exception) { error = e.message ?: "Не удалось сохранить хранилище." }
    }

    if (envelope == null && !unlocked) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VaultHero()
            SectionTitle("Создать защищённое хранилище")
            Text("Мастер-пароль должен содержать минимум 12 символов. Он не сохраняется в открытом виде.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(master, { master = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Мастер-пароль") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), singleLine = true)
            OutlinedTextField(confirm, { confirm = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Повтори мастер-пароль") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), singleLine = true)
            Button(onClick = {
                if (master.length < 12) error = "Минимум 12 символов."
                else if (master != confirm) error = "Пароли не совпадают."
                else try { val enc = VaultCrypto.encrypt(master, "[]"); prefs.saveVaultEnvelope(enc); entries = emptyList(); unlocked = true; confirm = ""; error = "Хранилище создано." }
                catch (e: Exception) { error = e.message ?: "Не удалось создать хранилище." }
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(7.dp)); Text("Создать хранилище") }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    } else if (!unlocked) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VaultHero()
            SectionTitle(if (importedEnvelope != null) "Открыть резервную копию" else "Хранилище заблокировано")
            Text("Записи зашифрованы AES-256-GCM. Без мастер-пароля содержимое не открывается.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(master, { master = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Мастер-пароль") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), singleLine = true)
            Button(onClick = {
                val opened = envelope?.let { VaultCrypto.decrypt(master, it) }
                if (opened == null) error = "Не удалось открыть хранилище. Проверь мастер-пароль или целостность копии."
                else try {
                    entries = decodeEntries(opened)
                    if (importedEnvelope != null) { prefs.saveVaultEnvelope(importedEnvelope); onImportHandled() }
                    unlocked = true; error = "Хранилище разблокировано."
                } catch (_: Exception) { error = "Формат содержимого хранилища повреждён." }
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.LockOpen, null); Spacer(Modifier.width(7.dp)); Text("Разблокировать") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Импорт копии") }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f), enabled = storedEnvelope != null) { Text("Экспорт") }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Хранилище открыто", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("${entries.size} записей", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    IconButton(onClick = { unlocked = false; master = ""; entries = emptyList(); error = "" }) { Icon(Icons.Default.Lock, "Заблокировать") }
                }
            }
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Генератор паролей", fontWeight = FontWeight.Bold)
                        Text("Длина: ${length.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Slider(value = length, onValueChange = { length = it }, valueRange = 4f..64f, steps = 59)
                        GeneratorToggle("Строчные буквы", lower) { lower = it }
                        GeneratorToggle("Заглавные буквы", upper) { upper = it }
                        GeneratorToggle("Цифры", digits) { digits = it }
                        GeneratorToggle("Символы", symbols) { symbols = it }
                        GeneratorToggle("Исключать похожие символы", exclude) { exclude = it }
                        Button(onClick = { try { generated = PasswordGenerator.generate(length.toInt(), lower, upper, digits, symbols, exclude); error = "" } catch (e: Exception) { error = e.message ?: "Не удалось создать пароль" } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(7.dp)); Text("Сгенерировать") }
                        if (generated.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (showGenerated) generated else "•".repeat(generated.length), Modifier.weight(1f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                IconButton(onClick = { showGenerated = !showGenerated }) { Icon(if (showGenerated) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Показать") }
                                IconButton(onClick = { copySensitive(context, generated); toast(context, "Пароль скопирован") }) { Icon(Icons.Default.ContentCopy, "Копировать") }
                            }
                            OutlinedButton(onClick = { entryPassword = generated; showEntry = true; editingId = null }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить в хранилище") }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Мои записи", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(onClick = { editingId = null; service = ""; login = ""; entryPassword = ""; note = ""; showEntry = true }) { Icon(Icons.Default.Add, null); Text("Добавить") }
                }
                OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Поиск по сервису или логину") }, shape = RoundedCornerShape(16.dp), singleLine = true)
            }
            val filtered = entries.filter { (it.service + " " + it.login + " " + it.note).contains(search, ignoreCase = true) }.sortedBy { it.service.lowercase(Locale.ROOT) }
            if (filtered.isEmpty()) item { EmptyState("Нет сохранённых записей", "Добавь логин и пароль через кнопку выше.") }
            items(filtered, key = { it.id }) { entry ->
                var revealed by remember(entry.id) { mutableStateOf(false) }
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(entry.service, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (entry.login.isNotBlank()) Text(entry.login, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (revealed) entry.password else "•".repeat(minOf(16, entry.password.length)), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            IconButton(onClick = { revealed = !revealed }) { Icon(if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Показать пароль") }
                            IconButton(onClick = { copySensitive(context, entry.password); toast(context, "Пароль скопирован") }) { Icon(Icons.Default.ContentCopy, "Копировать") }
                        }
                        if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { editingId = entry.id; service = entry.service; login = entry.login; entryPassword = entry.password; note = entry.note; showEntry = true }) { Text("Изменить") }
                            TextButton(onClick = { persist(entries.filterNot { it.id == entry.id }) }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Экспорт копии") }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Импорт копии") }
                }
                Text("Резервная копия содержит только зашифрованное хранилище. Храни её в безопасном месте.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        }
    }

    if (showEntry && unlocked) {
        AlertDialog(
            onDismissRequest = { showEntry = false },
            title = { Text(if (editingId == null) "Новая запись" else "Изменить запись") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(service, { service = it }, label = { Text("Сервис / сайт") }, singleLine = true)
                    OutlinedTextField(login, { login = it }, label = { Text("Логин / почта") }, singleLine = true)
                    OutlinedTextField(entryPassword, { entryPassword = it }, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(note, { note = it }, label = { Text("Заметка") }, maxLines = 3)
                }
            },
            confirmButton = { TextButton(onClick = {
                if (service.isBlank() || entryPassword.isBlank()) error = "Укажи сервис и пароль."
                else {
                    val item = VaultEntry(editingId ?: System.currentTimeMillis().toString(), service.trim(), login.trim(), entryPassword, note.trim())
                    persist(if (editingId == null) entries + item else entries.map { if (it.id == editingId) item else it })
                    showEntry = false
                }
            }) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = { showEntry = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun VaultHero() {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("PRIVATE BY DESIGN", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Твои пароли под защитой", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("AES-256-GCM · мастер-пароль не сохраняется", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
            Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
        }
    }
}

@Composable
private fun GeneratorToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private enum class ControlSection { OVERVIEW, VOICE, MEMORY, API, PRIVACY, NOTIFICATIONS, DATA }

@Composable
private fun SettingsScreen(
    prefs: HubPrefs,
    onExportFairy: () -> Unit,
    onImportFairy: () -> Unit,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    auth: HubAuth?,
    user: FirebaseUser?,
    onOpenAuth: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    var section by rememberSaveable { mutableStateOf(ControlSection.OVERVIEW) }
    Column(Modifier.fillMaxSize()) {
        if (section != ControlSection.OVERVIEW) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { section = ControlSection.OVERVIEW }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад к Control Center")
                }
                Text(
                    when (section) {
                        ControlSection.VOICE -> "Озвучивание и голос"
                        ControlSection.MEMORY -> "Память FAIRY"
                        ControlSection.API -> "API Monitor"
                        ControlSection.PRIVACY -> "Приватность"
                        ControlSection.NOTIFICATIONS -> "Уведомления"
                        ControlSection.DATA -> "Данные и резервные копии"
                        else -> "Control Center"
                    }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge
                )
            }
        }
        when (section) {
            ControlSection.OVERVIEW -> LazyColumn(
                Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("PURPLE HUB · CONTROL CENTER", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Всё под контролем", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Голос, локальная память, API, приватность, напоминания и данные — в одном месте.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
                            Text("NATIVE EDITION · 5.6.0", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                        }
                    }
                }
                item { AccountSettingsCard(auth = auth, user = user, onOpenAuth = onOpenAuth, onSignOut = onSignOut) }
                item {
                    Card(shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Внешний вид", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Тёмная тема")
                                    Text("Фиолетовая палитра Purple Hub", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = dark, onCheckedChange = onDarkChange)
                            }
                        }
                    }
                }
                item { ControlCenterTile("Озвучивание и голос", "Голос устройства, скорость, тон и микрофон", Icons.Default.VolumeUp) { section = ControlSection.VOICE } }
                item { ControlCenterTile("Память FAIRY", "Просмотр, редактирование, импорт и экспорт", Icons.Default.AutoAwesome) { section = ControlSection.MEMORY } }
                item { ControlCenterTile("AI API и статистика", "Провайдер, модель, ключ, запросы и ошибки", Icons.Default.Refresh) { section = ControlSection.API } }
                item { ControlCenterTile("Приватность", "Контекст чата, передача памяти и веб-поиск", Icons.Default.Shield) { section = ControlSection.PRIVACY } }
                item { ControlCenterTile("Уведомления", "Разрешения Android и локальные напоминания", Icons.Default.Notifications) { section = ControlSection.NOTIFICATIONS } }
                item { ControlCenterTile("Данные", "Очистка истории и резервные копии", Icons.Default.Folder) { section = ControlSection.DATA } }
                item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Готово") } }
            }
            ControlSection.VOICE -> VoiceControlSection(prefs)
            ControlSection.MEMORY -> MemoryControlSection(prefs, onExportFairy, onImportFairy)
            ControlSection.API -> ApiMonitorSection(prefs)
            ControlSection.PRIVACY -> PrivacyControlSection(prefs)
            ControlSection.NOTIFICATIONS -> NotificationControlSection(prefs)
            ControlSection.DATA -> DataControlSection(prefs, onExportFairy, onImportFairy)
        }
    }
}

@Composable
private fun ControlCenterTile(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.MoreHoriz, contentDescription = "Открыть", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ControlSwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ControlCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
    }
}

@Composable
private fun VoiceControlSection(prefs: HubPrefs) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(prefs.loadVoiceSettings()) }
    var availableVoices by remember { mutableStateOf<List<android.speech.tts.Voice>>(emptyList()) }
    var voiceMenu by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    val micPermission = remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micPermission.value = it }
    DisposableEffect(context, settings.languageTag) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val language = Locale.forLanguageTag(settings.languageTag).language
                availableVoices = engine.voices?.filter { it.locale.language == language }?.sortedBy { it.name } ?: emptyList()
                engine.language = Locale.forLanguageTag(settings.languageTag)
                ttsEngine = engine
            }
        }
        onDispose { engine.stop(); engine.shutdown(); if (ttsEngine === engine) ttsEngine = null }
    }
    SideEffect {
        ttsEngine?.let { engine ->
            engine.setSpeechRate(settings.speechRate)
            engine.setPitch(settings.speechPitch)
            val selectedVoice = engine.voices?.firstOrNull { it.name == settings.voiceName }
            if (selectedVoice != null) engine.voice = selectedVoice
            else if (settings.voiceName.isBlank()) engine.defaultVoice?.let { engine.voice = it }
        }
    }
    fun update(next: VoiceSettings) { settings = next; prefs.saveVoiceSettings(next) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ControlCard {
                SectionTitle("Озвучивание")
                ControlSwitchRow("Озвучивание ответов", "Показывает кнопку динамика у ответов FAIRY.", settings.speechEnabled) { update(settings.copy(speechEnabled = it)) }
                ControlSwitchRow("Автоматически читать ответы", "FAIRY будет озвучивать новые ответы без нажатия.", settings.autoSpeak) { update(settings.copy(autoSpeak = it)) }
                ControlSwitchRow("Голосовой ввод", "Разрешает кнопку микрофона в чате.", settings.voiceInputEnabled) { update(settings.copy(voiceInputEnabled = it)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Язык распознавания и речи")
                        Text("Используется системная служба Android.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterTab("Русский", settings.languageTag == "ru-RU") { update(settings.copy(languageTag = "ru-RU", voiceName = "")) }
                    FilterTab("English", settings.languageTag == "en-US") { update(settings.copy(languageTag = "en-US", voiceName = "")) }
                }
            }
        }
        item {
            ControlCard {
                SectionTitle("Голос устройства")
                Text("Набор голосов зависит от установленного TTS-движка Android. Purple Hub использует выбранный голос только для локального озвучивания.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val selected = availableVoices.firstOrNull { it.name == settings.voiceName }
                OutlinedButton(onClick = { voiceMenu = true }, enabled = availableVoices.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text(selected?.let { "${it.name} · ${it.locale.displayName}" } ?: if (availableVoices.isEmpty()) "Голоса загружаются / недоступны" else "Голос по умолчанию")
                }
                DropdownMenu(expanded = voiceMenu, onDismissRequest = { voiceMenu = false }) {
                    DropdownMenuItem(text = { Text("Системный по умолчанию") }, onClick = { update(settings.copy(voiceName = "")); voiceMenu = false })
                    availableVoices.take(30).forEach { voice ->
                        DropdownMenuItem(text = { Text("${voice.name} · ${voice.locale.displayName}") }, onClick = {
                            update(settings.copy(voiceName = voice.name)); voiceMenu = false
                        })
                    }
                }
                Text("Скорость: ${String.format(Locale.ROOT, "%.1f", settings.speechRate)}×")
                Slider(value = settings.speechRate, onValueChange = { update(settings.copy(speechRate = it)) }, valueRange = 0.5f..2.0f, steps = 14)
                Text("Высота голоса: ${String.format(Locale.ROOT, "%.1f", settings.speechPitch)}×")
                Slider(value = settings.speechPitch, onValueChange = { update(settings.copy(speechPitch = it)) }, valueRange = 0.5f..2.0f, steps = 14)
                Button(onClick = {
                    if (settings.speechEnabled) ttsEngine?.speak("Проверка озвучивания Purple Hub. Всё работает.", TextToSpeech.QUEUE_FLUSH, null, "purple-hub-voice-test")
                    else toast(context, "Сначала включи озвучивание ответов")
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(7.dp)); Text("Проверить голос") }
            }
        }
        item {
            ControlCard {
                SectionTitle("Микрофон")
                Text(if (micPermission.value) "Разрешение микрофона выдано." else "Разрешение микрофона ещё не выдано.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Mic, null); Spacer(Modifier.width(7.dp)); Text(if (micPermission.value) "Проверить разрешение" else "Разрешить микрофон")
                }
            }
        }
    }
}

@Composable
private fun MemoryControlSection(prefs: HubPrefs, onExport: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    var memories by remember { mutableStateOf(prefs.loadMemories()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<FairyNote?>(null) }
    var editText by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ControlCard {
                SectionTitle("Локальная память")
                Text("Сейчас память хранится только на этом устройстве. По умолчанию она не включается в запросы AI. Передачу можно отдельно разрешить в разделе «Приватность».",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Записей: ${memories.size}/100", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(500) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Новая запись памяти") }, maxLines = 3)
                Button(onClick = {
                    if (draft.isNotBlank()) {
                        memories = (listOf(FairyNote(System.currentTimeMillis().toString(), draft.trim().take(500))) + memories).take(100)
                        prefs.saveMemories(memories); draft = ""; toast(context, "Запись памяти сохранена")
                    }
                }, enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Добавить в память") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Экспорт") }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Импорт") }
                }
            }
        }
        if (memories.isEmpty()) item { EmptyState("Память пуста", "Добавь запись, чтобы FAIRY могла хранить её локально.") }
        items(memories, key = { it.id }) { item ->
            Card(shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.text, Modifier.weight(1f).clickable { editing = item; editText = item.text })
                    IconButton(onClick = { editing = item; editText = item.text }) { Icon(Icons.Default.Edit, "Редактировать") }
                    IconButton(onClick = { memories = memories.filterNot { it.id == item.id }; prefs.saveMemories(memories) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (editing != null) AlertDialog(
        onDismissRequest = { editing = null },
        title = { Text("Изменить запись памяти") },
        text = { OutlinedTextField(value = editText, onValueChange = { editText = it.take(500) }, modifier = Modifier.fillMaxWidth(), minLines = 3) },
        confirmButton = { TextButton(onClick = {
            val current = editing
            if (current != null && editText.isNotBlank()) {
                memories = memories.map { if (it.id == current.id) it.copy(text = editText.trim()) else it }
                prefs.saveMemories(memories)
            }
            editing = null
        }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = { editing = null }) { Text("Отмена") } }
    )
}

@Composable
private fun ApiMonitorSection(prefs: HubPrefs) {
    val context = LocalContext.current
    val initial = remember { FairyApi.config(context) }
    var endpoint by rememberSaveable { mutableStateOf(initial.endpoint) }
    var model by rememberSaveable { mutableStateOf(initial.model) }
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var testMessage by remember { mutableStateOf("") }
    var testBusy by remember { mutableStateOf(false) }
    var providerMenu by remember { mutableStateOf(false) }
    var monitor by remember { mutableStateOf(prefs.loadApiMonitor()) }
    val current = FairyApi.config(context)
    val providers = listOf(
        "OpenAI" to "https://api.openai.com/v1",
        "Gemini · Google AI Studio (OpenAI-compatible)" to "https://generativelanguage.googleapis.com/v1beta/openai",
        "OpenRouter · каталог разных моделей" to "https://openrouter.ai/api/v1",
        "Groq" to "https://api.groq.com/openai/v1",
        "Qwen · DashScope International" to "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
        "DeepSeek" to "https://api.deepseek.com",
        "Mistral · OpenAI-compatible" to "https://api.mistral.ai/openai/v1"
    )
    val normalizedEndpoint = endpoint.trim().trimEnd('/')
    val savedEndpoint = current.endpoint.trim().trimEnd('/')
    val endpointChanged = normalizedEndpoint != savedEndpoint
    val modelChanged = model.trim() != current.model.trim()
    val canTest = current.hasKey && !endpointChanged && !modelChanged && model.isNotBlank()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ControlCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Статистика AI API")
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { monitor = prefs.loadApiMonitor() }) { Icon(Icons.Default.Refresh, "Обновить") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallStatCard("${monitor.total}", "Запросы", Modifier.weight(1f))
                    SmallStatCard("${monitor.failed}", "Ошибки", Modifier.weight(1f))
                    SmallStatCard("${monitor.rateLimited}", "HTTP 429", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallStatCard("${monitor.totalTokens}", "Токены", Modifier.weight(1f))
                    SmallStatCard("${monitor.lastLatencyMs} ms", "Последний", Modifier.weight(1f))
                    SmallStatCard("${monitor.averageLatencyMs} ms", "Среднее", Modifier.weight(1f))
                }
                Text("Теперь монитор учитывает реальные сетевые запросы FAIRY, ответы провайдера, ошибки и задержку. Для обновления нажми кнопку со стрелкой.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { monitor = prefs.loadApiMonitor() }, modifier = Modifier.fillMaxWidth()) { Text("Обновить статистику") }
            }
        }
        item {
            ControlCard {
                SectionTitle("Последние запросы")
                if (monitor.calls.isEmpty()) Text("Запросов пока нет. Сначала подключи провайдера и отправь FAIRY сообщение.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                monitor.calls.take(15).forEach { call ->
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${call.kind.uppercase(Locale.ROOT)} · ${if (call.success) "успешно" else if (call.rateLimited) "HTTP 429" else "ошибка"}", fontWeight = FontWeight.Medium,
                                color = if (call.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text(java.text.SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(call.at)),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (call.detail.isNotBlank()) Text(call.detail.take(100), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${call.latencyMs} ms", fontWeight = FontWeight.Bold)
                            Text("${call.tokens} ток.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            ControlCard {
                SectionTitle("Подключение FAIRY")
                Text(
                    "Подключай не только OpenAI: подойдут Gemini (Google AI Studio), OpenRouter, Groq, Qwen (DashScope), DeepSeek, Mistral и другие сервисы — при условии, что они поддерживают OpenAI-совместимый Chat Completions API и Bearer-авторизацию. Для подключения нужны адрес Base URL, точный ID модели и API-ключ из кабинета выбранного сервиса.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Важно: одного API-ключа недостаточно. Нужен совместимый формат запросов. Например, Anthropic Messages API напрямую не поддерживается; модели Anthropic можно подключить через OpenRouter или другой OpenAI-совместимый шлюз. У разных провайдеров свои тарифы, лимиты и списки моделей.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    OutlinedButton(onClick = { providerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Выбрать готовый адрес провайдера")
                    }
                    DropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                        providers.forEach { (name, url) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = {
                                endpoint = url
                                model = ""
                                apiKey = ""
                                providerMenu = false
                                testMessage = ""
                                message = "Адрес выбран. Введи ID модели и API-ключ именно этого провайдера, затем сохрани настройки."
                            })
                        }
                        DropdownMenuItem(text = { Text("Свой OpenAI-совместимый адрес") }, onClick = {
                            providerMenu = false
                            message = "Введи собственный HTTPS Base URL в поле ниже."
                        })
                    }
                }
                OutlinedTextField(
                    value = endpoint, onValueChange = { endpoint = it; message = ""; testMessage = "" },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Base URL · адрес API") }, singleLine = true,
                    placeholder = { Text("https://api.example.com/v1") }
                )
                Text("Указывай базовый HTTPS-адрес без /chat/completions. Для Qwen/DashScope, других регионов или собственного шлюза выбери подходящий региональный адрес и используй ключ от того же региона.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = model, onValueChange = { model = it; message = ""; testMessage = "" },
                    modifier = Modifier.fillMaxWidth(), label = { Text("ID модели") }, singleLine = true,
                    placeholder = { Text("Точное имя модели из кабинета провайдера") }
                )
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it; message = "" }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (current.hasKey && !endpointChanged) "Новый API-ключ (пусто — оставить сохранённый)" else "API-ключ выбранного провайдера") }, singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Показать") } }
                )
                if (endpointChanged && current.hasKey) {
                    Text("Адрес провайдера изменён. Для безопасности введи новый API-ключ: ключ от предыдущего адреса не будет перенесён автоматически.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = {
                    val ok = FairyApi.saveConfig(context, endpoint, model, apiKey)
                    message = if (ok) {
                        apiKey = ""
                        testMessage = ""
                        "Настройки сохранены. API-ключ зашифрован на устройстве."
                    } else when {
                        endpointChanged && current.hasKey && apiKey.isBlank() -> "После смены Base URL введи API-ключ нового провайдера."
                        !current.hasKey && apiKey.isBlank() -> "Введи API-ключ провайдера."
                        model.isBlank() -> "Укажи точный ID модели."
                        else -> "Не удалось сохранить. Проверь HTTPS Base URL, модель и длину ключа (до 512 символов)."
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить настройки API") }
                OutlinedButton(
                    onClick = {
                        testBusy = true
                        testMessage = "Отправляю короткий тестовый запрос. Провайдер может списать за него небольшое количество токенов."
                        FairyApi.send(context, listOf(ChatMessage("user", "Ответь ровно одним словом: OK."))) { result ->
                            testBusy = false
                            monitor = prefs.loadApiMonitor()
                            testMessage = if (result.error.isNotBlank()) "Проверка не пройдена: ${result.error}"
                                else "Подключение работает. Ответ модели: ${result.answer.take(120)}"
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = canTest && !testBusy
                ) {
                    if (testBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Проверить подключение")
                }
                if (!canTest) Text("Чтобы проверить соединение, сначала сохрани текущий Base URL, ID модели и API-ключ.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (current.hasKey) OutlinedButton(onClick = {
                    FairyApi.clearKey(context)
                    apiKey = ""
                    message = "Сохранённый API-ключ удалён с устройства."
                    testMessage = ""
                }, modifier = Modifier.fillMaxWidth()) { Text("Удалить сохранённый API-ключ") }
                if (message.isNotBlank()) Text(message, color = if (message.startsWith("Не удалось") || message.startsWith("После смены") || message.startsWith("Введи") || message.startsWith("Укажи")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                if (testMessage.isNotBlank()) Text(testMessage, color = if (testMessage.startsWith("Проверка не пройдена")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    when {
                        endpointChanged && current.hasKey -> "Сохранённый ключ принадлежит прежнему Base URL — введи ключ нового сервиса."
                        current.hasKey -> "API-ключ сохранён и не отображается повторно."
                        else -> "API-ключ ещё не настроен."
                    }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Text("Возможности и ограничения", fontWeight = FontWeight.Bold)
                Text("• Vision доступен только тогда, когда выбранная модель и провайдер поддерживают изображения в формате image_url. Это нужно проверить в документации конкретной модели.\n• Встроенный веб-поиск пока работает через отдельный OpenAI Responses API и требует официальный OpenAI Base URL; обычный чат при этом может использовать Gemini, OpenRouter, Qwen и другие совместимые сервисы.\n• Используй только HTTPS и API-ключ, выданный выбранным провайдером. Ключ шифруется на устройстве Android Keystore и не включается в резервные копии.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SmallStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun PrivacyControlSection(prefs: HubPrefs) {
    var settings by remember { mutableStateOf(prefs.loadPrivacySettings()) }
    fun update(next: PrivacySettings) { settings = next; prefs.savePrivacySettings(next) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ControlCard {
                SectionTitle("Что отправляется в AI")
                Text("Сообщение, которое ты отправляешь в FAIRY, передаётся выбранному API-провайдеру для получения ответа. Локальная память и заметки по умолчанию не отправляются.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ControlSwitchRow("Контекст предыдущего чата", "Включать предыдущие сообщения в обычные запросы. Текущее сообщение отправляется в любом случае.", settings.includeChatHistory) {
                    update(settings.copy(includeChatHistory = it))
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ControlSwitchRow("Разрешить передачу памяти FAIRY", "При включении до 10 локальных записей памяти добавляются в системный контекст обычного AI-диалога, но не веб-поиска.", settings.shareMemories) {
                    update(settings.copy(shareMemories = it))
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ControlSwitchRow("Разрешить веб-поиск", "При включении поисковый запрос отправляется через отдельный API веб-поиска.", settings.allowWebSearch) {
                    update(settings.copy(allowWebSearch = it))
                }
            }
        }
        item {
            ControlCard {
                SectionTitle("Что остаётся локально")
                Text("Пароли Password Vault, локальные заметки, память (пока переключатель передачи выключен), напоминания и данные ZZZ Companion не синхронизируются с Firebase автоматически. Firebase используется для входа в аккаунт, а не для резервного копирования этих данных.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("API-ключ не включается в резервные копии. Не отправляй его сообщениями в чат.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun NotificationControlSection(prefs: HubPrefs) {
    val context = LocalContext.current
    var notificationGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var exactGranted by remember { mutableStateOf(ReminderScheduler.hasExactAlarmAccess(context)) }
    var reminders by remember { mutableStateOf(prefs.loadReminders()) }
    var reminderText by rememberSaveable { mutableStateOf("") }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ControlCard {
                SectionTitle("Разрешения Android")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Уведомления")
                        Text(when {
                            !notificationGranted -> "Разрешение Android не выдано"
                            !notificationsEnabled -> "Уведомления отключены в настройках приложения"
                            else -> "Уведомления разрешены"
                        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (Build.VERSION.SDK_INT >= 33 && !notificationGranted) Button(onClick = { notificationLauncher.launch(POST_NOTIFICATIONS) }) { Text("Разрешить") }
                    else if (!notificationsEnabled) OutlinedButton(onClick = {
                        try { context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)) }
                        catch (_: Exception) { toast(context, "Не удалось открыть настройки уведомлений") }
                    }) { Text("Открыть настройки") }
                    else Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Точные будильники")
                        Text(if (exactGranted) "Точное расписание доступно" else "Будут использоваться приблизительные напоминания", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactGranted) {
                        OutlinedButton(onClick = {
                            try {
                                context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (_: Exception) { toast(context, "Не удалось открыть настройки будильников") }
                        }) { Text("Настроить") }
                    } else Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
                OutlinedButton(onClick = {
                    notificationGranted = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                    exactGranted = ReminderScheduler.hasExactAlarmAccess(context)
                }, modifier = Modifier.fillMaxWidth()) { Text("Обновить статус разрешений") }
            }
        }
        item {
            ControlCard {
                SectionTitle("Локальные напоминания")
                Text("Напоминания хранятся на устройстве. Точность зависит от системного разрешения на точные будильники и режима энергосбережения.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = reminderText, onValueChange = { reminderText = it.take(120) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Текст напоминания") }, singleLine = true)
                Button(onClick = {
                    if (!notificationGranted || !notificationsEnabled) { toast(context, "Сначала включи уведомления в разрешениях Android"); return@Button }
                    val calendar = Calendar.getInstance().apply { add(Calendar.MINUTE, 15) }
                    DatePickerDialog(context, { _, year, month, day ->
                        calendar.set(Calendar.YEAR, year); calendar.set(Calendar.MONTH, month); calendar.set(Calendar.DAY_OF_MONTH, day)
                        TimePickerDialog(context, { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                                toast(context, "Выбери время в будущем")
                            } else {
                                val reminder = HubReminder(System.currentTimeMillis(), reminderText.trim().ifBlank { "Напоминание Purple Hub" }, calendar.timeInMillis)
                                val isExact = ReminderScheduler.schedule(context, reminder)
                                reminders = (reminders + reminder).sortedBy { it.at }.take(100)
                                prefs.saveReminders(reminders)
                                reminderText = ""
                                toast(context, if (isExact) "Точное напоминание создано" else "Напоминание создано, но без точного режима")
                            }
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text("Создать напоминание через дату и время") }
                Text("${reminders.size} запланировано", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (reminders.isEmpty()) item { EmptyState("Напоминаний пока нет", "Создай локальное напоминание выше.") }
        items(reminders, key = { it.id }) { reminder ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(reminder.title, fontWeight = FontWeight.Medium)
                        Text(java.text.SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.getDefault()).format(Date(reminder.at)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        ReminderScheduler.cancel(context, reminder)
                        reminders = reminders.filterNot { it.id == reminder.id }; prefs.saveReminders(reminders)
                    }) { Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun DataControlSection(prefs: HubPrefs, onExport: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    var confirmAction by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ControlCard {
                SectionTitle("Резервная копия FAIRY")
                Text("Экспорт включает историю чата, заметки, локальную память и напоминания. API-ключи и пароли Password Vault не экспортируются. Данные ZZZ Companion имеют отдельные кнопки импорта и экспорта в своём разделе.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Важно: JSON-резервная копия FAIRY не зашифрована. Храни её в приватном месте и не отправляй посторонним.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Создать резервную копию") }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Восстановить из копии") }
            }
        }
        item {
            ControlCard {
                SectionTitle("Очистка данных")
                Text("Удаление необратимо. Перед очисткой важных записей сначала создай резервную копию.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { confirmAction = "history" }, modifier = Modifier.fillMaxWidth()) { Text("Очистить историю чата") }
                OutlinedButton(onClick = { confirmAction = "memory" }, modifier = Modifier.fillMaxWidth()) { Text("Очистить заметки и память") }
                OutlinedButton(onClick = { confirmAction = "reminders" }, modifier = Modifier.fillMaxWidth()) { Text("Удалить все напоминания") }
                OutlinedButton(onClick = { confirmAction = "stats" }, modifier = Modifier.fillMaxWidth()) { Text("Сбросить статистику API") }
            }
        }
    }
    if (confirmAction.isNotBlank()) {
        val title = when (confirmAction) {
            "history" -> "Очистить историю чата?"
            "memory" -> "Удалить заметки и память?"
            "reminders" -> "Удалить все напоминания?"
            else -> "Сбросить статистику API?"
        }
        AlertDialog(
            onDismissRequest = { confirmAction = "" },
            title = { Text(title) },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = { TextButton(onClick = {
                when (confirmAction) {
                    "history" -> prefs.clearChatHistory()
                    "memory" -> prefs.clearFairyNotesAndMemories()
                    "stats" -> prefs.clearApiMonitor()
                    "reminders" -> { prefs.loadReminders().forEach { ReminderScheduler.cancel(context, it) }; prefs.saveReminders(emptyList()) }
                }
                confirmAction = ""
                toast(context, "Готово")
            }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = "" }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyState(title: String, description: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun copySensitive(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Password", value)
    if (android.os.Build.VERSION.SDK_INT >= 24) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    clipboard.setPrimaryClip(clip)
}

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
