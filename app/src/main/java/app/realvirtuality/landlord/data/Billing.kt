package app.realvirtuality.landlord.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow

// ── Google Play Billing — nakit paketleri (INAPP) + VIP (SUBS) ──────────────────
// Satın alım → sunucuda doğrulanır (onGrant → /wallet/play-grant) → consume/acknowledge.
class Billing(context: Context) {

    companion object {
        val CASH_IDS = listOf(
            "app.realvirtuality.landlord.starter", "app.realvirtuality.landlord.investor",
            "app.realvirtuality.landlord.tycoon", "app.realvirtuality.landlord.mogul",
            "app.realvirtuality.landlord.empire")
        val VIP_IDS = listOf("app.realvirtuality.landlord.vip.monthly", "app.realvirtuality.landlord.vip.yearly")
    }

    data class Item(val id: String, val title: String, val price: String, val sub: Boolean)

    val items = MutableStateFlow<List<Item>>(emptyList())
    var onGrant: ((kind: String, productId: String, token: String) -> Unit)? = null
    private val details = HashMap<String, ProductDetails>()

    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
                purchases.forEach { handle(it) }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {}
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) { query(); restore() }
            }
        })
    }

    private fun query() {
        fun q(ids: List<String>, type: String, sub: Boolean) {
            val params = QueryProductDetailsParams.newBuilder().setProductList(
                ids.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it).setProductType(type).build()
                }).build()
            client.queryProductDetailsAsync(params) { _, list ->
                val add = list.map { pd ->
                    details[pd.productId] = pd
                    val price = if (sub) pd.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
                    else pd.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                    Item(pd.productId, pd.title.substringBefore(" ("), price, sub)
                }
                items.value = (items.value + add).distinctBy { it.id }
            }
        }
        q(CASH_IDS, BillingClient.ProductType.INAPP, false)
        q(VIP_IDS, BillingClient.ProductType.SUBS, true)
    }

    fun buy(activity: Activity, productId: String) {
        val pd = details[productId] ?: return
        val pParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).apply {
            pd.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { setOfferToken(it) }
        }.build()
        client.launchBillingFlow(activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(pParams)).build())
    }

    private fun handle(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val pid = p.products.firstOrNull() ?: return
        val sub = VIP_IDS.contains(pid)
        onGrant?.invoke(if (sub) "sub" else "product", pid, p.purchaseToken)
        if (sub) {
            if (!p.isAcknowledged) client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {}
        } else {
            client.consumeAsync(
                ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { _, _ -> }
        }
    }

    private fun restore() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { _, ps ->
            ps.forEach { handle(it) }
        }
    }
}
