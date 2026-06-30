package app.realvirtuality.landlord.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.realvirtuality.landlord.data.GameVM
import app.realvirtuality.landlord.data.Property
import app.realvirtuality.landlord.ui.money
import app.realvirtuality.landlord.ui.theme.Brand
import app.realvirtuality.landlord.ui.theme.liquidGlass

@Composable
private fun ScreenScaffold(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 76.dp)) {
        Text(title, color = Brand.text, fontWeight = FontWeight.Bold, fontSize = 22.sp,
            modifier = Modifier.padding(16.dp))
        content()
    }
}

@Composable
private fun PropRow(p: Property, vm: GameVM, owned: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
        .liquidGlass(16).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(p.category.emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, color = Brand.text, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${p.neighborhood} · ${money(p.incomePerDay)}/gün", color = Brand.textMuted, fontSize = 12.sp)
        }
        if (owned) Text("✓", color = Brand.green, fontWeight = FontWeight.Bold)
        else Button(onClick = { vm.buy(p) {} },
            colors = ButtonDefaults.buttonColors(containerColor = Brand.primary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(99.dp)) { Text(money(vm.livePrice(p)), fontSize = 12.sp) }
    }
}

@Composable
fun MarketScreen(vm: GameVM) {
    val props by vm.properties.collectAsState()
    val owned by vm.ownedIds.collectAsState()
    val list = remember(props) { props.sortedByDescending { it.price }.take(120) }
    ScreenScaffold("Piyasa") {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(list) { p -> PropRow(p, vm, owned.contains(p.id)) }
        }
    }
}

@Composable
fun PortfolioScreen(vm: GameVM) {
    val props by vm.properties.collectAsState()
    val owned by vm.ownedIds.collectAsState()
    val mine = remember(props, owned) { props.filter { owned.contains(it.id) } }
    ScreenScaffold("Portföy") {
        if (mine.isEmpty()) Text("Henüz mülkün yok. Haritadan satın al.",
            color = Brand.textMuted, modifier = Modifier.padding(16.dp))
        else LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(mine) { p -> PropRow(p, vm, true) }
        }
    }
}

@Composable
fun ForexScreen(vm: GameVM) {
    val rates by vm.rates.collectAsState()
    val idx by vm.marketIndex.collectAsState()
    val flags = mapOf("EUR" to "🇪🇺","GBP" to "🇬🇧","JPY" to "🇯🇵","TRY" to "🇹🇷",
        "CNY" to "🇨🇳","AED" to "🇦🇪","CHF" to "🇨🇭","CAD" to "🇨🇦")
    ScreenScaffold("Döviz") {
        Text("Piyasa endeksi: ${"%.4f".format(idx)}", color = Brand.gold,
            modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(rates.entries.toList()) { (code, rate) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
                    .liquidGlass(16).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${flags[code] ?: "💱"}  $code", color = Brand.text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("1 USD = ${"%.4f".format(rate)}", color = Brand.textSub)
                }
            }
        }
    }
}

@Composable
fun StoreScreen(vm: GameVM) {
    val packs = listOf("Başlangıç" to "1.5M", "Yatırımcı" to "9M", "Tycoon" to "30M",
        "Mogul" to "90M", "İmparatorluk" to "250M")
    ScreenScaffold("Mağaza") {
        Text("Nakit paketleri (Play Billing yakında bağlanacak)",
            color = Brand.textMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(packs) { (name, amt) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
                    .liquidGlass(16).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💰  $name", color = Brand.text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("+\$$amt", color = Brand.gold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RankingsScreen(vm: GameVM) {
    ScreenScaffold("Sıralama") {
        Text("Liderlik tablosu yakında.", color = Brand.textMuted, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun SettingsScreen(vm: GameVM) {
    val cash by vm.cash.collectAsState()
    ScreenScaffold("Ayarlar") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard("Sunucu-Otoriter Ekonomi",
                "Nakit, mülk ve gelir sunucuda tutulur — hile/korsan kazanç sağlamaz.")
            InfoCard("Çevrimiçi Kimlik", "Oyun kimlik/token olmadan ve internetsiz açılmaz.")
            InfoCard("Nakit", money(cash))
            Text("Hooder · Android (Kotlin · Jetpack Compose)",
                color = Brand.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Column(Modifier.fillMaxWidth().liquidGlass(16).padding(14.dp)) {
        Text(title, color = Brand.text, fontWeight = FontWeight.SemiBold)
        Text(body, color = Brand.textSub, fontSize = 13.sp)
    }
}
