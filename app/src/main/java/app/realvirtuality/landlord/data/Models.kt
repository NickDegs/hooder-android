package app.realvirtuality.landlord.data

// Mülk kategorisi (iOS ile aynı)
enum class Category(val emoji: String, val title: String) {
    building("🏢", "Bina"), hotel("🏨", "Otel"), office("🏬", "Ofis"),
    retail("🛍️", "Mağaza"), landmark("🗽", "Simge"), park("🌳", "Park"),
    stadium("🏟️", "Stadyum");

    companion object {
        fun from(s: String?) = entries.firstOrNull { it.name == s } ?: building
    }
}

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
