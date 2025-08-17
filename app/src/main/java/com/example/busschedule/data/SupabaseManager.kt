@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.busschedule.data

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SupabaseManager {

    private const val SUPABASE_URL = "https://supabase2.ruggedradiance.store"
    private const val SUPABASE_KEY = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdXBhYmFzZSIsImlhdCI6MTc0ODM2NTMyMCwiZXhwIjo0OTA0MDM4OTIwLCJyb2xlIjoic2VydmljYV9yb2xlIn0.AFC-XC3i517X-Ur0nYikIO6io1y4KSJ48BUkh_HJNs4"

    const val TABLE_NAME = "notes"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }

    @Serializable
    private data class NoteRow(
        val id: Int? = null,
        @SerialName("arrival_time") val arrivalTime: String? = null,
        @SerialName("stop_name") val stopName: String? = null
    )

    suspend fun testConnection(): Boolean = try {
        client.from(TABLE_NAME).select()
        true
    } catch (_: Exception) {
        false
    }

    suspend fun getAllNotes(): List<BusScheduleSerializable> = try {
        val rows = client.from(TABLE_NAME)
            .select()
            .decodeList<NoteRow>()
        rows.filter { it.arrivalTime != null && it.stopName != null }
            .map {
                BusScheduleSerializable(
                    id = it.id ?: -1,
                    arrivalTime = it.arrivalTime!!,
                    stopName = it.stopName!!
                )
            }
    } catch (_: Exception) {
        emptyList()
    }

    // Insert list without sending id to let DB autogenerate
    suspend fun insertNotes(dataList: List<BusScheduleSerializable>): Boolean = try {
        val rows = dataList.map { NoteRow(arrivalTime = it.arrivalTime, stopName = it.stopName) }
        client.from(TABLE_NAME).insert(rows)
        true
    } catch (e: Exception) {
        Log.e("SupabaseManager", "Insert failed: ${'$'}{e.message}", e)
        false
    }

    // Insert single without id
    suspend fun insertNote(stopName: String, arrivalTime: String): Boolean = try {
        client.from(TABLE_NAME).insert(NoteRow(arrivalTime = arrivalTime, stopName = stopName))
        true
    } catch (e: Exception) {
        Log.e("SupabaseManager", "Insert single failed: ${'$'}{e.message}", e)
        false
    }
}
