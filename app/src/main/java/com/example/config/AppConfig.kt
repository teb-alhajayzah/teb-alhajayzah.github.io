package com.example.config

import androidx.annotation.DrawableRes
import com.example.R

/**
 * ملف الإعدادات الموحد للمنصة (Unified Platform Config)
 * يمكن تعديل اسم المنصة، اسم القرية/المركز، المستشفى التخصصي، رابط تحميل التطبيق،
 * ورابط قاعدة بيانات الأدوية المصرية بسهولة دون تعديل منطق التطبيق.
 */
data class PlatformBrandConfig(
    val platformNameAr: String = "طب الحجايزة",
    val platformNameEn: String = "Teb Al-Hajayzah",
    val villageNameAr: String = "قرية الحجايزة والقرى المجاورة",
    val hospitalNameAr: String = "مستشفى الحجايزة التخصصي",
    val taglineAr: String = "منصتك الطبية لحجز العيادات والملف الصحي الموحد",
    val supportPhone: String = "0100-555-0199",
    val currencyLabelAr: String = "ج.م",
    val defaultSlotMinutes: Int = 20,
    val appVersionLabel: String = "v1.0.0",
    // رابط التحميل المباشر لأحدث ملف APK من GitHub Releases
    val apkDownloadUrl: String = "https://github.com/teb-alhajayzah/teb-alhajayzah.github.io/releases/latest/download/app-release.apk",
    // رابط واجهة الأدوية المصرية المفتوحة (workers.dev) — وعند تركه فارغاً أو عدم توفر الإنترنت يعمل الكتالوج المدمج تلقائياً
    val medicationsApiUrl: String = "",
    @DrawableRes val logoRes: Int = R.drawable.app_icon_fg,
    @DrawableRes val heroBannerRes: Int = R.drawable.img_hospital_banner,
    @DrawableRes val medicalRecordIllustRes: Int = R.drawable.img_medical_record,
)

object AppConfig {
    val defaultBrand = PlatformBrandConfig()
}
