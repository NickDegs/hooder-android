package app.realvirtuality.landlord.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

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
    val leaders = MutableStateFlow<List<Leader>>(emptyList())
    val fx = MutableStateFlow<Map<String, FxPosition>>(emptyMap())

    private val prefs = app.getSharedPreferences("hooder", Context.MODE_PRIVATE)
    private val registry = LinkedHashMap<String, Property>()
    private val fetched = HashSet<String>()

    private fun deviceId(): String {
        prefs.getString("device_id", null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", id).apply(); return id
    }

    // ── KİLİT: internet + Play Integrity + sunucu kimliği ZORUNLU ────────────────
    fun authenticate() {
        connecting.value = true
        viewModelScope.launch {
            val integrity = requestIntegrity()      // korsan/tamper APK → sunucu reddeder
            val tok = Api.anon(deviceId(), integrity)
            ready.value = tok != null
            connecting.value = false
            if (tok != null) startup()
        }
    }

    // Play Integrity token (classic istek) — nonce ile; Play'den gelmeyen/değiştirilmiş
    // APK'da Google geçerli token vermez → backend reddeder.
    private suspend fun requestIntegrity(): String? = suspendCancellableCoroutine { cont ->
        try {
            val mgr = IntegrityManagerFactory.create(getApplication())
            val nonce = java.lang.Long.toHexString(System.nanoTime()) +
                UUID.randomUUID().toString().replace("-", "")
            mgr.requestIntegrityToken(IntegrityTokenRequest.builder().setNonce(nonce).build())
                .addOnSuccessListener { r -> if (cont.isActive) cont.resume(r.token()) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        } catch (_: Exception) { if (cont.isActive) cont.resume(null) }
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
        fx.value = w.fx
    }

    // Döviz al (usd nakitle) / sat (pozisyonu kapat) — SUNUCU otoriter
    fun buyFx(code: String, usd: Double) {
        val rate = rates.value[code] ?: return
        if (usd <= 0 || cash.value < usd) return
        cash.value -= usd
        viewModelScope.launch {
            Api.fxTrade(code, usd, true, rate)?.let { cash.value = it }
            syncWallet()
        }
    }
    fun sellFx(code: String) {
        val rate = rates.value[code] ?: return
        if ((fx.value[code]?.units ?: 0.0) <= 0) return
        viewModelScope.launch {
            Api.fxTrade(code, 0.0, false, rate)?.let { cash.value = it }
            syncWallet()
        }
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

    fun loadLeaderboard() { viewModelScope.launch { leaders.value = Api.leaderboard() } }

    // Yer ara → koordinat bul → o bölgeyi yükle → haritayı oraya uçur (onFound)
    fun search(query: String, onFound: (Double, Double) -> Unit) {
        viewModelScope.launch {
            Api.geocode(query)?.let { (lat, lng) -> loadArea(lat, lng); onFound(lat, lng) }
        }
    }

    // Play Billing satın alımı sunucuda doğrulanır → nakit/VIP sunucudan güncellenir
    fun grantPlayPurchase(kind: String, productId: String, token: String) {
        viewModelScope.launch {
            val c = Api.playPurchase(kind, productId, token)
            if (c != null && c > 0) cash.value = c
            syncWallet()
        }
    }

    // Rakibe teklif: kabul eşiği fiyatın %15 üstü. 0=yetersiz/geçersiz, 1=kabul, 2=red(düşük)
    fun makeOffer(p: Property, amount: Double, onResult: (Int) -> Unit) {
        val floor = livePrice(p) * 1.15
        if (amount < floor) { onResult(2); return }
        if (cash.value < amount) { onResult(0); return }
        cash.value -= amount
        ownedIds.value = ownedIds.value + p.id
        viewModelScope.launch {
            val nc = Api.buy(p.id, amount)
            if (nc != null) { cash.value = nc; Api.recordTrade(true, amount) } else syncWallet()
            onResult(if (nc != null) 1 else 0)
        }
    }

    fun rivalOwner(p: Property): String? = if (isOwned(p.id)) null else Rivals.owner(p)

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
