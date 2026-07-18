package com.neonrush.game.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameProfile(
    val id: Int = 1,
    val username: String = "NeonPilot_99",
    val bestScore: Int = 0,
    val gems: Int = 120, // Starter gems
    val transcendenceCount: Int = 0,
    val activeSkinId: String = "cyan_diamond",
    val unlockedSkinsCsv: String = "cyan_diamond,purple_square,green_triangle",
    val followedUsersCsv: String = "CyberRunner,ZeroGlitch,RetroWave",
    val subscriptionPro: Boolean = false,
    val dailyAttemptsToday: Int = 0,
    val lastDailyRushDate: String = "",
    val activePilotSkinId: String = "default",
    val unlockedPilotSkinsCsv: String = "default",
    val currentStreak: Int = 0,
    val lastStreakLoginDate: String = "",
    val totalRuns: Int = 0,
    val averageScore: Int = 0,
    val totalGemsEarned: Int = 0
)
    


data class GhostChallengeEntity(
    val challengeId: String,
    val playerName: String,
    val score: Int,
    val zoneReached: Int,
    val yPositionsCsv: String,
    val timestamp: Long = System.currentTimeMillis()
)

class GameDao(context: Context) {
    private val dbHelper = GameDbHelper(context)
    private val _profileFlow = MutableStateFlow<GameProfile?>(null)
    
    init {
        refreshProfile()
    }

    fun getProfileFlow(): StateFlow<GameProfile?> {
        return _profileFlow.asStateFlow()
    }

    private fun refreshProfile() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM game_profile WHERE id = 1", null)
        if (cursor.moveToFirst()) {
            val usernameIdx = cursor.getColumnIndex("username")
            val bestScoreIdx = cursor.getColumnIndex("bestScore")
            val gemsIdx = cursor.getColumnIndex("gems")
            val transIdx = cursor.getColumnIndex("transcendenceCount")
            val activeSkinIdx = cursor.getColumnIndex("activeSkinId")
            val unlockedSkinsIdx = cursor.getColumnIndex("unlockedSkinsCsv")
            val followedIdx = cursor.getColumnIndex("followedUsersCsv")
            val subIdx = cursor.getColumnIndex("subscriptionPro")
            val dailyIdx = cursor.getColumnIndex("dailyAttemptsToday")
            val dateIdx = cursor.getColumnIndex("lastDailyRushDate")
            val activePilotSkinIdx = cursor.getColumnIndex("activePilotSkinId")
            val unlockedPilotSkinsIdx = cursor.getColumnIndex("unlockedPilotSkinsCsv")
            val currentStreakIdx = cursor.getColumnIndex("currentStreak")
            val lastStreakLoginIdx = cursor.getColumnIndex("lastStreakLoginDate")
            val profile = GameProfile(
                id = 1,
                username = if (usernameIdx != -1) cursor.getString(usernameIdx) else "NeonPilot_99",
                bestScore = if (bestScoreIdx != -1) cursor.getInt(bestScoreIdx) else 0,
                gems = if (gemsIdx != -1) cursor.getInt(gemsIdx) else 120,
                transcendenceCount = if (transIdx != -1) cursor.getInt(transIdx) else 0,
                activeSkinId = if (activeSkinIdx != -1) cursor.getString(activeSkinIdx) else "cyan_diamond",
                unlockedSkinsCsv = if (unlockedSkinsIdx != -1) cursor.getString(unlockedSkinsIdx) else "cyan_diamond,purple_square,green_triangle",
                followedUsersCsv = if (followedIdx != -1) cursor.getString(followedIdx) else "CyberRunner,ZeroGlitch,RetroWave",
                subscriptionPro = if (subIdx != -1) cursor.getInt(subIdx) == 1 else false,
                dailyAttemptsToday = if (dailyIdx != -1) cursor.getInt(dailyIdx) else 0,
                lastDailyRushDate = if (dateIdx != -1) cursor.getString(dateIdx) else "",
                activePilotSkinId = if (activePilotSkinIdx != -1) cursor.getString(activePilotSkinIdx) else "default",
                unlockedPilotSkinsCsv = if (unlockedPilotSkinsIdx != -1) cursor.getString(unlockedPilotSkinsIdx) else "default",
                currentStreak = if (currentStreakIdx != -1) cursor.getInt(currentStreakIdx) else 0,
                lastStreakLoginDate = if (lastStreakLoginIdx != -1) cursor.getString(lastStreakLoginIdx) else ""
            
            )
            _profileFlow.value = profile
        } else {
            // Save initial profile
            val initial = GameProfile()
            saveProfileDirect(initial)
            _profileFlow.value = initial
        }
        cursor.close()
    }

    suspend fun getProfileDirect(): GameProfile? {
        refreshProfile()
        return _profileFlow.value
    }

    suspend fun saveProfile(profile: GameProfile) {
        saveProfileDirect(profile)
    }

    private fun saveProfileDirect(profile: GameProfile) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", 1)
            put("username", profile.username)
            put("bestScore", profile.bestScore)
            put("gems", profile.gems)
            put("transcendenceCount", profile.transcendenceCount)
            put("activeSkinId", profile.activeSkinId)
            put("unlockedSkinsCsv", profile.unlockedSkinsCsv)
            put("followedUsersCsv", profile.followedUsersCsv)
            put("subscriptionPro", if (profile.subscriptionPro) 1 else 0)
            put("dailyAttemptsToday", profile.dailyAttemptsToday)
            put("lastDailyRushDate", profile.lastDailyRushDate)
            put("activePilotSkinId", profile.activePilotSkinId)
            put("unlockedPilotSkinsCsv", profile.unlockedPilotSkinsCsv)
            put("currentStreak", profile.currentStreak)
            put("lastStreakLoginDate", profile.lastStreakLoginDate)
            put("totalRuns", profile.totalRuns)
            put("averageScore", profile.averageScore)
            put("totalGemsEarned", profile.totalGemsEarned)
        }
        db.insertWithOnConflict("game_profile", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _profileFlow.value = profile
    }

    suspend fun insertGhost(ghost: GhostChallengeEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("challengeId", ghost.challengeId)
            put("playerName", ghost.playerName)
            put("score", ghost.score)
            put("zoneReached", ghost.zoneReached)
            put("yPositionsCsv", ghost.yPositionsCsv)
            put("timestamp", ghost.timestamp)
        }
        db.insertWithOnConflict("ghost_challenges", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getGhostById(id: String): GhostChallengeEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ghost_challenges WHERE challengeId = ?", arrayOf(id))
        var ghost: GhostChallengeEntity? = null
        if (cursor.moveToFirst()) {
            val playerIdx = cursor.getColumnIndex("playerName")
            val scoreIdx = cursor.getColumnIndex("score")
            val zoneIdx = cursor.getColumnIndex("zoneReached")
            val yIdx = cursor.getColumnIndex("yPositionsCsv")
            val tsIdx = cursor.getColumnIndex("timestamp")

            ghost = GhostChallengeEntity(
                challengeId = id,
                playerName = if (playerIdx != -1) cursor.getString(playerIdx) else "",
                score = if (scoreIdx != -1) cursor.getInt(scoreIdx) else 0,
                zoneReached = if (zoneIdx != -1) cursor.getInt(zoneIdx) else 1,
                yPositionsCsv = if (yIdx != -1) cursor.getString(yIdx) else "",
                timestamp = if (tsIdx != -1) cursor.getLong(tsIdx) else System.currentTimeMillis()
            )
        }
        cursor.close()
        return ghost
    }
}

class GameDbHelper(context: Context) : SQLiteOpenHelper(context, "neon_rush_companion.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE game_profile (
                id INTEGER PRIMARY KEY,
                username TEXT,
                bestScore INTEGER,
                gems INTEGER,
                transcendenceCount INTEGER,
                activeSkinId TEXT,
                unlockedSkinsCsv TEXT,
                followedUsersCsv TEXT,
                subscriptionPro INTEGER,
                dailyAttemptsToday INTEGER,
                lastDailyRushDate TEXT,
                activePilotSkinId TEXT,
                unlockedPilotSkinsCsv TEXT,
                currentStreak INTEGER,
                lastStreakLoginDate TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE ghost_challenges (
                challengeId TEXT PRIMARY KEY,
                playerName TEXT,
                score INTEGER,
                zoneReached INTEGER,
                yPositionsCsv TEXT,
                timestamp INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE game_profile ADD COLUMN activePilotSkinId TEXT DEFAULT 'default'")
            db.execSQL("ALTER TABLE game_profile ADD COLUMN unlockedPilotSkinsCsv TEXT DEFAULT 'default'")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE game_profile ADD COLUMN currentStreak INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE game_profile ADD COLUMN lastStreakLoginDate TEXT DEFAULT ''")
        }
    }
}
