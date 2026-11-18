package com.example.util

data class ChronicConditionOption(
    val key: String,
    val labelAr: String,
    val labelEn: String,
)

object ChronicConditionsCatalog {
    val bloodTypes: List<String> = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    val presets: List<ChronicConditionOption> = listOf(
        ChronicConditionOption("diabetes", "السكر", "Diabetes"),
        ChronicConditionOption("hypertension", "ضغط الدم", "Hypertension"),
        ChronicConditionOption("heart", "أمراض القلب", "Heart Disease"),
        ChronicConditionOption("asthma", "الحساسية الصدرية / الربو", "Asthma / Respiratory Allergy"),
        ChronicConditionOption("kidney", "أمراض الكلى", "Kidney Disease"),
        ChronicConditionOption("liver", "أمراض الكبد", "Liver Disease"),
    )

    fun parseKeys(raw: String): List<String> =
        raw.split("|", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun encodeKeys(keys: List<String>): String =
        keys.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("|")

    fun labelForKey(key: String, isEn: Boolean): String {
        val found = presets.firstOrNull {
            it.key.equals(key, ignoreCase = true) ||
                it.labelAr == key ||
                it.labelEn.equals(key, ignoreCase = true)
        }
        return if (found != null) {
            if (isEn) found.labelEn else found.labelAr
        } else {
            key
        }
    }

    fun formatLabels(
        rawKeys: String,
        otherNotes: String = "",
        isEn: Boolean = false,
    ): String {
        val items = parseKeys(rawKeys).map { labelForKey(it, isEn) }.toMutableList()
        otherNotes.trim().takeIf { it.isNotEmpty() }?.let { items.add(it) }
        return items.distinct().joinToString(" • ")
    }
}
