package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PatientAccountEntity::class,
        FamilyMemberEntity::class,
        ClinicDoctorEntity::class,
        AppointmentSlotEntity::class,
        MedicalRecordEntity::class,
        DelayNotificationEntity::class,
        DoctorFavoriteMedEntity::class,
        BlockedPhoneEntity::class,
        ActivationOtpCodeEntity::class,
        MedicalLabEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class TebDatabase : RoomDatabase() {
    abstract fun tebDao(): TebDao

    companion object {
        @Volatile
        private var INSTANCE: TebDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `doctor_favorite_meds` (
                        `doctorId` TEXT NOT NULL,
                        `medicationId` TEXT NOT NULL,
                        PRIMARY KEY(`doctorId`, `medicationId`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `bio` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `mapsUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `username` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `password` TEXT NOT NULL DEFAULT '1234'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `isPhoneVerified` INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blocked_phones` (
                        `phone` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `blockedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`phone`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activation_otp_codes` (
                        `code` TEXT NOT NULL,
                        `isUsed` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `usedByName` TEXT NOT NULL,
                        `usedByPhone` TEXT NOT NULL,
                        `usedAtMillis` INTEGER NOT NULL,
                        `generatedBy` TEXT NOT NULL,
                        PRIMARY KEY(`code`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `medical_labs` (
                        `id` TEXT NOT NULL,
                        `labName` TEXT NOT NULL,
                        `phone` TEXT NOT NULL DEFAULT '',
                        `locationLabel` TEXT NOT NULL DEFAULT '',
                        `username` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `attachmentMimeType` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `attachmentName` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `doctorNameEn` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `clinicNameEn` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `locationLabelEn` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `additionalSpecialties` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `diagnosisEn` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `medicationsEn` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `notesEn` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `aiReportAr` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `aiReportEn` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): TebDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val appCtx = context.applicationContext
                    // حذف قاعدة البيانات التجريبية القديمة تلقائياً لضمان عدم بقاء أي بيانات وهمية على الجهاز أو المحاكي
                    appCtx.deleteDatabase("teb_al_hajayzah.db")
                    Room.databaseBuilder(
                        appCtx,
                        TebDatabase::class.java,
                        "teb_al_hajayzah_prod_v100.db",
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                        .build()
                }.also { INSTANCE = it }
            }
        }
    }
}
