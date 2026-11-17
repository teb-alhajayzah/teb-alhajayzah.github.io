package com.example.util

import com.example.data.local.ClinicDoctorEntity

data class MedicalSpecialtyItem(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val medCatalogKey: String,
    val aliases: List<String> = emptyList(),
) {
    fun label(isEn: Boolean): String = if (isEn) nameEn else nameAr
}

object SpecialtyCatalog {

    val all: List<MedicalSpecialtyItem> = listOf(
        MedicalSpecialtyItem(
            id = "pediatrics",
            nameAr = "طب الأطفال وحديثي الولادة",
            nameEn = "Pediatrics & Neonatology",
            medCatalogKey = "أطفال وحديثي الولادة",
            aliases = listOf("أطفال", "طب الأطفال", "أطفال وحديثي الولادة", "pediatrics"),
        ),
        MedicalSpecialtyItem(
            id = "obgyn",
            nameAr = "النساء والتوليد وتأخر الإنجاب",
            nameEn = "Obstetrics & Gynecology",
            medCatalogKey = "نساء وتوليد",
            aliases = listOf("نساء وتوليد", "النساء والتوليد", "obgyn", "ob/gyn"),
        ),
        MedicalSpecialtyItem(
            id = "internal_medicine",
            nameAr = "الباطنة العامة والجهاز الهضمي والكبد",
            nameEn = "Internal Medicine & Gastroenterology",
            medCatalogKey = "باطنة وجهاز هضمي",
            aliases = listOf("باطنة", "الباطنة", "باطنة وجهاز هضمي", "internal medicine"),
        ),
        MedicalSpecialtyItem(
            id = "cardiology",
            nameAr = "القلب والأوعية الدموية",
            nameEn = "Cardiology",
            medCatalogKey = "قلب وأوعية دموية",
            aliases = listOf("قلب وأوعية دموية", "قلب", "cardiology"),
        ),
        MedicalSpecialtyItem(
            id = "general_surgery",
            nameAr = "الجراحة العامة والمناظير",
            nameEn = "General Surgery",
            medCatalogKey = "باطنة وجهاز هضمي",
            aliases = listOf("جراحة", "الجراحة العامة", "general surgery"),
        ),
        MedicalSpecialtyItem(
            id = "orthopedics",
            nameAr = "العظام والمفاصل والعمود الفقري",
            nameEn = "Orthopedics",
            medCatalogKey = "عظام ومفاصل",
            aliases = listOf("عظام", "عظام ومفاصل", "orthopedics"),
        ),
        MedicalSpecialtyItem(
            id = "dermatology",
            nameAr = "الجلدية والتناسلية والليزر",
            nameEn = "Dermatology",
            medCatalogKey = "جلدية وتجميل",
            aliases = listOf("جلدية", "جلدية وتجميل", "dermatology"),
        ),
        MedicalSpecialtyItem(
            id = "ent",
            nameAr = "الأنف والأذن والحنجرة",
            nameEn = "ENT",
            medCatalogKey = "أنف وأذن وحنجرة",
            aliases = listOf("أنف وأذن وحنجرة", "ent"),
        ),
        MedicalSpecialtyItem(
            id = "ophthalmology",
            nameAr = "طب وجراحة العيون",
            nameEn = "Ophthalmology",
            medCatalogKey = "رمد وجراحة عيون",
            aliases = listOf("عيون", "رمد وجراحة عيون", "ophthalmology"),
        ),
        MedicalSpecialtyItem(
            id = "dentistry",
            nameAr = "طب وجراحة الأسنان",
            nameEn = "Dentistry",
            medCatalogKey = "أسنان",
            aliases = listOf("أسنان", "dentistry"),
        ),
        MedicalSpecialtyItem(
            id = "neurology",
            nameAr = "المخ والأعصاب",
            nameEn = "Neurology",
            medCatalogKey = "باطنة وجهاز هضمي",
            aliases = listOf("مخ وأعصاب", "neurology"),
        ),
        MedicalSpecialtyItem(
            id = "urology",
            nameAr = "المسالك البولية والتناسلية",
            nameEn = "Urology",
            medCatalogKey = "باطنة وجهاز هضمي",
            aliases = listOf("مسالك بولية", "urology"),
        ),
        MedicalSpecialtyItem(
            id = "pulmonology",
            nameAr = "الصدر والجهاز التنفسي",
            nameEn = "Pulmonology",
            medCatalogKey = "باطنة وجهاز هضمي",
            aliases = listOf("صدر", "الجهاز التنفسي", "pulmonology"),
        ),
        MedicalSpecialtyItem(
            id = "eye_care",
            nameAr = "الرمد",
            nameEn = "Eye Care",
            medCatalogKey = "رمد وجراحة عيون",
            aliases = listOf("الرمد", "eye care"),
        ),
        MedicalSpecialtyItem(
            id = "physical_therapy",
            nameAr = "العلاج الطبيعي",
            nameEn = "Physical Therapy",
            medCatalogKey = "عظام ومفاصل",
            aliases = listOf("علاج طبيعي", "physical therapy"),
        ),
        MedicalSpecialtyItem(
            id = "medical_lab",
            nameAr = "التحاليل الطبية",
            nameEn = "Medical Laboratory",
            medCatalogKey = "باطنة وجهاز هضمي",
            aliases = listOf("تحاليل", "معامل", "medical laboratory"),
        ),
        MedicalSpecialtyItem(
            id = "radiology",
            nameAr = "الأشعة التشخيصية",
            nameEn = "Diagnostic Radiology",
            medCatalogKey = "عظام ومفاصل",
            aliases = listOf("أشعة", "radiology"),
        ),
        MedicalSpecialtyItem(
            id = "other",
            nameAr = "تخصصات أخرى",
            nameEn = "Other Specialties",
            medCatalogKey = "",
            aliases = listOf("تخصصات أخرى", "other", "other specialties"),
        ),
    )

    fun findByNameOrAlias(raw: String): MedicalSpecialtyItem? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        return all.firstOrNull { item ->
            item.nameAr.equals(clean, ignoreCase = true) ||
                item.nameEn.equals(clean, ignoreCase = true) ||
                item.aliases.any { it.equals(clean, ignoreCase = true) }
        }
    }

    fun normalizeSpecialty(raw: String): String {
        val clean = raw.trim()
        return findByNameOrAlias(clean)?.nameAr ?: clean
    }

    fun displaySpecialty(raw: String, isEn: Boolean): String {
        val clean = raw.trim()
        if (clean.isEmpty()) return ""
        val withoutExtSuffix = clean.removeSuffix(" (زيارة خارجية)").trim()
        val hasExt = clean.endsWith(" (زيارة خارجية)")
        val item = findByNameOrAlias(withoutExtSuffix)
        val base = item?.label(isEn) ?: withoutExtSuffix
        return if (hasExt) {
            if (isEn) "$base (External Visit)" else "$base (زيارة خارجية)"
        } else {
            base
        }
    }

    fun parseAdditionalSpecialties(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|", "،", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun serializeAdditionalSpecialties(items: List<String>): String {
        return items.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("|")
    }

    fun allDoctorSpecialties(primary: String, additionalRaw: String): List<String> {
        val list = mutableListOf<String>()
        val cleanPrimary = primary.trim()
        if (cleanPrimary.isNotEmpty()) {
            list += cleanPrimary
        }
        parseAdditionalSpecialties(additionalRaw).forEach { extra ->
            if (list.none { it.equals(extra, ignoreCase = true) }) {
                list += extra
            }
        }
        return list
    }

    fun formatDoctorSpecialties(
        primary: String,
        additionalRaw: String,
        isEn: Boolean,
    ): String {
        val sep = if (isEn) " • " else " • "
        return allDoctorSpecialties(primary, additionalRaw)
            .map { displaySpecialty(it, isEn) }
            .joinToString(sep)
    }

    fun doctorMatchesSpecialty(doctor: ClinicDoctorEntity, filterSpec: String): Boolean {
        val targetNorm = normalizeSpecialty(filterSpec)
        val docSpecs = allDoctorSpecialties(doctor.specialty, doctor.additionalSpecialties)
        return docSpecs.any { spec ->
            spec.equals(filterSpec, ignoreCase = true) ||
                normalizeSpecialty(spec).equals(targetNorm, ignoreCase = true)
        }
    }

    fun medicationCatalogKeysForDoctor(doctor: ClinicDoctorEntity): Set<String> {
        return allDoctorSpecialties(doctor.specialty, doctor.additionalSpecialties)
            .mapNotNull { spec ->
                findByNameOrAlias(spec)?.medCatalogKey?.takeIf { it.isNotBlank() } ?: spec.takeIf { it.isNotBlank() }
            }
            .toSet()
    }
}
