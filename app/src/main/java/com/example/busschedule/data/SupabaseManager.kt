package com.example.busschedule.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    /** Row flexibil pentru tabelul 'notes' (coloana id poate lipsi sau fi numită diferit) */
    @Serializable
    private data class NoteRow(
        val id: Int? = null,
        @SerialName("arrival_time") val arrivalTime: String? = null,
        @SerialName("stop_name") val stopName: String? = null
    )

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
     * Obține toate înregistrările din tabelul notes; mapează flexibil la structura internă
     */
    suspend fun getAllNotes(): List<BusScheduleSerializable> {
        return try {
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
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Inserează mai multe înregistrări într-o singură operație; nu trimite 'id' pentru a lăsa DB să-l autogenereze
     */
    suspend fun insertNotes(dataList: List<BusScheduleSerializable>): Boolean {
        return try {
            Log.d("SupabaseManager", "🔄 Insert pentru ${'$'}{dataList.size} înregistrări (fără id)...")
            // Trimite doar câmpurile necesare DB-ului
            val rows = dataList.map {
                NoteRow(
                    arrivalTime = it.arrivalTime,
                    stopName = it.stopName
                )
            }
            client.from(TABLE_NAME).insert(rows)
            Log.d("SupabaseManager", "✅ Insert reușit pentru ${'$'}{rows.size} înregistrări")
            true
        } catch (e: Exception) {
            Log.e("SupabaseManager", "❌ Eroare la insert: ${'$'}{e.message}", e)
            false
        }
    }

    /**
     * Inserează o singură înregistrare (fără id)
     */
    suspend fun insertNote(stopName: String, arrivalTime: String): Boolean {
        return try {
            val row = NoteRow(arrivalTime = arrivalTime, stopName = stopName)
            client.from(TABLE_NAME).insert(row)
            true
        } catch (e: Exception) {
            Log.e("SupabaseManager", "❌ Eroare la insert single: ${'$'}{e.message}", e)
            false
        }
    }
}