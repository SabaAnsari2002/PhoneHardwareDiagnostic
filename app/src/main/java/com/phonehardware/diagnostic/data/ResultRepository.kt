package com.phonehardware.diagnostic.data

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.phonehardware.diagnostic.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settings by preferencesDataStore("settings")

@Singleton
class ResultRepository @Inject constructor(
    private val db: LocalDatabase,
    @ApplicationContext private val context: Context
) {
    val sessions: Flow<List<SessionEntity>> = db.dao().sessions()
    val darkMode: Flow<Boolean> = context.settings.data.map { it[booleanPreferencesKey("dark_theme")] ?: true }
    suspend fun setDarkMode(value: Boolean) {
        context.settings.edit { it[booleanPreferencesKey("dark_theme")] = value }
    }
    fun results(sessionId: String): Flow<List<DiagnosticResult>> =
        db.dao().results(sessionId).map { rows -> rows.map { decode(it.json) } }

    suspend fun newSession(): String {
        val id = UUID.randomUUID().toString()
        db.dao().putSession(SessionEntity(id, "${Build.MANUFACTURER} ${Build.MODEL}", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", System.currentTimeMillis()))
        return id
    }
    suspend fun save(session: String, result: DiagnosticResult) {
        db.dao().putResult(ResultEntity(sessionId = session, testId = result.id, testName = result.name,
            category = result.category, status = result.outcome.name, json = encode(result).toString(), createdAt = result.timestamp))
    }
    suspend fun delete(session: String) = db.dao().deleteSession(session)
    suspend fun clear() = db.dao().clear()

    suspend fun exportJson(session: String): String {
        val rows = db.dao().resultsOnce(session)
        return JSONObject().put("schema", "phone-hardware-diagnostic.report")
            .put("schemaVersion", 1).put("exportTimestamp", System.currentTimeMillis())
            .put("results", JSONArray().apply { rows.forEach { put(JSONObject(it.json)) } }).toString(2)
    }
    suspend fun exportCsv(session: String): String {
        val rows = db.dao().resultsOnce(session)
        val lines = mutableListOf("test_id,category,status,metric,value,unit,evidence,timestamp,source")
        rows.forEach { row ->
            val r = decode(row.json)
            if (r.metrics.isEmpty()) lines += listOf(r.id,r.category,r.outcome.name,"","","","",r.timestamp.toString(),r.source).joinToString(",", transform = ::escapeCsv)
            r.metrics.forEach { m ->
                lines += listOf(r.id,r.category,r.outcome.name,m.name,m.value,m.unit,m.evidence.name,r.timestamp.toString(),r.source).joinToString(",", transform = ::escapeCsv)
            }
        }
        return lines.joinToString("\r\n")
    }
    private fun escapeCsv(value: String) = "\"${value.replace("\"", "\"\"")}\""

    private fun encode(r: DiagnosticResult): JSONObject = JSONObject()
        .put("id", r.id).put("name", r.name).put("category", r.category)
        .put("outcome", r.outcome.name).put("source", r.source).put("method", r.method)
        .put("explanation", r.explanation).put("nextAction", r.nextAction)
        .put("warning", r.warning).put("errorCode", r.errorCode)
        .put("timestamp", r.timestamp).put("androidApi", r.androidApi)
        .put("metrics", JSONArray().apply { r.metrics.forEach { m ->
            put(JSONObject().put("name",m.name).put("value",m.value).put("unit",m.unit).put("evidence",m.evidence.name))
        } })

    private fun decode(s: String): DiagnosticResult {
        val o = JSONObject(s)
        val metrics = o.optJSONArray("metrics") ?: JSONArray()
        return DiagnosticResult(
            id = o.getString("id"), name = o.getString("name"), category = o.getString("category"),
            outcome = Outcome.valueOf(o.getString("outcome")), source = o.getString("source"),
            method = o.getString("method"), explanation = o.getString("explanation"),
            nextAction = o.optString("nextAction"), warning = o.optString("warning"),
            errorCode = o.optString("errorCode"), timestamp = o.optLong("timestamp"),
            androidApi = o.optInt("androidApi"),
            metrics = (0 until metrics.length()).map { idx ->
                val v = metrics.getJSONObject(idx)
                Metric(v.getString("name"),v.getString("value"),v.optString("unit"), Evidence.valueOf(v.getString("evidence")))
            }
        )
    }
}
