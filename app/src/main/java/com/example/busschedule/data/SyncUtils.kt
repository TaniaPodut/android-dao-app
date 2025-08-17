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

// Funcție helper pentru a converti milisecunde/secondes în string de timp canonic
fun convertMillisToTimeString(value: Int): String {
    val timestamp = value.toLong()
    val timeInMillis = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    val sdf = SimpleDateFormat("h:mm a", Locale.ENGLISH)
    return sdf.format(Date(timeInMillis))
}
