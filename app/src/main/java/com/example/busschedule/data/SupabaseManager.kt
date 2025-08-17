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
     * Inserează mai multe înregistrări într-o singură operație
     */
    suspend fun insertNotes(dataList: List<BusScheduleSerializable>): Boolean {
        return try {
            Log.d("SupabaseManager", "🔄 Încerc să fac upsert pentru ${dataList.size} înregistrări...")
            dataList.forEachIndexed { index, data ->
                Log.d("SupabaseManager", "  [$index]: ID=${data.id}, Timp=${data.arrivalTime}, Stație=${data.stopName}")
            }

            // Întâi încercăm upsert cu toate câmpurile (inclusiv id)
            client.from(TABLE_NAME).upsert(dataList)
            Log.d("SupabaseManager", "✅ Upsert reușit pentru ${dataList.size} înregistrări")
            true
        } catch (e: Exception) {
            Log.e("SupabaseManager", "❌ Eroare la upsert: ${e.message}", e)
            Log.e("SupabaseManager", "   Tip eroare: ${e.javaClass.simpleName}")

            // Fallback: încearcă inserare fără coloana 'id'
            return try {
                val fallback = dataList.map { mapOf(
                    "stop_name" to it.stopName,
                    "arrival_time" to it.arrivalTime
                ) }
                Log.d("SupabaseManager", "🔁 Fallback insert fără 'id' pentru ${fallback.size} înregistrări...")
                client.from(TABLE_NAME).insert(fallback)
                Log.d("SupabaseManager", "✅ Insert fallback reușit")
                true
            } catch (fallbackError: Exception) {
                Log.e("SupabaseManager", "❌ Eroare și la fallback insert: ${fallbackError.message}", fallbackError)

                // Ultima încercare: una câte una fără 'id' pentru diagnostic
                var successCount = 0
                dataList.forEachIndexed { index, data ->
                    try {
                        val item = mapOf(
                            "stop_name" to data.stopName,
                            "arrival_time" to data.arrivalTime
                        )
                        client.from(TABLE_NAME).insert(item)
                        successCount++
                        Log.d("SupabaseManager", "  ✅ [$index] Succes insert fallback")
                    } catch (individualError: Exception) {
                        Log.e("SupabaseManager", "  ❌ [$index] Eroare insert fallback: ${individualError.message}")
                    }
                }
                successCount > 0
            }
        }
    }
}