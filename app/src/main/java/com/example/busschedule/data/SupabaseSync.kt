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
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.text.SimpleDateFormat
import java.util.*

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
                val timeString = convertMillisToTimeString(schedule.arrivalTimeInMillis.toLong())
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
                val localTimeString = convertMillisToTimeString(local.arrivalTimeInMillis.toLong())
                val exists = onlineSchedules.any { online ->
                    // Comparăm DOAR după stop_name + timp normalizat, ignorăm id-ul
                    normalizeTimeString(online.arrivalTime) == localTimeString &&
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
                    val timeString = convertMillisToTimeString(schedule.arrivalTimeInMillis.toLong())
                    Log.d(TAG, "  Pre-mapare[$index]: ID=${schedule.id}, MillisOriginali=${schedule.arrivalTimeInMillis}, TimpConvertit=$timeString, Stație=${schedule.stopName}")
                }

                val dataToInsert = newSchedules.map { schedule ->
                    val convertedTime = convertMillisToTimeString(schedule.arrivalTimeInMillis.toLong())
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

    // Funcție helper pentru a converti timestamp (secunde sau milisecunde) în string de timp canonic "h:mm a"
    private fun convertMillisToTimeString(timestamp: Long): String {
        // Dacă e mai mic decât 1 trilion, tratăm ca secunde Unix; altfel ca milisecunde
        val timeInMillis = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
        val sdf = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        val convertedTime = sdf.format(Date(timeInMillis))
        Log.d("SupabaseSync", "🕐 Conversie timp: $timestamp -> $timeInMillis ms -> $convertedTime")
        return convertedTime
    }

    // Normalizează orice string de timp la formatul canonic "h:mm a" (Locale.ENGLISH)
    private fun normalizeTimeString(input: String): String {
        val canonicalFormatter = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        canonicalFormatter.isLenient = true

        val candidates = listOf(
            SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { isLenient = true },
            SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply { isLenient = true },
            SimpleDateFormat("H:mm", Locale.ENGLISH).apply { isLenient = true },
            SimpleDateFormat("HH:mm", Locale.ENGLISH).apply { isLenient = true }
        )

        val trimmed = input.trim().replace("\\s+".toRegex(), " ")
        // Încercăm parse cu AM/PM în engleză (uppercase) dacă e cazul
        val upper = trimmed.uppercase(Locale.ENGLISH)

        for (fmt in candidates) {
            try {
                val date = when (fmt.toPattern()) {
                    "h:mm a", "hh:mm a" -> fmt.parse(upper)
                    else -> fmt.parse(trimmed)
                }
                if (date != null) return canonicalFormatter.format(date)
            } catch (_: Exception) {
                // ignorăm și încercăm următorul pattern
            }
        }
        // Dacă nu reușim, returnăm inputul original trimis
        return trimmed
    }

    /**
     * Adaugă un timp de sosire în tabelul secundar folosind numele străzii
     * @param supabaseClient Clientul Supabase
     * @param streetName numele străzii
     * @param arrivalTime timpul de sosire în format "h:mm a" (va fi normalizat dacă e alt format)
     */
    suspend fun addArrivalTimeByStreetName(
        supabaseClient: SupabaseClient,
        streetName: String,
        arrivalTime: String
    ): Boolean {
        return try {
            val normalized = normalizeTimeString(arrivalTime)
            Log.d(TAG, "🕒 Adaug timpul $normalized pentru strada $streetName (original: $arrivalTime)")

            // Creare map cu datele de inserat
            val arrivalData = mapOf(
                "street_name" to streetName,
                "arrival_time" to normalized
            )

            // TODO: Efectuează inserția reală când endpoint-ul este stabilit
            Log.d(TAG, "✅ Timp de sosire pregătit pentru inserare cu succes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Eroare la adăugarea timpului de sosire: ${e.message}", e)
            false
        }
    }
}
