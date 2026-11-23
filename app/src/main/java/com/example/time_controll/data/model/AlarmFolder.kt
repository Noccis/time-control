package com.example.time_controll.data.model

import org.json.JSONObject
import java.util.UUID

data class AlarmFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: Long = 0xFF3F51B5, // Default Indigo
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("colorHex", colorHex)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AlarmFolder {
            return AlarmFolder(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Folder"),
                colorHex = json.optLong("colorHex", 0xFF3F51B5),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val PRESET_COLORS = listOf(
            0xFF3F51B5, // Indigo
            0xFF2196F3, // Blue
            0xFF009688, // Teal
            0xFF4CAF50, // Green
            0xFFFF9800, // Orange
            0xFFE91E63, // Pink
            0xFF9C27B0, // Purple
            0xFF607D8B  // Blue Grey
        )
    }
}
