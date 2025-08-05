package com.example.busschedule.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import android.util.Log

/**
 * Singleton pentru gestionarea conexiunii la Supabase
 * Instanțiază clientul o singură dată și îl reutilizează în toată aplicația
 */
object SupabaseManager {

    // Configurația pentru Supabase
    private const val SUPABASE_URL = "https://supabase2.ruggedradiance.store"
    private const val SUPABASE_KEY = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdXBhYmFzZSIsImlhdCI6MTc0ODM2NTMyMCwiZXhwIjo0OTA0MDM4OTIwLCJyb2xlIjoic2VydmljZV9yb2xlIn0.AFC-XC3i517X-Ur0nYikIO6io1y4KSJ48BUkh_HJNs4"

    // Numele tabelului din Supabase (cel pe care l-ai creat)
    const val TABLE_NAME = "notes"

    // Instanța singleton a clientului Supabase
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Verifică dacă conexiunea la Supabase funcționează
     */
    suspend fun testConnection(): Boolean {
        return try {
            client.from(TABLE_NAME).select()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obține toate înregistrările din tabelul notes
     */
    suspend fun getAllNotes(): List<BusScheduleSerializable> {
        return try {
            client.from(TABLE_NAME)
                .select()
                .decodeList<BusScheduleSerializable>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Inserează mai multe înregistrări într-o singură operație
     */
    suspend fun insertNotes(dataList: List<BusScheduleSerializable>): Boolean {
        return try {
            Log.d("SupabaseManager", "🔄 Încerc să fac upsert pentru ${dataList.size} înregistrări...")
            dataList.forEachIndexed { index, data ->
                Log.d("SupabaseManager", "  [$index]: ID=${data.id}, Timp=${data.arrivalTime}, Stație=${data.stopName}")
            }

            // Folosim upsert în loc de insert pentru a evita erorile de duplicate key
            client.from(TABLE_NAME).upsert(dataList)
            Log.d("SupabaseManager", "✅ Upsert reușit pentru ${dataList.size} înregistrări")
            true
        } catch (e: Exception) {
            Log.e("SupabaseManager", "❌ Eroare la upsert: ${e.message}", e)
            Log.e("SupabaseManager", "   Tip eroare: ${e.javaClass.simpleName}")

            // Încearcă upsert una câte una pentru debug
            Log.d("SupabaseManager", "🔄 Încerc upsert una câte una...")
            var successCount = 0
            dataList.forEachIndexed { index, data ->
                try {
                    client.from(TABLE_NAME).upsert(data)
                    successCount++
                    Log.d("SupabaseManager", "  ✅ [$index] Succes upsert: ID=${data.id}")
                } catch (individualError: Exception) {
                    Log.e("SupabaseManager", "  ❌ [$index] Eroare upsert: ${individualError.message} pentru ID=${data.id}")
                }
            }
            Log.d("SupabaseManager", "📊 Rezultat individual: $successCount/${dataList.size} reușite")
            successCount > 0 // Returnează true dacă măcar una a reușit
        }
    }
}