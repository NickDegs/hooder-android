package app.realvirtuality.landlord.ui.screens

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import app.realvirtuality.landlord.data.Billing
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
    val fx by vm.fx.collectAsState()
    val flags = mapOf("EUR" to "🇪🇺","GBP" to "🇬🇧","JPY" to "🇯🇵","TRY" to "🇹🇷",
        "CNY" to "🇨🇳","AED" to "🇦🇪","CHF" to "🇨🇭","CAD" to "🇨🇦")
    ScreenScaffold("Döviz") {
        Text("Piyasa endeksi: ${"%.4f".format(idx)}", color = Brand.gold,
            modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(rates.entries.toList()) { (code, rate) ->
                val pos = fx[code]
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
                    .liquidGlass(16).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${flags[code] ?: "💱"}  $code", color = Brand.text, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("1 USD = ${"%.4f".format(rate)}", color = Brand.textSub)
                    }
                    if (pos != null && pos.units > 0) {
                        val value = if (rate > 0) pos.units / rate else 0.0
                        Text("Pozisyon: ${"%.0f".format(pos.units)} $code  ≈ ${money(value)}",
                            color = Brand.green, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.buyFx(code, 1_000_000.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Brand.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(99.dp)) { Text("Al \$1M", fontSize = 12.sp) }
                        if (pos != null && pos.units > 0)
                            Button(onClick = { vm.sellFx(code) },
                                colors = ButtonDefaults.buttonColors(containerColor = Brand.orange),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(99.dp)) { Text("Sat", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreScreen(vm: GameVM, billing: Billing) {
    val list by billing.items.collectAsState()
    val vip by vm.isVip.collectAsState()
    val activity = LocalContext.current as? Activity
    ScreenScaffold("Mağaza") {
        if (vip) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF5A4410), Color(0xFF2A2008))))
            .border(1.dp, Brand.gold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("👑", fontSize = 24.sp); Spacer(Modifier.width(10.dp))
            Column { Text("VIP Üye", color = Brand.gold, fontWeight = FontWeight.Bold)
                Text("+%25 gelir · özel mülkler · altın tema", color = Brand.textSub, fontSize = 12.sp) }
        }
        if (list.isEmpty())
            Text("Ürünler yükleniyor…", color = Brand.textMuted, modifier = Modifier.padding(16.dp))
        else LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(list) { item ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
                    .liquidGlass(16).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${if (item.sub) "👑" else "💰"}  ${item.title}",
                        color = Brand.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Button(onClick = { activity?.let { billing.buy(it, item.id) } },
                        colors = ButtonDefaults.buttonColors(containerColor = Brand.primary),
                        shape = RoundedCornerShape(99.dp)) {
                        Text(item.price.ifEmpty { "Satın Al" }, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RankingsScreen(vm: GameVM) {
    val leaders by vm.leaders.collectAsState()
    LaunchedEffect(Unit) { vm.loadLeaderboard() }
    ScreenScaffold("Sıralama") {
        if (leaders.isEmpty())
            Text("Liderlik yükleniyor…", color = Brand.textMuted, modifier = Modifier.padding(16.dp))
        else LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(leaders) { i, l ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
                    .liquidGlass(16).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${i + 1}", color = if (i < 3) Brand.gold else Brand.textMuted,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                    Text(l.name, color = Brand.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(money(l.netWorth), color = Brand.gold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: GameVM) {
    val cash by vm.cash.collectAsState()
    val langs = listOf("en" to "English", "tr" to "Türkçe", "es" to "Español", "fr" to "Français",
        "de" to "Deutsch", "it" to "Italiano", "pt" to "Português", "ru" to "Русский", "ar" to "العربية",
        "zh" to "中文", "ja" to "日本語", "ko" to "한국어", "uk" to "Українська", "hi" to "हिन्दी",
        "az" to "Azərbaycan", "fa" to "فارسی")
    ScreenScaffold("Ayarlar") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth().liquidGlass(16).padding(14.dp)) {
                Text("Dil / Language", color = Brand.text, fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    langs.forEach { (code, name) ->
                        Text(name, color = Brand.primary, fontSize = 13.sp,
                            modifier = Modifier.clip(RoundedCornerShape(99.dp))
                                .background(Brand.primary.copy(alpha = 0.14f))
                                .clickable {
                                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
            }
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
