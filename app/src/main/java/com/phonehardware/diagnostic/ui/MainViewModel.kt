package com.phonehardware.diagnostic.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonehardware.diagnostic.core.*
import com.phonehardware.diagnostic.data.ResultRepository
import com.phonehardware.diagnostic.hardware.ActiveDiagnostics
import com.phonehardware.diagnostic.hardware.HardwareSnapshot
import com.phonehardware.diagnostic.hardware.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: ResultRepository,
    private val passive: HardwareSnapshot,
    private val active: ActiveDiagnostics,
    private val exporter: ReportExporter
): ViewModel() {
    val sessions = repository.sessions.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val darkMode = repository.darkMode.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),true)
    val results = MutableStateFlow<List<DiagnosticResult>>(emptyList())
    val activeId = MutableStateFlow<String?>(null)
    val currentSession = MutableStateFlow<String?>(null)
    val error = MutableStateFlow<String?>(null)
    val quickScanProgress = MutableStateFlow(0)
    private var job: Job? = null

    fun scan() {
        if(job?.isActive == true) return
        job = viewModelScope.launch {
            activeId.value = "quick_scan"; quickScanProgress.value = 0
            try {
                val session = repository.newSession(); currentSession.value = session
                results.value = emptyList()
                TestCatalog.passive.forEachIndexed { idx,spec ->
                    ensureActive()
                    val result = withContext(Dispatchers.Default) { passive.read(spec.id) }
                    save(session,result)
                    quickScanProgress.value = idx+1
                }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { error.value = "Scan stopped: ${e.javaClass.simpleName}" }
            finally { activeId.value = null }
        }
    }
    fun runOne(id: String) {
        if(job?.isActive == true) return
        job = viewModelScope.launch {
            activeId.value = id
            try {
                val session = currentSession.value ?: repository.newSession().also { currentSession.value = it }
                val result = if(TestCatalog.byId(id).kind == TestKind.PASSIVE)
                    withContext(Dispatchers.Default) { passive.read(id) } else active.run(id)
                save(session,result)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { error.value = "Test unavailable: ${e.javaClass.simpleName}" }
            finally { activeId.value = null }
        }
    }
    fun store(result: DiagnosticResult) {
        viewModelScope.launch {
            try {
                val session = currentSession.value ?: repository.newSession().also { currentSession.value = it }
                save(session,result)
            } catch(e: Exception) { error.value = "Could not store result: ${e.javaClass.simpleName}" }
        }
    }
    private suspend fun save(session: String, result: DiagnosticResult) {
        repository.save(session,result)
        results.value = results.value + result
    }
    fun cancel() { job?.cancel(); activeId.value = null }
    fun setDark(value: Boolean) = viewModelScope.launch { repository.setDarkMode(value) }
    fun deleteSession(id: String) = viewModelScope.launch { repository.delete(id) }
    fun clearHistory() = viewModelScope.launch { repository.clear() }
    fun savedResults(id: String): Flow<List<DiagnosticResult>> = repository.results(id)
    fun export(id: String, uri: Uri, format: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try { exporter.export(id,uri,format) }
            catch(e: Exception) { error.value = "Export unsuccessful: ${e.message}" }
        }
    }
    fun interactiveResult(id: String, success: Boolean?, method: String, metrics: List<Metric> = emptyList(), warning: String = "") {
        val spec = TestCatalog.byId(id)
        val outcome = when(success) { true -> Outcome.PASSED; false -> Outcome.FAILED; null -> Outcome.INCONCLUSIVE }
        store(DiagnosticResult(id,spec.title,spec.category,outcome,metrics,"Direct UI interaction / user confirmation",method,
            when(success) { true -> "The declared test criteria were satisfied."; false -> "The user did not observe the expected result."; null -> "The observation was insufficient to decide." },
            "Repeat the test in a controlled environment if uncertain.",warning))
    }
}
