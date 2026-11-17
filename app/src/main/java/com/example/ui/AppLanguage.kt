package com.example.ui

enum class AppLanguage(val code: String, val displayLabel: String) {
    AR("AR", "العربية (AR)"),
    EN("EN", "English (EN)");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            if (code?.equals("EN", ignoreCase = true) == true) EN else AR
    }
}

fun bi(isEn: Boolean, ar: String, en: String): String = if (isEn) en else ar
