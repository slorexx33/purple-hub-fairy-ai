package com.purplehub.app

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Firebase Authentication adapter. The app stays fully usable offline when Firebase is not configured. */
internal class HubAuth private constructor(val firebaseAuth: FirebaseAuth) {
    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser

    companion object {
        fun create(context: Context): HubAuth? {
            return try {
                val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
                HubAuth(FirebaseAuth.getInstance(app))
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun register(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("Firebase не вернул созданный аккаунт.")
        user.sendEmailVerification().await()
        return user
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        return firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: throw IllegalStateException("Firebase не вернул пользователя.")
    }

    suspend fun sendPasswordReset(email: String) {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun resendVerification() {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("Сначала войди в аккаунт.")
        user.sendEmailVerification().await()
    }

    suspend fun refreshCurrentUser(): FirebaseUser? {
        val user = firebaseAuth.currentUser ?: return null
        user.reload().await()
        return firebaseAuth.currentUser
    }

    suspend fun signInWithGoogle(activity: Activity): FirebaseUser {
        val resourceId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (resourceId == 0) throw IllegalStateException("Не найден Google Web Client ID. Добавь app/google-services.json из Firebase Console и пересобери проект.")
        val webClientId = activity.getString(resourceId).trim()
        if (webClientId.isBlank()) throw IllegalStateException("В Firebase-конфигурации отсутствует Google Web Client ID.")

        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(activity).getCredential(activity, request)
        val credential = response.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Google вернул неподдерживаемый тип учётных данных.")
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        return firebaseAuth.signInWithCredential(firebaseCredential).await().user
            ?: throw IllegalStateException("Firebase не вернул пользователя Google.")
    }

    fun signOut(context: Context) {
        firebaseAuth.signOut()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try { CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest()) }
            catch (_: Exception) { /* Firebase sign-out still succeeds if the provider cannot be cleared. */ }
        }
    }
}

internal fun authErrorMessage(error: Throwable): String {
    val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
    val details = generateSequence(error) { it.cause }.joinToString(" ") { it.message.orEmpty() }
    if (code.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
        details.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
        return "Firebase Authentication не настроен для этого проекта (CONFIGURATION_NOT_FOUND). Это не ошибка AI API: открой Purple Hub без аккаунта и настрой AI API отдельно. Для входа проверь Firebase Console и включённые способы авторизации."
    }
    return when (code) {
        "ERROR_INVALID_EMAIL" -> "Проверь адрес электронной почты."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Не удалось войти. Проверь почту и пароль."
        "ERROR_USER_NOT_FOUND" -> "Не удалось войти. Проверь почту и пароль."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Этот адрес уже зарегистрирован. Попробуй войти."
        "ERROR_WEAK_PASSWORD" -> "Пароль слишком простой. Используй не менее 8 символов."
        "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток. Сделай паузу и попробуй позже."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Нет соединения. Проверь интернет и повтори попытку."
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "Этот адрес уже связан с другим способом входа. Войди прежним способом."
        else -> error.message?.takeIf { it.isNotBlank() }?.take(220) ?: "Не удалось выполнить вход. Попробуй ещё раз."
    }
}
