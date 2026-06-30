package app.realvirtuality.landlord.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Oyun durumu — sunucu-otoriter; kimlik alınana kadar oyun KİLİTLİ (offline/korsan engeli)
class GameVM(app: Application) : AndroidViewModel(app) {

    val ready = MutableStateFlow(false)        // sunucu kimliği alındı mı → kilit
    val connecting = MutableStateFlow(true)
    val cash = MutableStateFlow(0.0)
    val ownedIds = MutableStateFlow<Set<String>>(emptySet())
    val isVip = MutableStateFlow(false)
    val properties: MutableStateFlow<List<Property>> = MutableStateFlow(emptyList())
    val rates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val marketIndex = MutableStateFlow(1.0)

    private val prefs = app.getSharedPreferences("hooder", Context.MODE_PRIVATE)
    private val registry = LinkedHashMap<String, Property>()
    private val fetched = HashSet<String>()

    private fun deviceId(): String {
        prefs.getString("device_id", null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", id).apply(); return id
    }

    // ── KİLİT: internet + sunucu kimliği ZORUNLU (Play Integrity güvenlik fazında eklenecek) ──
    fun authenticate() {
        connecting.value = true
        viewModelScope.launch {
            val tok = Api.anon(deviceId())
            ready.value = tok != null
            connecting.value = false
            if (tok != null) startup()
        }
    }

    private fun startup() {
        viewModelScope.launch { syncWallet() }
        viewModelScope.launch { economyLoop() }
        viewModelScope.launch {
            val added = Api.fetchArea(41.0082, 28.9784)   // İstanbul başlangıç
            ingest(added)
        }
    }

    suspend fun syncWallet() {
        val w = Api.wallet() ?: return
        cash.value = w.cash
        ownedIds.value = w.owned.map { it.id }.toSet()
        isVip.value = w.vip
    }

    private suspend fun economyLoop() {
        while (true) {
            Api.economy()?.let { (idx, r) -> marketIndex.value = idx; if (r.isNotEmpty()) rates.value = r }
            kotlinx.coroutines.delay(3000)
        }
    }

    fun loadArea(lat: Double, lng: Double) {
        val key = "${"%.3f".format(lat)},${"%.3f".format(lng)}"
        if (!fetched.add(key)) return
        viewModelScope.launch { ingest(Api.fetchArea(lat, lng)) }
    }

    private fun ingest(list: List<Property>) {
        if (list.isEmpty()) return
        for (p in list) registry.putIfAbsent(p.id, p)
        properties.value = registry.values.toList()
    }

    fun livePrice(p: Property): Double {
        val premium = 1 + ownedIds.value.size * 0.012
        return (p.price * premium * marketIndex.value).let { Math.round(it).toDouble() }
    }

    fun isOwned(id: String) = ownedIds.value.contains(id)

    fun buy(p: Property, onResult: (Boolean) -> Unit) {
        val cost = livePrice(p)
        if (isOwned(p.id) || cash.value < cost) { onResult(false); return }
        cash.value -= cost                              // iyimser
        ownedIds.value = ownedIds.value + p.id
        viewModelScope.launch {
            val newCash = Api.buy(p.id, cost)
            if (newCash != null) { cash.value = newCash; Api.recordTrade(true, cost) }
            else syncWallet()                           // sunucu reddetti → gerçeğe dön
            onResult(newCash != null)
        }
    }
}
