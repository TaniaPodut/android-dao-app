/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.busschedule.data

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.InternalSerializationApi
import java.text.SimpleDateFormat
import java.util.*

@OptIn(InternalSerializationApi::class)
@Serializable
data class BusScheduleSerializable(
    val id: Int,
    @SerialName("arrival_time")
    val arrivalTime: String,
    @SerialName("stop_name")
    val stopName: String
)

// Data class simplă pentru maparea din response-ul raw
data class BusScheduleRaw(
    val id: Int,
    val arrivalTime: String,
    val stopName: String
)

class SupabaseSync {

    companion object {
        private const val TAG = "SupabaseSync"
    }

    suspend fun syncWithSupabase(dao: BusScheduleDao, supabaseClient: SupabaseClient) {
        Log.d(TAG, "🚀 Începe sincronizarea cu Supabase...")

        try {
            // 0. Testează conexiunea folosind SupabaseManager
            Log.d(TAG, "🔍 Testez conexiunea la Supabase...")
            val connectionWorks = SupabaseManager.testConnection()

            if (!connectionWorks) {
                Log.e(TAG, "❌ Nu pot conecta la Supabase")
                return
            }

            Log.d(TAG, "✅ Conexiunea la Supabase funcționează")

            // 1. Obține datele locale
            val localSchedules = dao.getAllSchedules()
            Log.d(TAG, "📱 Date locale găsite: ${localSchedules.size} intrări")

            // Debug: afișează primele 5 înregistrări din baza de date locală
            localSchedules.take(5).forEachIndexed { index, schedule ->
                val timeString = convertMillisToTimeString(schedule.arrivalTimeInMillis)
                Log.d(TAG, "  Debug Local[$index]: ID=${schedule.id}, MillisRaw=${schedule.arrivalTimeInMillis}, TimpConvertit=$timeString, Stație=${schedule.stopName}")
            }

            // 2. Obține datele din Supabase
            Log.d(TAG, "🌐 Obțin datele existente din Supabase...")
            val onlineSchedules = SupabaseManager.getAllNotes()
            Log.d(TAG, "📡 Date online găsite: ${onlineSchedules.size} intrări")

            // Debug: afișează primele 5 înregistrări din Supabase
            onlineSchedules.take(5).forEachIndexed { index, schedule ->
                Log.d(TAG, "  Debug Online[$index]: ID=${schedule.id}, Timp=${schedule.arrivalTime}, Stație=${schedule.stopName}")
            }

            // 3. Găsește datele noi (care nu există în Supabase)
            val newSchedules = localSchedules.filter { local ->
                val localTimeString = convertMillisToTimeString(local.arrivalTimeInMillis)
                val exists = onlineSchedules.any { online ->
                    online.id == local.id &&
                    online.arrivalTime == localTimeString &&
                    online.stopName == local.stopName
                }
                !exists
            }

            Log.d(TAG, "📊 Date noi de sincronizat: ${newSchedules.size} intrări")

            // 4. Inserează datele noi în Supabase
            if (newSchedules.isNotEmpty()) {
                Log.d(TAG, "📤 Sincronizez ${newSchedules.size} intrări noi...")

                // Logging detaliat pentru fiecare înregistrare înainte de mapare
                newSchedules.forEachIndexed { index, schedule ->
                    val timeString = convertMillisToTimeString(schedule.arrivalTimeInMillis)
                    Log.d(TAG, "  Pre-mapare[$index]: ID=${schedule.id}, MillisOriginali=${schedule.arrivalTimeInMillis}, TimpConvertit=$timeString, Stație=${schedule.stopName}")
                }

                val dataToInsert = newSchedules.map { schedule ->
                    val convertedTime = convertMillisToTimeString(schedule.arrivalTimeInMillis)
                    Log.d(TAG, "  Mapare: ID=${schedule.id} -> Timp=$convertedTime")
                    BusScheduleSerializable(
                        id = schedule.id,
                        arrivalTime = convertedTime,
                        stopName = schedule.stopName
                    )
                }

                // Logging pentru datele finale mapate
                Log.d(TAG, "📋 Date finale pentru Supabase:")
                dataToInsert.forEachIndexed { index, data ->
                    Log.d(TAG, "  Final[$index]: ID=${data.id}, Timp=${data.arrivalTime}, Stație=${data.stopName}")
                }

                val success = SupabaseManager.insertNotes(dataToInsert)

                if (success) {
                    Log.d(TAG, "🎉 Sincronizare completă! Trimise ${newSchedules.size} intrări")
                } else {
                    Log.e(TAG, "❌ Eroare la sincronizare")
                }
            } else {
                Log.d(TAG, "✅ Toate datele sunt deja sincronizate")
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Eroare generală în sincronizare: ${e.message}", e)
        }
    }

    // Funcție helper pentru a converti timestamp Unix în string de timp
    private fun convertMillisToTimeString(timestamp: Int): String {
        // Verificăm dacă este timestamp Unix (secunde) sau milisecunde
        val timeInMillis = if (timestamp > 1000000000) {
            // Pare să fie timestamp Unix în secunde, convertim la milisecunde
            timestamp.toLong() * 1000
        } else {
            // Pare să fie deja în milisecunde sau altă unitate
            timestamp.toLong()
        }

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val convertedTime = sdf.format(Date(timeInMillis))

        Log.d("SupabaseSync", "🕐 Conversie timp: $timestamp -> $timeInMillis ms -> $convertedTime")
        return convertedTime
    }
}
