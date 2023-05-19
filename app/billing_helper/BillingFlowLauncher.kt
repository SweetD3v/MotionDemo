package com.interactivewhiteboard.blackboard.billing_helper

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.ProductDetails

class BillingFlowLauncher {
    companion object {
        fun launchBillingFlow(
            activity: Activity,
            list: List<ProductDetails>,
            billingClient: BillingClient,
            billingStateListener: BillingHelper.BillingStateListener?
        ) {
            val productDetailsParams = ProductDetailsParams.newBuilder()
                .setProductDetails(list.first()).build()

            val paramsList: MutableList<ProductDetailsParams> = ArrayList()
            paramsList.add(productDetailsParams)

            val billingFlowParams = BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(paramsList)
                .build()
            val response = billingClient.launchBillingFlow(activity, billingFlowParams)
                .responseCode
            when (response) {
                BillingClient.BillingResponseCode.USER_CANCELED, BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, BillingClient.BillingResponseCode.DEVELOPER_ERROR, BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED, BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED, BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                    billingStateListener?.onPurchaseFailed()

                else -> {}
            }
        }
    }
}