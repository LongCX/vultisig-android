package com.vultisig.wallet.data.models

import wallet.core.jni.CoinType

typealias TokenId = String

data class CoinTradeAsset(
    val chain: Chain,
    val ticker: String,
    val logo: String,
    val address: String,
    val decimal: Int,
    val hexPublicKey: String,
    val priceProviderID: String,
    val contractAddress: String,
    val isNativeToken: Boolean,
) {
    val id: TokenId
        get() = "${ticker}~${contractAddress}"

    val coinType: CoinType
        get() = chain.coinType

}

fun Coin.allowZeroGas(): Boolean {
    return this.chain == Chain.Polkadot
}
