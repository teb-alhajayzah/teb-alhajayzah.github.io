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
    ],
    version = 2,
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

        fun getInstance(context: Context): TebDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TebDatabase::class.java,
                    "teb_al_hajayzah.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
