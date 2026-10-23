package com.example.data.model

import androidx.room.TypeConverter

class AuditConverters {
    @TypeConverter
    fun fromFormType(type: AuditFormType): String = type.name

    @TypeConverter
    fun toFormType(value: String): AuditFormType {
        return try {
            AuditFormType.valueOf(value)
        } catch (_: Exception) {
            AuditFormType.WITH_COOLER
        }
    }
}
