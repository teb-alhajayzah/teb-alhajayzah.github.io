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
    val hospitalNameAr: String = "مستشفى الحجايزة التخصصي",
    val taglineAr: String = "منصتك الطبية لحجز العيادات والملف الصحي الموحد",
    val homeAnnouncementAr: String = "",
    val supportPhone: String = "01125002428",
    val currencyLabelAr: String = "ج.م",
    val defaultSlotMinutes: Int = 20,
    val appVersionLabel: String = "v1.0.0",
    // رابط التحميل المباشر لأحدث ملف APK من GitHub Releases
    val apkDownloadUrl: String = "https://github.com/teb-alhajayzah/teb-alhajayzah.github.io/releases/latest/download/teb-alhajayzah.apk",
    // رابط واجهة الأدوية المصرية المفتوحة (workers.dev)
    val medicationsApiUrl: String = "",
    val adminOwnerName: String = "المهندس محمد يوسف",
    val adminUsername: String = "admin",
    val adminPassword: String = "teb2026",
    val adminPin: String = "202600",
    @param:DrawableRes val logoRes: Int = R.drawable.app_icon_fg,
    @param:DrawableRes val heroBannerRes: Int = R.drawable.img_hospital_banner,
    @param:DrawableRes val medicalRecordIllustRes: Int = R.drawable.img_medical_record,
)

object AppConfig {
    val defaultBrand = PlatformBrandConfig()
}
