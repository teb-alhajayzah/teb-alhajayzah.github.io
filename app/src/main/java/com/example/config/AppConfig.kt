package com.example.config

import androidx.annotation.DrawableRes
import com.example.R

/**
 * ملف الإعدادات الموحد للمنصة (Unified Platform Config)
 * يمكن تعديله مباشرة من لوحة تحكم الإدارة العليا (Super Admin) بدون كود.
 */
data class PlatformBrandConfig(
    val platformNameAr: String = "طب الحجايزة",
    val platformNameEn: String = "Teb Al-Hajayzah",
    val villageNameAr: String = "قرية الحجايزة والقرى المجاورة",
    val villageNameEn: String = "Al-Hajayzah Village & Surrounding Areas",
    val hospitalNameAr: String = "مستشفى الحجايزة التخصصي",
    val hospitalNameEn: String = "Al-Hajayzah Specialized Hospital",
    val taglineAr: String = "منصتك الطبية لحجز العيادات والملف الصحي الموحد",
    val taglineEn: String = "Your medical platform for clinic booking & unified EMR",
    val homeAnnouncementAr: String = "",
    val supportPhone: String = "01125002428",
    val otpWhatsappPhone: String = "01141271027",
    val currencyLabelAr: String = "ج.م",
    val currencyLabelEn: String = "EGP",
    val defaultSlotMinutes: Int = 20,
    val appVersionLabel: String = "v1.0.0",
    // رابط المنصة الحية المباشر للتحديث الفوري
    val liveWebUrl: String = "https://teb-alhajayzah.github.io/",
    // رابط التحميل المباشر لأحدث ملف APK من GitHub Releases
    val apkDownloadUrl: String = "https://github.com/teb-alhajayzah/teb-alhajayzah.github.io/releases/latest/download/teb-alhajayzah.apk",
    // رابط واجهة الأدوية المصرية المفتوحة (workers.dev)
    val medicationsApiUrl: String = "",
    val adminOwnerName: String = "المهندس محمد يوسف",
    val adminOwnerNameEn: String = "Eng. Mohamed Youssef",
    val adminUsername: String = "admin",
    val adminPassword: String = "teb2026",
    val adminPin: String = "202600",
    val globalChatEnabled: Boolean = true,
    // إعدادات الربط السحابي الافتراضية (Firebase Web Config + Cloudflare Worker URL) معزولة تحت مسار teb_alhajayzah
    val firebaseApiKey: String = "AIzaSyB0qr3K8jf1LgKog8b5knh1W4AClpw5iIs",
    val firebaseAuthDomain: String = "gen-lang-client-0015800098.firebaseapp.com",
    val firebaseProjectId: String = "gen-lang-client-0015800098",
    val firebaseStorageBucket: String = "gen-lang-client-0015800098.firebasestorage.app",
    val firebaseMessagingSenderId: String = "588431549120",
    val firebaseAppId: String = "1:588431549120:web:c10278b3df39d11a668325",
    val firebaseVapidKey: String = "BNjwnHCGNHMZt2rSbDbkIsa2NCI96ivhJSGYNA9MgbMehoQNnyP2MyUmqrDav2MUfg4YPMeeiOyIqQTE7MfA6kiw",
    val workerUrl: String = "https://masarik-otp.masarik-tourism.workers.dev",
    @param:DrawableRes val logoRes: Int = R.drawable.app_icon_fg,
    @param:DrawableRes val heroBannerRes: Int = R.drawable.img_hospital_banner,
    @param:DrawableRes val medicalRecordIllustRes: Int = R.drawable.img_medical_record,
) {
    val isCloudConfigured: Boolean
        get() = firebaseApiKey.isNotBlank() && firebaseProjectId.isNotBlank()
}

/**
 * مسارات عزل البيانات السحابية في Firebase Firestore & FCM
 * تضمن عدم تداخل أي بيانات مع مشاريع أخرى مستضافة على نفس الحساب السحابي.
 */
object CloudNamespacePaths {
    const val ROOT = "teb_alhajayzah"
    const val SETTINGS = "$ROOT/settings"
    const val DATA_ROOT = "$ROOT/data"
    const val USERS = "$DATA_ROOT/accounts"
    const val FAMILY_MEMBERS = "$DATA_ROOT/family_members"
    const val CLINICS = "$DATA_ROOT/doctors"
    const val LABS = "$DATA_ROOT/labs"
    const val APPOINTMENTS = "$DATA_ROOT/slots"
    const val RECORDS = "$DATA_ROOT/records"
    const val MESSAGES = "$DATA_ROOT/messages"
    const val NOTIFICATIONS = "$DATA_ROOT/notifications"
    const val BLOCKED_PHONES = "$DATA_ROOT/blocked_phones"
    const val ACTIVATION_CODES = "$DATA_ROOT/activation_codes"
    const val CONFIG = SETTINGS
}

object AppConfig {
    val defaultBrand = PlatformBrandConfig()
}
