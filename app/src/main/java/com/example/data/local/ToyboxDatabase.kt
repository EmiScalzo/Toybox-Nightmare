package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val totalRuns: Int = 0,
    val totalWins: Int = 0,
    val totalKills: Int = 0,
    val totalRoomsCleared: Int = 0,
    val challengePoints: Int = 0,
    val medals: Int = 0,
    val highestFloor: Int = 1,
    val tutorialCompleted: Boolean = false,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val screenShakeEnabled: Boolean = true,
    val darkVignetteIntensity: Float = 0.85f,
    val companionGrudges: String = "",
    val loreSeen: Boolean = false,
    val manufacturerAccepted: Int = 0,
    val manufacturerRejected: Int = 0
)

@Entity(tableName = "achievement_progress")
data class AchievementProgressEntity(
    @PrimaryKey val id: String,
    val unlockedAtEpochMs: Long
)

@Entity(tableName = "collection_discovery")
data class CollectionDiscoveryEntity(
    @PrimaryKey val entryKey: String, // e.g. "part:spring_arm", "enemy:TIN_SOLDIER"
    val category: String,
    val discoveredAtEpochMs: Long
)

@Entity(tableName = "unlocked_character")
data class UnlockedCharacterEntity(
    @PrimaryKey val characterId: String,
    val unlockedAtEpochMs: Long
)

@Entity(tableName = "run_history")
data class RunHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: String,
    val difficulty: String,
    val victory: Boolean,
    val floorReached: Int,
    val roomsCleared: Int,
    val enemiesDefeated: Int,
    val durationSeconds: Int,
    val challengeScore: Int,
    val timestampMs: Long
)

@Dao
interface ToyboxDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfile(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM achievement_progress")
    fun observeAchievements(): Flow<List<AchievementProgressEntity>>

    @Query("SELECT * FROM achievement_progress")
    suspend fun getAchievements(): List<AchievementProgressEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(item: AchievementProgressEntity): Long

    @Query("SELECT * FROM collection_discovery")
    fun observeDiscoveries(): Flow<List<CollectionDiscoveryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDiscovery(item: CollectionDiscoveryEntity)

    @Query("SELECT * FROM unlocked_character")
    fun observeUnlockedCharacters(): Flow<List<UnlockedCharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedCharacter(item: UnlockedCharacterEntity)

    @Query("SELECT * FROM run_history ORDER BY timestampMs DESC LIMIT 20")
    fun observeRunHistory(): Flow<List<RunHistoryEntity>>

    @Insert
    suspend fun insertRunHistory(run: RunHistoryEntity)
}

@Database(
    entities = [
        PlayerProfileEntity::class,
        AchievementProgressEntity::class,
        CollectionDiscoveryEntity::class,
        UnlockedCharacterEntity::class,
        RunHistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ToyboxDatabase : RoomDatabase() {
    abstract fun dao(): ToyboxDao

    companion object {
        @Volatile
        private var INSTANCE: ToyboxDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE player_profile ADD COLUMN musicEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE player_profile ADD COLUMN companionGrudges TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE player_profile ADD COLUMN loreSeen INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE player_profile ADD COLUMN manufacturerAccepted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE player_profile ADD COLUMN manufacturerRejected INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): ToyboxDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ToyboxDatabase::class.java,
                    "toybox_nightmare.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
        }
    }
}
