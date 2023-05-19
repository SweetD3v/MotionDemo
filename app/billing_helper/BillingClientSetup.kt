package com.interactivewhiteboard.blackboard.billing_helper

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

class BillingClientSetup {
    companion object {
        fun getInstance(
            context: Context,
            listener: PurchasesUpdatedListener
        ): BillingClient {
            return setupBillingClient(context, listener)
        }

        private fun setupBillingClient(
            context: Context,
            listener: PurchasesUpdatedListener
        ): BillingClient {
            return BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(listener)
                .build()
        }
    }
}