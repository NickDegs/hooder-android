package app.realvirtuality.landlord.data

import app.realvirtuality.landlord.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

// ── Sunucu + Mapbox API (iOS Hooder backend'iyle birebir) ───────────────────────
object Api {
    private const val BASE = BuildConfig.API_BASE
    private const val APP_KEY = BuildConfig.HOODER_APP_KEY
    private const val MAPBOX = BuildConfig.MAPBOX_TOKEN
    private val JSON = "application/json".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()

    @Volatile var token: String? = null

    private fun req(path: String): Request.Builder =
        Request.Builder().url("$BASE/$path").header("X-Hooder-Key", APP_KEY)
            .apply { token?.let { header("X-Auth-Token", it) } }

    private fun bodyOf(o: JSONObject) = o.toString().toRequestBody(JSON)

    private fun call(b: Request.Builder): JSONObject? = try {
        client.newCall(b.build()).execute().use { r ->
            val s = r.body?.string().orEmpty()
            if (r.isSuccessful && s.isNotEmpty()) JSONObject(s) else null
        }
    } catch (_: Exception) { null }

    // ── Kimlik (anonim cihaz) — token olmadan oyun açılmaz ──────────────────────
    suspend fun anon(deviceId: String, integrityToken: String? = null): String? = withContext(Dispatchers.IO) {
        val o = JSONObject().put("device_id", deviceId).put("platform", "android")
        integrityToken?.let { o.put("integrity_token", it) }
        val r = call(req("anon").post(bodyOf(o)))
        token = r?.optString("token")?.ifEmpty { null }
        token
    }

    // ── Sunucu cüzdan (otoriter): nakit + mülk + fx + vip ───────────────────────
    suspend fun wallet(): Wallet? = withContext(Dispatchers.IO) {
        val r = call(req("wallet").get()) ?: return@withContext null
        val owned = r.optJSONArray("owned") ?: JSONArray()
        val ownedList = (0 until owned.length()).map {
            val x = owned.getJSONObject(it)
            OwnedItem(x.optString("id"), x.optDouble("price"), x.optDouble("income"))
        }
        val fxObj = r.optJSONObject("fx") ?: JSONObject()
        val fx = fxObj.keys().asSequence().associateWith {
            val p = fxObj.getJSONObject(it); FxPosition(p.optDouble("units"), p.optDouble("costUSD"))
        }
        Wallet(r.optDouble("cash"), ownedList, fx, r.optBoolean("vip"))
    }

    suspend fun buy(id: String, price: Double): Double? = withContext(Dispatchers.IO) {
        call(req("wallet/buy").post(bodyOf(JSONObject().put("id", id).put("price", price))))
            ?.takeIf { it.optBoolean("ok") }?.optDouble("cash")
    }
    suspend fun sell(id: String): Double? = withContext(Dispatchers.IO) {
        call(req("wallet/sell").post(bodyOf(JSONObject().put("id", id))))
            ?.takeIf { it.optBoolean("ok") }?.optDouble("cash")
    }
    suspend fun fxTrade(code: String, usd: Double, buy: Boolean, rate: Double): Double? = withContext(Dispatchers.IO) {
        call(req("wallet/fx").post(bodyOf(
            JSONObject().put("code", code).put("usd", usd).put("buy", buy).put("rate", rate))))
            ?.takeIf { it.optBoolean("ok") }?.optDouble("cash")
    }

    // ── Tek dünya ekonomisi (piyasa endeksi + döviz kurları) ────────────────────
    suspend fun economy(): Pair<Double, Map<String, Double>>? = withContext(Dispatchers.IO) {
        val r = call(req("economy").get()) ?: return@withContext null
        val rt = r.optJSONObject("rates") ?: JSONObject()
        val rates = rt.keys().asSequence().associateWith { rt.optDouble(it) }
        r.optDouble("index", 1.0) to rates
    }
    suspend fun recordTrade(buy: Boolean, magnitude: Double) { withContext(Dispatchers.IO) {
        call(req("economy/trade").post(bodyOf(JSONObject().put("buy", buy).put("magnitude", magnitude))))
    } }

    // ── Mülkler: Mapbox tilequery + reverse geocode → satın alınabilir Property ──
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun hash01(s: String): Double {
        var h = 2166136261u
        for (b in s.toByteArray()) { h = h xor b.toUInt(); h *= 16777619u }
        return h.toDouble() / UInt.MAX_VALUE.toDouble()
    }
    private val classMap = mapOf(
        "lodging" to Triple(Category.hotel, 90_000_000.0, 5),
        "commercial" to Triple(Category.retail, 28_000_000.0, 3),
        "food_and_drink" to Triple(Category.retail, 14_000_000.0, 2),
        "office" to Triple(Category.office, 65_000_000.0, 4),
        "landmark" to Triple(Category.landmark, 160_000_000.0, 5),
        "historic" to Triple(Category.landmark, 140_000_000.0, 5),
        "museum" to Triple(Category.landmark, 120_000_000.0, 5),
        "park_like" to Triple(Category.park, 18_000_000.0, 2),
        "sport_and_leisure" to Triple(Category.stadium, 110_000_000.0, 4),
        "general" to Triple(Category.building, 30_000_000.0, 2),
    )

    suspend fun fetchArea(lat: Double, lng: Double): List<Property> = withContext(Dispatchers.IO) {
        if (MAPBOX.startsWith("pk.placeholder")) return@withContext emptyList()
        val ctx = reverseGeocode(lat, lng)
        val url = "https://api.mapbox.com/v4/mapbox.mapbox-streets-v8/tilequery/$lng,$lat.json" +
            "?radius=1000&limit=50&dedupe=true&layers=poi_label,building&access_token=$MAPBOX"
        val feats = try {
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                JSONObject(r.body?.string().orEmpty()).optJSONArray("features") ?: JSONArray()
            }
        } catch (_: Exception) { JSONArray() }
        val out = ArrayList<Property>()
        for (i in 0 until feats.length()) convert(feats.getJSONObject(i), ctx)?.let { out.add(it) }
        out
    }

    private data class Area(val district: String, val city: String, val country: String)
    private fun reverseGeocode(lat: Double, lng: Double): Area {
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json" +
            "?types=country,region,district,place,locality,neighborhood&access_token=$MAPBOX"
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                val fs = JSONObject(r.body?.string().orEmpty()).optJSONArray("features") ?: JSONArray()
                fun pick(t: String): String? {
                    for (i in 0 until fs.length()) {
                        val f = fs.getJSONObject(i)
                        val types = f.optJSONArray("place_type") ?: continue
                        for (j in 0 until types.length()) if (types.getString(j) == t) return f.optString("text")
                    }
                    return null
                }
                Area(pick("neighborhood") ?: pick("locality") ?: pick("district") ?: "Çevre",
                     pick("place") ?: pick("region") ?: "Bölge", "")
            }
        } catch (_: Exception) { Area("Çevre", "Bölge", "") }
    }

    private fun convert(f: JSONObject, area: Area): Property? {
        val geom = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return null
        if (geom.length() < 2) return null
        val lng = geom.getDouble(0); val lat = geom.getDouble(1)
        val props = f.optJSONObject("properties") ?: JSONObject()
        val isBuilding = props.optJSONObject("tilequery")?.optString("layer") == "building"
        val name = props.optString("name_en").ifEmpty { props.optString("name") }
        val seed = hash01("${name.ifEmpty { "b" }}:$lat:$lng")
        fun round100k(v: Double) = (v / 100_000.0).roundToLong() * 100_000.0
        return if (isBuilding) {
            val h = props.optDouble("height", 0.0)
            val kind = if (h > 60) "Rezidans" else if (h > 25) "Plaza" else "Apartman"
            val cat = if (h > 25) Category.office else Category.building
            val base = if (h > 60) 28_000_000.0 else if (h > 25) 22_000_000.0 else 7_000_000.0
            val price = maxOf(2_000_000.0, round100k(base * (0.6 + seed * 1.6)))
            Property("bld_${"%.5f".format(lat)}_${"%.5f".format(lng)}",
                "${area.district} $kind No.${(seed * 90).toInt() + 1}", area.district, area.city,
                cat, price, maxOf(1500.0, (price * 0.0009)), if (cat == Category.office) 3 else 1, lat, lng)
        } else {
            if (name.isEmpty()) return null
            val cls = props.optString("class").ifEmpty { "general" }
            val meta = classMap[cls] ?: classMap["general"]!!
            val price = round100k(meta.second * (0.55 + seed * 1.7))
            Property("loc_${"%.5f".format(lat)}_${"%.5f".format(lng)}_$cls",
                name, area.district, area.city, meta.first, price,
                maxOf(1000.0, (price * 0.0009)), minOf(5, meta.third + if (seed > 0.8) 1 else 0), lat, lng)
        }
    }
}
