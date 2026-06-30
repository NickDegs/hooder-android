package app.realvirtuality.landlord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.realvirtuality.landlord.data.GameVM
import app.realvirtuality.landlord.data.Property
import app.realvirtuality.landlord.ui.money
import app.realvirtuality.landlord.ui.theme.Brand
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

@OptIn(com.mapbox.maps.MapboxExperimental::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(vm: GameVM) {
    val props by vm.properties.collectAsState()
    val owned by vm.ownedIds.collectAsState()
    var selected by remember { mutableStateOf<Property?>(null) }
    var showList by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(28.9784, 41.0082))
            zoom(13.5); pitch(45.0)
        }
    }

    Box(Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewport,
            style = { MapStyle(style = Style.SATELLITE_STREETS) },
        ) {
            MapEffect(Unit) { mapView: MapView ->
                mapView.mapboxMap.subscribeMapIdle {
                    val c = mapView.mapboxMap.cameraState.center
                    vm.loadArea(c.latitude(), c.longitude())
                }
            }
            props.take(300).forEach { p ->
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(p.lng, p.lat))
                        allowOverlap(false)
                    }
                ) {
                    Pill(p, owned.contains(p.id), vm) { selected = p }
                }
            }
        }

        // Üst: yer arama çubuğu (HUD'un altında)
        Row(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 76.dp, start = 14.dp, end = 14.dp)
                .fillMaxWidth().liquidGlass(18).padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = Brand.textMuted)
            TextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Yer ara: şehir, ülke…", color = Brand.textMuted) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Brand.text, unfocusedTextColor = Brand.text),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    vm.search(query) { lat, lng ->
                        viewport.flyTo(CameraOptions.Builder()
                            .center(Point.fromLngLat(lng, lat)).zoom(13.5).pitch(45.0).build())
                    }
                }),
                modifier = Modifier.weight(1f)
            )
        }

        // Sol alt: bölge listesi
        Button(onClick = { showList = true },
            colors = ButtonDefaults.buttonColors(containerColor = Brand.surface),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 24.dp)) {
            Icon(Icons.Filled.List, null, tint = Brand.primary)
            Spacer(Modifier.width(6.dp)); Text("Liste", color = Brand.primary)
        }
    }

    selected?.let { p ->
        BuySheet(p, vm, owned.contains(p.id)) { selected = null }
    }

    if (showList) {
        val top = remember(props) { props.sortedByDescending { it.price }.take(60) }
        ModalBottomSheet(onDismissRequest = { showList = false }, containerColor = Brand.surface) {
            LazyColumn(Modifier.padding(bottom = 24.dp)) {
                items(top) { p ->
                    Row(Modifier.fillMaxWidth().clickable { showList = false; selected = p }
                        .padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.category.emoji, fontSize = 20.sp); Spacer(Modifier.width(10.dp))
                        Text(p.name, color = Brand.text, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(money(vm.livePrice(p)), color = if (owned.contains(p.id)) Brand.green else Brand.gold,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Pill(p: Property, isOwned: Boolean, vm: GameVM, onTap: () -> Unit) {
    val accent = if (isOwned) Brand.green else Brand.text
    Row(
        Modifier.clip(RoundedCornerShape(99.dp))
            .background(Brush.verticalGradient(
                if (isOwned) listOf(Color(0xCC1A4A2E), Color(0xEE08200F))
                else listOf(Color(0xBB28324F), Color(0xEE080B16))))
            .clickable { onTap() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(p.category.emoji, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Text(money(vm.livePrice(p)), color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuySheet(p: Property, vm: GameVM, isOwned: Boolean, onClose: () -> Unit) {
    var msg by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Brand.surface) {
        Column(Modifier.padding(20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${p.category.emoji}  ${p.name}", color = Brand.text,
                fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("${p.neighborhood} · ${p.city}", color = Brand.textSub, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat("Fiyat", money(vm.livePrice(p)))
                Stat("Günlük", money(p.incomePerDay))
                Stat("Prestij", "★".repeat(p.prestige))
            }
            msg?.let { Text(it, color = Brand.green, fontSize = 14.sp) }
            if (!isOwned) Button(
                onClick = { vm.buy(p) { ok -> msg = if (ok) "✓ Satın alındı" else "Yetersiz bakiye" } },
                colors = ButtonDefaults.buttonColors(containerColor = Brand.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Satın Al · ${money(vm.livePrice(p))}") }
            else Text("✓ Bu mülk senin", color = Brand.green, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column { Text(value, color = Brand.text, fontWeight = FontWeight.Bold)
        Text(label, color = Brand.textMuted, fontSize = 11.sp) }
}
