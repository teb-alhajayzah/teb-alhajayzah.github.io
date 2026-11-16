package com.example.auth

import java.security.SecureRandom

/**
 * واجهة قياسية للتحقق عبر رسائل الهاتف (متوافقة مع Firebase Phone Authentication).
 * حالياً مفعّل وضع الاختبار الفوري (Test OTP) لتجربة الدورة الكاملة قبل الربط الفعلي.
 */
data class OtpChallenge(
    val verificationId: String,
    val formattedE164Phone: String,
    val isTestMode: Boolean,
    val testOtpCode: String?,
    val expiresAtMillis: Long,
)

interface PhoneOtpAuthProvider {
    fun formatEgyptianE164(rawPhone: String): String
    fun sendVerificationCode(rawPhone: String): Result<OtpChallenge>
    fun verifyCode(challenge: OtpChallenge, enteredCode: String): Result<Unit>
}

/**
 * مزوّد اختبار فوري يحاكي تدفق PhoneAuthProvider.verifyPhoneNumber + PhoneAuthProvider.getCredential
 * ويُظهر الرمز المكون من 6 أرقام للمستخدم في إشعار تجريبي على الشاشة.
 */
class FirebaseReadyTestOtpProvider : PhoneOtpAuthProvider {

    private val random = SecureRandom()

    override fun formatEgyptianE164(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("20") && digits.length >= 12 -> "+$digits"
            digits.startsWith("01") && digits.length == 11 -> "+2$digits"
            digits.startsWith("1") && digits.length == 10 -> "+20$digits"
            digits.isNotEmpty() -> "+2$digits"
            else -> ""
        }
    }

    override fun sendVerificationCode(rawPhone: String): Result<OtpChallenge> {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.length < 10) {
            return Result.failure(IllegalArgumentException("يرجى إدخال رقم هاتف صحيح مكون من 11 رقماً"))
        }
        val codeInt = 100000 + random.nextInt(900000)
        val challenge = OtpChallenge(
            verificationId = "fb_test_vid_${System.currentTimeMillis()}",
            formattedE164Phone = formatEgyptianE164(rawPhone),
            isTestMode = true,
            testOtpCode = codeInt.toString(),
            expiresAtMillis = System.currentTimeMillis() + 5 * 60_000L,
        )
        return Result.success(challenge)
    }

    override fun verifyCode(challenge: OtpChallenge, enteredCode: String): Result<Unit> {
        val clean = enteredCode.filter { it.isDigit() }
        if (clean.length != 6) {
            return Result.failure(IllegalArgumentException("يرجى إدخال رمز التحقق المكون من 6 أرقام"))
        }
        if (System.currentTimeMillis() > challenge.expiresAtMillis) {
            return Result.failure(IllegalArgumentException("انتهت صلاحية الرمز، اضغط إعادة إرسال"))
        }
        if (clean != challenge.testOtpCode) {
            return Result.failure(IllegalArgumentException("رمز التحقق غير صحيح، تأكد من الأرقام الستة"))
        }
        return Result.success(Unit)
    }
}
