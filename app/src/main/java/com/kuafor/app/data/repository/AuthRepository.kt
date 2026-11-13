package com.kuafor.app.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Kullanıcıya gösterilebilir (Türkçe) mesajı taşıyan kimlik doğrulama hatası. */
class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Telefon + SMS OTP ile kimlik doğrulama. Firebase Auth'a erişimin tek geçtiği yer
 * (ViewModel'ler FirebaseAuth'u doğrudan kullanmaz).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {

    private val _currentUid = MutableStateFlow(auth.currentUser?.uid)

    /** Oturum açık değilse null. Firebase'in oturum durumu değiştikçe güncellenir. */
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUid.value = firebaseAuth.currentUser?.uid
        }
    }

    /**
     * Telefona SMS gönderir ve doğrulama ID'sini döndürür.
     *
     * Cihaz SMS'i otomatik okuyup doğrulamayı kendisi tamamlarsa (instant verification)
     * oturum açılır ve [AUTO_VERIFIED_ID] döner; bu durumda kod adımı gerekmez.
     *
     * @param phoneE164 "+905xxxxxxxxx" biçiminde telefon numarası.
     * @param activity reCAPTCHA / doğrulama akışı için gerekli Activity.
     */
    suspend fun sendOtp(phoneE164: String, activity: Activity): Result<String> =
        try {
            Result.success(requestVerification(phoneE164, activity))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AuthException(mapSendError(e), e))
        }

    /** Kullanıcının SMS'te gördüğü kodla oturum açar. */
    suspend fun verifyOtp(verificationId: String, code: String): Result<Unit> =
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AuthException(mapVerifyError(e), e))
        }

    fun signOut() {
        auth.signOut()
    }

    private suspend fun requestVerification(phoneE164: String, activity: Activity): String =
        suspendCancellableCoroutine { cont ->
            var finished = false

            fun finish(verificationId: String?, error: Throwable?) {
                if (finished || !cont.isActive) return
                finished = true
                if (error != null) cont.resumeWith(Result.failure(error))
                else cont.resumeWith(Result.success(verificationId ?: AUTO_VERIFIED_ID))
            }

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Otomatik doğrulama: kodu beklemeden oturum aç.
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) finish(AUTO_VERIFIED_ID, null)
                        else finish(null, task.exception ?: AuthException(DEFAULT_ERROR))
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    finish(null, e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    // Yeniden gönderme için token burada saklanmıyor (bkz. LoginScreen TODO).
                    finish(verificationId, null)
                }
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneE164)
                .setTimeout(SMS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }

    private fun mapSendError(e: Exception): String = when (e) {
        is FirebaseNetworkException -> "İnternet bağlantınızı kontrol edip tekrar deneyin."
        is FirebaseTooManyRequestsException ->
            "Çok fazla deneme yapıldı. Lütfen bir süre sonra tekrar deneyin."
        is FirebaseAuthException ->
            if (e.errorCode == ERROR_INVALID_PHONE_NUMBER) "Telefon numarası geçersiz."
            else DEFAULT_ERROR
        else -> DEFAULT_ERROR
    }

    private fun mapVerifyError(e: Exception): String = when (e) {
        is FirebaseNetworkException -> "İnternet bağlantınızı kontrol edip tekrar deneyin."
        is FirebaseAuthInvalidCredentialsException -> "Doğrulama kodu hatalı."
        is FirebaseTooManyRequestsException ->
            "Çok fazla deneme yapıldı. Lütfen bir süre sonra tekrar deneyin."
        is FirebaseAuthException -> when (e.errorCode) {
            ERROR_SESSION_EXPIRED -> "Doğrulama kodunun süresi doldu. Yeni kod isteyin."
            else -> DEFAULT_ERROR
        }
        else -> DEFAULT_ERROR
    }

    companion object {
        /** [sendOtp] sonucu: SMS otomatik doğrulandı, kod adımı gerekmez. */
        const val AUTO_VERIFIED_ID = "auto-verified"

        private const val SMS_TIMEOUT_SECONDS = 60L
        private const val DEFAULT_ERROR = "Giriş yapılamadı, lütfen tekrar deneyin."
        private const val ERROR_INVALID_PHONE_NUMBER = "ERROR_INVALID_PHONE_NUMBER"
        private const val ERROR_SESSION_EXPIRED = "ERROR_SESSION_EXPIRED"
    }
}
