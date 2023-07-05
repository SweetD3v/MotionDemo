package com.interactivewhiteboard.blackboard.billing_helper

import android.app.Activity
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.QueryPurchasesParams
import com.interactivewhiteboard.blackboard.R
import com.interactivewhiteboard.blackboard.utils.isOnline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BillingHelper(
    private var activity: Activity,
    var billingStateListener: BillingStateListener?
) {
    private var billingClient: BillingClient? = null
    private var successDialog: Boolean? = false
    private var productDetailsList: MutableList<ProductDetails> = mutableListOf()

    private val jobPurchase = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + jobPurchase)

    fun queryProduct(productId: String, successDialog: Boolean? = false) {
        this.successDialog = successDialog

        if (!activity.isOnline()) {
            billingStateListener?.onConnectionFailed()
            return
        }

        ioScope.launch {
            val purchaseListener = PurchasesUpdatedListener { billingResult, purchaseList ->
                if (billingResult.responseCode == BillingResponseCode.OK
                    && !purchaseList.isNullOrEmpty()
                ) {
                    handleItemAlreadyPurchase(purchaseList.first())
                    ensureMainThread {
                        if (successDialog == true)
                            showPurchaseSuccessDialog()
                        else {
                            billingStateListener?.onProductPurchased()
                        }
                    }
                } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
                    ensureMainThread {
                        billingStateListener?.onPurchaseFailed()
                    }
                }
            }

            billingClient = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(purchaseListener)
                .build()

            billingClient?.let { bc ->
                bc.startConnection(object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                        billingStateListener?.onConnectionFailed()
                    }

                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            queryPurchaseAsync(bc)
                            queryDetailsAsync(bc, productId)
                        }
                    }
                })
            }
        }
    }

    private fun queryDetailsAsync(
        bc: BillingClient,
        productId: String
    ) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                mutableListOf(
                    Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(
                            BillingClient.ProductType.INAPP
                        )
                        .build()
                )
            ).build()

        bc.queryProductDetailsAsync(queryProductDetailsParams) { billingRes, productDetailsList ->
            val responseCode1 = billingRes.responseCode
            if (responseCode1 == BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList.first()
                    this@BillingHelper.productDetailsList =
                        productDetailsList
                    val productInfo = ProductInfo(
                        productId = productDetails.productId,
                        productTitle = productDetails.title,
                        price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                            ?: ""
                    )

                    ensureMainThread {
                        billingStateListener?.onProductAvailable(productInfo)
                    }
                } else {
                    ensureMainThread {
                        billingStateListener?.onPurchaseFailed()
                    }
                }
            } else {
                ensureMainThread {
                    billingStateListener?.onQueryProductFailed()
                }
            }
        }
    }

    private fun queryPurchaseAsync(
        bc: BillingClient
    ) {
        val purchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()

        bc.queryPurchasesAsync(
            purchasesParams
        ) { billingResult12: BillingResult, purchasesList: List<Purchase?> ->
            val responseCode = billingResult12.responseCode
            if (bc.isReady) {
                if (responseCode == BillingResponseCode.OK) {
                    val skuList = ArrayList<String>()
                    val productList: MutableList<Product> = ArrayList()

                    if (purchasesList.isNotEmpty()) {
                        for (i in purchasesList.indices) {
                            val purchaseItem = purchasesList[i]
                            purchaseItem?.let { purchase ->
                                skuList.add(purchase.products.first())
                                handleItemAlreadyPurchase(purchase)
                                billingStateListener?.onProductsAlreadyPurchased(purchase)
                            }
                        }
                        for (i in skuList.indices) {
                            val product = Product.newBuilder()
                                .setProductId(skuList[i])
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                            productList.add(product)
                        }
                    }
                }
            } else {
                billingStateListener?.onConnectionFailed()
            }
        }
    }

    fun launchBilling() {
        if (activity.isOnline()) {
            billingClient?.let {
                if (it.isReady) {
                    launchBillingFlow(
                        activity,
                        productDetailsList,
                        it,
                        billingStateListener
                    )
                } else billingStateListener?.onPurchaseFailed()
            }
        } else {
            billingStateListener?.onConnectionFailed()
        }
    }

    private fun launchBillingFlow(
        activity: Activity,
        list: List<ProductDetails>,
        billingClient: BillingClient,
        billingStateListener: BillingStateListener?
    ) {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(list.first()).build()

        val paramsList: MutableList<BillingFlowParams.ProductDetailsParams> = ArrayList()
        paramsList.add(productDetailsParams)

        val billingFlowParams = BillingFlowParams
            .newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()
        when (billingClient.launchBillingFlow(activity, billingFlowParams).responseCode) {
            BillingResponseCode.USER_CANCELED, BillingResponseCode.BILLING_UNAVAILABLE, BillingResponseCode.DEVELOPER_ERROR, BillingResponseCode.FEATURE_NOT_SUPPORTED, BillingResponseCode.ITEM_ALREADY_OWNED, BillingResponseCode.SERVICE_DISCONNECTED, BillingResponseCode.SERVICE_TIMEOUT,
            BillingResponseCode.ITEM_UNAVAILABLE ->
                billingStateListener?.onPurchaseFailed()

            else -> {}
        }
    }

    private fun handleItemAlreadyPurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(
                    acknowledgePurchaseParams
                ) { billingResult: BillingResult? ->
                    billingResult?.let { result ->
                        if (result.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
                            ensureMainThread {
                                billingStateListener?.onPurchaseAcknowledged()
                            }
                        }
                    }
                }
            } else {
                ensureMainThread {
                    billingStateListener?.onPurchaseAcknowledged()
                }
            }
        }
    }

    private fun showPurchaseSuccessDialog() {
        val builder1 = AlertDialog.Builder(activity, R.style.RoundedCornersDialog)
            .setTitle(activity.getString(R.string.congratulations))
            .setMessage(activity.getString(R.string.purchase_successful))
            .setCancelable(false)
            .setPositiveButton(
                activity.getString(R.string.ok)
            ) { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
                ensureMainThread {
                    billingStateListener?.onPurchaseDialogClicked()
                }
            }
        val alertDialog1 = builder1.create()
        alertDialog1.show()
        val buttonbackground = alertDialog1.getButton(DialogInterface.BUTTON_POSITIVE)
        buttonbackground.setTextColor(ContextCompat.getColor(activity, R.color.color_primary))
    }

    fun cancelPurchaseJob() {
        jobPurchase.cancel()
    }

    fun isChecking() = jobPurchase.isActive

    interface BillingStateListener {
        fun onProductsAlreadyPurchased(purchase: Purchase?)
        fun onProductAvailable(productInfo: ProductInfo)
        fun onProductPurchased()
        fun onPurchaseDialogClicked()
        fun onQueryProductFailed()
        fun onPurchaseAcknowledged()
        fun onPurchaseFailed()
        fun onConnectionFailed()
    }

    @Keep
    data class ProductInfo(var productTitle: String, var productId: String, var price: String)
}

fun ensureMainThread(callback: () -> Unit) {
    if (isOnMainThread()) {
        callback()
    } else {
        Handler(Looper.getMainLooper()).post {
            callback()
        }
    }
}

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()









lm.spanSizeLookup = SpannedGridLayoutManager.SpanSizeLookup { position ->
            when (position % 80) {
                5, 22 ->
                    SpanSize(2, 2)

                35, 45 ->
                    SpanSize(3, 3)

                52, 70 ->
                    SpanSize(2, 2)

                else ->
                    SpanSize(1, 1)
            }
        }
