package app.realvirtuality.landlord.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.realvirtuality.landlord.R
import app.realvirtuality.landlord.data.Billing
import app.realvirtuality.landlord.data.GameVM
import app.realvirtuality.landlord.ui.screens.*
import app.realvirtuality.landlord.ui.theme.Brand
import app.realvirtuality.landlord.ui.theme.liquidGlass
import app.realvirtuality.landlord.ui.theme.specularSweep
import java.text.NumberFormat

@Composable
fun HooderApp(billing: Billing) {
    val vm: GameVM = viewModel()
    val ready by vm.ready.collectAsState()
    val connecting by vm.connecting.collectAsState()
    LaunchedEffect(Unit) {
        vm.authenticate()
        billing.onGrant = { kind, pid, tok -> vm.grantPlayPurchase(kind, pid, tok) }
    }

    if (!ready) LockScreen(connecting) { vm.authenticate() }
    else GameScaffold(vm, billing)
}

private data class Tab(val labelRes: Int, val icon: ImageVector)

@Composable
private fun GameScaffold(vm: GameVM, billing: Billing) {
    var sel by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Tab(R.string.tab_map, Icons.Filled.Map),
        Tab(R.string.tab_market, Icons.Filled.Storefront),
        Tab(R.string.tab_portfolio, Icons.Filled.BusinessCenter),
        Tab(R.string.tab_forex, Icons.Filled.CurrencyExchange),
        Tab(R.string.tab_store, Icons.Filled.ShoppingBag),
        Tab(R.string.tab_rankings, Icons.Filled.EmojiEvents),
        Tab(R.string.tab_settings, Icons.Filled.Settings),
    )
    Scaffold(
        containerColor = Brand.bg,
        bottomBar = {
            NavigationBar(containerColor = Brand.surface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = sel == i,
                        onClick = { sel = i },
                        icon = { Icon(t.icon, null) },
                        label = { Text(stringResource(t.labelRes), fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Brand.primary, selectedTextColor = Brand.primary,
                            indicatorColor = Brand.primary.copy(alpha = 0.16f),
                            unselectedIconColor = Brand.textMuted, unselectedTextColor = Brand.textMuted),
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (sel) {
                0 -> MapScreen(vm)
                1 -> MarketScreen(vm)
                2 -> PortfolioScreen(vm)
                3 -> ForexScreen(vm)
                4 -> StoreScreen(vm, billing)
                5 -> RankingsScreen(vm)
                else -> SettingsScreen(vm)
            }
            HudBar(vm, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun HudBar(vm: GameVM, modifier: Modifier = Modifier) {
    val cash by vm.cash.collectAsState()
    val vip by vm.isVip.collectAsState()
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)
            .liquidGlass(18).specularSweep(18).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (vip) "Hooder 👑" else "Hooder", color = Brand.text,
            fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(money(cash), color = Brand.gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(stringResource(R.string.cash), color = Brand.gold.copy(alpha = 0.8f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun LockScreen(connecting: Boolean, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(40.dp)) {
            Icon(Icons.Outlined.Lock, null, tint = Brand.primary, modifier = Modifier.size(60.dp))
            Text("Hooder", color = Brand.text, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            Text(stringResource(if (connecting) R.string.auth_checking else R.string.auth_required),
                color = Brand.textSub, fontSize = 18.sp)
            Text(stringResource(R.string.auth_online_note), color = Brand.textMuted,
                textAlign = TextAlign.Center, fontSize = 14.sp)
            if (connecting) CircularProgressIndicator(color = Brand.primary)
            else Button(onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.primary),
                shape = RoundedCornerShape(99.dp)) { Text(stringResource(R.string.retry)) }
        }
    }
}

fun money(v: Double): String {
    val a = kotlin.math.abs(v)
    val s = when {
        a >= 1e9 -> "%.1fB".format(v / 1e9)
        a >= 1e6 -> "%.1fM".format(v / 1e6)
        a >= 1e3 -> "%.0fK".format(v / 1e3)
        else -> NumberFormat.getIntegerInstance().format(v.toLong())
    }
    return "\$$s"
}
