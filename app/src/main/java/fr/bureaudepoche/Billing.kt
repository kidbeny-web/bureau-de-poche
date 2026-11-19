package fr.bureaudepoche

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Achat unique « Pro » via Google Play.
 * Le produit "bureau_pro" (achat intégré, non consommable) doit être créé dans la Play Console.
 * Tant que l'appli n'est pas sur le Play Store, l'achat n'est pas disponible : utiliser le mode test.
 */
class Billing(ctx: Context, private val surPro: () -> Unit) : PurchasesUpdatedListener {
    companion object { const val PRODUIT = "bureau_pro" }

    var prix by mutableStateOf<String?>(null)
        private set
    private var details: ProductDetails? = null

    private val client = BillingClient.newBuilder(ctx)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connecter() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                    chargerProduit()
                    restaurer(null)
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun chargerProduit() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUIT)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            ),
        ).build()
        client.queryProductDetailsAsync(params) { r, resultat ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                details = resultat.productDetailsList.firstOrNull()
                prix = details?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    /** Renvoie false si l'achat n'est pas disponible (appli installée hors Play Store). */
    fun acheter(activity: Activity): Boolean {
        val d = details ?: return false
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()),
        ).build()
        client.launchBillingFlow(activity, flow)
        return true
    }

    fun restaurer(fin: ((Boolean) -> Unit)?) {
        if (!client.isReady) { fin?.invoke(false); return }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { _, achats ->
            var ok = false
            achats.forEach { if (traiter(it)) ok = true }
            fin?.invoke(ok)
        }
    }

    override fun onPurchasesUpdated(r: BillingResult, achats: MutableList<Purchase>?) {
        achats?.forEach { traiter(it) }
    }

    private fun traiter(p: Purchase): Boolean {
        if (PRODUIT !in p.products || p.purchaseState != Purchase.PurchaseState.PURCHASED) return false
        if (!p.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {}
        }
        surPro()
        return true
    }
}
