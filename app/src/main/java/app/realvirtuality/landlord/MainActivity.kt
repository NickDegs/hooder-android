package app.realvirtuality.landlord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.realvirtuality.landlord.data.Billing
import app.realvirtuality.landlord.ui.HooderApp
import app.realvirtuality.landlord.ui.theme.HooderTheme
import com.mapbox.common.MapboxOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN
        val billing = Billing(this).also { it.start() }
        enableEdgeToEdge()
        setContent { HooderTheme { HooderApp(billing) } }
    }
}
