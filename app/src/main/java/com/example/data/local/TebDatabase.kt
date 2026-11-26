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
        DirectMessageEntity::class,
        PermanentMedicalLogEntity::class,
        FamilyLinkRequestEntity::class,
        PlatformVisitorHitEntity::class,
    ],
    version = 14,
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `verifiedBy` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `verifiedAtMillis` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `otpCode` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `otpCreatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `otpExpiresAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `otpUsed` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `weightKg` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `heightCm` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `bloodType` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `chronicConditions` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `drugAllergies` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `otherChronicNotes` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `aiSummaryAr` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `aiSummaryEn` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `aiSummaryUpdatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `aiSummaryError` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `chatEnabled` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `delay_notifications` ADD COLUMN `targetAudience` TEXT NOT NULL DEFAULT 'ALL'")
                db.execSQL("ALTER TABLE `delay_notifications` ADD COLUMN `targetId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `delay_notifications` ADD COLUMN `senderLabel` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `direct_messages` (
                        `id` TEXT NOT NULL,
                        `threadType` TEXT NOT NULL,
                        `patientAccountId` TEXT NOT NULL DEFAULT '',
                        `patientName` TEXT NOT NULL DEFAULT '',
                        `patientPhone` TEXT NOT NULL DEFAULT '',
                        `doctorId` TEXT NOT NULL DEFAULT '',
                        `doctorName` TEXT NOT NULL DEFAULT '',
                        `clinicName` TEXT NOT NULL DEFAULT '',
                        `senderRole` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL DEFAULT 0,
                        `isReadByPatient` INTEGER NOT NULL DEFAULT 0,
                        `isReadByClinic` INTEGER NOT NULL DEFAULT 0,
                        `isReadByAdmin` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `driveWebAppUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medical_records` ADD COLUMN `driveFileUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `permanent_medical_logs` (
                        `id` TEXT NOT NULL,
                        `familyMemberId` TEXT NOT NULL,
                        `patientName` TEXT NOT NULL,
                        `patientPhone` TEXT NOT NULL,
                        `sourceRecordId` TEXT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `eventTimestampMillis` INTEGER NOT NULL DEFAULT 0,
                        `eventDateTimeLabel` TEXT NOT NULL DEFAULT '',
                        `providerName` TEXT NOT NULL DEFAULT '',
                        `specialtyOrCategory` TEXT NOT NULL DEFAULT '',
                        `title` TEXT NOT NULL DEFAULT '',
                        `narrativeAr` TEXT NOT NULL DEFAULT '',
                        `narrativeEn` TEXT NOT NULL DEFAULT '',
                        `isAiGenerated` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clinic_doctors` ADD COLUMN `secondaryDriveWebAppUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_members` ADD COLUMN `nationalId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `linkedSpouseAccountId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `linkedSpousePhone` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `linkedSpouseName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `patient_accounts` ADD COLUMN `allowSpouseViewPersonalRecords` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `family_link_requests` (
                        `id` TEXT NOT NULL,
                        `requesterAccountId` TEXT NOT NULL,
                        `requesterName` TEXT NOT NULL,
                        `requesterPhone` TEXT NOT NULL,
                        `targetPhone` TEXT NOT NULL,
                        `relation` TEXT NOT NULL DEFAULT 'SPOUSE',
                        `proofMarriageContractUrl` TEXT NOT NULL DEFAULT '',
                        `proofNationalIdFrontUrl` TEXT NOT NULL DEFAULT '',
                        `proofNationalIdBackUrl` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `reviewedBy` TEXT NOT NULL DEFAULT '',
                        `reviewedAtMillis` INTEGER NOT NULL DEFAULT 0,
                        `requestedAtMillis` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `platform_visitor_hits` (
                        `id` TEXT NOT NULL,
                        `platform` TEXT NOT NULL,
                        `timestampMillis` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `appointment_slots` ADD COLUMN `appointmentTimestamp` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `appointment_slots` ADD COLUMN `reminded2h` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `appointment_slots` ADD COLUMN `reminded30m` INTEGER NOT NULL DEFAULT 0")
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
                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                            MIGRATION_8_9,
                            MIGRATION_9_10,
                            MIGRATION_10_11,
                            MIGRATION_11_12,
                            MIGRATION_12_13,
                            MIGRATION_13_14,
                        )
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()
                }.also { INSTANCE = it }
            }
        }
    }
}
