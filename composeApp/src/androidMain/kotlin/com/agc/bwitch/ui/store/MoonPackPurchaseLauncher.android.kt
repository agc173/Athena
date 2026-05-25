package com.agc.bwitch.ui.store

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.agc.bwitch.data.moons.billing.googleplay.GooglePlayMoonPackBillingDataSource
import com.agc.bwitch.domain.settings.GooglePlayPurchaseState
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

@Composable
actual fun rememberMoonPackPurchaseLauncher(): MoonPackPurchaseLauncher {
    val dataSource: GooglePlayMoonPackBillingDataSource = koinInject()
    return remember(dataSource) {
        object : MoonPackPurchaseLauncher {
            override suspend fun launch(productId: String): MoonPackPurchaseOutcome {
                return dataSource.launchMoonPackPurchase(productId).fold(
                    onSuccess = {
                        when (it.purchaseState) {
                            GooglePlayPurchaseState.Purchased -> MoonPackPurchaseOutcome.Purchased(it)
                            GooglePlayPurchaseState.Pending -> MoonPackPurchaseOutcome.Pending(it)
                            else -> MoonPackPurchaseOutcome.Failed
                        }
                    },
                    onFailure = {
                        if (it is CancellationException) MoonPackPurchaseOutcome.Cancelled else MoonPackPurchaseOutcome.Failed
                    },
                )
            }

            override suspend fun consume(purchaseToken: String): Boolean = dataSource.consumePurchase(purchaseToken).isSuccess
        }
    }
}
