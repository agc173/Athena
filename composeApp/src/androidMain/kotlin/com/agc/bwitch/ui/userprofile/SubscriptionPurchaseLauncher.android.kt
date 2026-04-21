package com.agc.bwitch.ui.userprofile

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.agc.bwitch.data.settings.billing.googleplay.GooglePlayBillingSubscriptionProducts
import com.agc.bwitch.data.settings.billing.googleplay.GooglePlaySubscriptionBillingDataSource
import com.agc.bwitch.presentation.userprofile.SubscriptionManagementOutcome
import com.agc.bwitch.presentation.userprofile.SubscriptionPlanSelection
import com.agc.bwitch.presentation.userprofile.SubscriptionPurchaseOutcome
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

@Composable
actual fun rememberSubscriptionPurchaseLauncher(): SubscriptionPurchaseLauncher {
    val context = LocalContext.current
    val billingDataSource: GooglePlaySubscriptionBillingDataSource = koinInject()

    return remember(context, billingDataSource) {
        object : SubscriptionPurchaseLauncher {
            override suspend fun launch(plan: SubscriptionPlanSelection): SubscriptionPurchaseOutcome {
                val activity = context as? Activity ?: return SubscriptionPurchaseOutcome.Unsupported
                val productId = when (plan) {
                    SubscriptionPlanSelection.Monthly -> GooglePlayBillingSubscriptionProducts.MONTHLY
                    SubscriptionPlanSelection.Annual -> GooglePlayBillingSubscriptionProducts.ANNUAL
                }

                return launch(productId)
            }

            override suspend fun launch(productId: String): SubscriptionPurchaseOutcome {
                val activity = context as? Activity ?: return SubscriptionPurchaseOutcome.Unsupported

                return runCatching {
                    billingDataSource.launchPurchaseFlow(activity = activity, productId = productId)
                }.getOrElse { error ->
                    Result.failure(error)
                }.fold(
                    onSuccess = { SubscriptionPurchaseOutcome.Success },
                    onFailure = { error ->
                        if (error is CancellationException) {
                            SubscriptionPurchaseOutcome.Cancelled
                        } else {
                            SubscriptionPurchaseOutcome.Failed
                        }
                    },
                )
            }
        }
    }
}

@Composable
actual fun rememberSubscriptionManagementLauncher(): SubscriptionManagementLauncher {
    val context = LocalContext.current
    val billingDataSource: GooglePlaySubscriptionBillingDataSource = koinInject()

    return remember(context, billingDataSource) {
        object : SubscriptionManagementLauncher {
            override suspend fun launch(productId: String?): SubscriptionManagementOutcome {
                val activity = context as? Activity ?: return SubscriptionManagementOutcome.Unsupported
                return billingDataSource.launchManageSubscriptions(activity, productId)
                    .fold(
                        onSuccess = { SubscriptionManagementOutcome.Opened },
                        onFailure = { SubscriptionManagementOutcome.Failed },
                    )
            }
        }
    }
}
