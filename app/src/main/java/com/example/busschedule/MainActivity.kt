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
package com.example.busschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.busschedule.ui.BusScheduleApp
import com.example.busschedule.ui.theme.BusScheduleTheme
import com.example.busschedule.data.AppDatabase
import com.example.busschedule.data.SupabaseSync
import com.example.busschedule.data.SupabaseManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://supabase2.ruggedradiance.store",
    supabaseKey = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdXBhYmFzZSIsImlhdCI6MTc0ODM2NTMyMCwiZXhwIjo0OTA0MDM4OTIwLCJyb2xlIjoic2VydmljZV9yb2xlIn0.AFC-XC3i517X-Ur0nYikIO6io1y4KSJ48BUkh_HJNs4"
) {
    install(Postgrest)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val dao = db.busScheduleDao()
                val supabaseSync = SupabaseSync()

                // Sincronizare cu Supabase folosind noul SupabaseManager
                supabaseSync.syncWithSupabase(dao, SupabaseManager.client)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            BusScheduleTheme {
                BusScheduleApp()
            }
        }
    }
}
