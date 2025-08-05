package com.example.busschedule.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest

class Repository {


    fun addNote(title: String, content: String) {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseManager.client.postgrest["notes"].insert(
                mapOf(
                    "stop_name" to "stop_name",
                    "arrival_time" to "arrival_time"
                )
            )
        }
    }

}