package com.example.busschedule.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

suspend fun sendMappedDataToSupabase(
    dao: BusScheduleDao,
    supabase: SupabaseClient
) {
    try {
        val localList = dao.getAll().first()  // extrage prima valoare emisă din Flow

        val mapped = localList.map {
            mapOf(
                "id" to it.id,
                "stop_name" to it.stopName,
                "arrival_time" to convertMillisToTimeString(it.arrivalTimeInMillis)
            )
        }

        supabase.from("BusSchedule").insert(mapped)
        println("✅ Trimise cu succes în Supabase!")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Funcție helper pentru a converti milisecunde în string de timp
private fun convertMillisToTimeString(millis: Int): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis.toLong()))
}
