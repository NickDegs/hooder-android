package app.realvirtuality.landlord.data

import kotlinx.serialization.Serializable

// Mülk kategorisi (iOS ile aynı)
@Serializable
enum class Category(val emoji: String, val title: String) {
    building("🏢", "Bina"), hotel("🏨", "Otel"), office("🏬", "Ofis"),
    retail("🛍️", "Mağaza"), landmark("🗽", "Simge"), park("🌳", "Park"),
    stadium("🏟️", "Stadyum");

    companion object {
        fun from(s: String?) = entries.firstOrNull { it.name == s } ?: building
    }
}

@Serializable
data class Property(
    val id: String,
    val name: String,
    val neighborhood: String,
    val city: String,
    val category: Category,
    val price: Double,
    val incomePerDay: Double,
    val prestige: Int,
    val lat: Double,
    val lng: Double,
    val vipOnly: Boolean = false,
)

// Sunucu cüzdan yanıtı
data class Wallet(
    val cash: Double,
    val owned: List<OwnedItem>,
    val fx: Map<String, FxPosition>,
    val vip: Boolean,
)
data class OwnedItem(val id: String, val price: Double, val income: Double)
data class FxPosition(val units: Double, val costUSD: Double)
data class Leader(val name: String, val netWorth: Double)

// Rakip sahipler: artık DEVRE DIŞI — her mülk (apartman/ev/her şey) doğrudan satın alınabilir.
object Rivals {
    fun owner(p: Property): String? = null
}
