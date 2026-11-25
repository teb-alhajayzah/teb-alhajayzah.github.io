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
    val adminOwnerName: String = "المدير العام",
    val adminOwnerNameEn: String = "General Manager",
    val adminUsername: String = "admin",
    val adminPassword: String = "teb2026",
    val adminPin: String = "132027",
    val globalChatEnabled: Boolean = true,
    // إعدادات الربط السحابي الافتراضية (Firebase Web Config + Cloudflare Worker URL) معزولة تحت مسار teb_alhajayzah
    val firebaseApiKey: String = "AIzaSyC08cH9E9le52NU-B56N894yt_k8hQMQ1M",
    val firebaseAuthDomain: String = "gen-lang-client-0514993881.firebaseapp.com",
    val firebaseProjectId: String = "gen-lang-client-0514993881",
    val firebaseStorageBucket: String = "gen-lang-client-0514993881.firebasestorage.app",
    val firebaseMessagingSenderId: String = "197836130786",
    val firebaseAppId: String = "1:197836130786:web:3a85d5a30dd81184c411bb",
    val firebaseVapidKey: String = "BPH0zDDVmb1w6XZ29F_xS4D31Z6U_L1mmg9qdRCsbPpbqtIoxbchNVN5djCHMcx783SJbunqzeQgcM2O6X2A",
    val workerUrl: String = "https://teb-alhajayzah.masarik-tourism.workers.dev",
    // إعدادات التخزين السحابي الاحتياطي على Google Drive وسياسة الاحتفاظ بالملفات الثقيلة (أيام: 90 / 180 / 360)
    val defaultDriveWebAppUrl: String = "",
    val fileRetentionDays: Int = 360,
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
    const val FIRESTORE_DATABASE_ID = "(default)"
    const val ROOT = "teb_alhajayzah"
    const val SETTINGS_DOC = "settings"
    const val SETTINGS = "teb_alhajayzah_settings"
    const val DATA_ROOT = "teb_alhajayzah"
    const val USERS = "teb_alhajayzah_accounts"
    const val FAMILY_MEMBERS = "teb_alhajayzah_family_members"
    const val CLINICS = "teb_alhajayzah_clinics"
    const val LABS = "teb_alhajayzah_labs"
    const val APPOINTMENTS = "teb_alhajayzah_appointments"
    const val RECORDS = "teb_alhajayzah_records"
    const val PERMANENT_LOGS = "teb_alhajayzah_permanent_logs"
    const val MESSAGES = "teb_alhajayzah_messages"
    const val NOTIFICATIONS = "teb_alhajayzah_notifications"
    const val BLOCKED_PHONES = "teb_alhajayzah_blocked_phones"
    const val ACTIVATION_CODES = "teb_alhajayzah_activation_codes"
    const val FAMILY_LINK_REQUESTS = "teb_alhajayzah_family_link_requests"
    const val VISITOR_HITS = "teb_alhajayzah_visitor_hits"
    const val FCM_TOKENS = "teb_alhajayzah_fcm_tokens"
    const val DATA_FCM_TOKENS = FCM_TOKENS
    const val CONFIG = SETTINGS
}

object AppConfig {
    val defaultBrand = PlatformBrandConfig()
}
