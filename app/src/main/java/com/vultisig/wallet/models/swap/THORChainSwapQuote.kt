package com.vultisig.wallet.models.swap

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

internal data class THORChainSwapQuote(
    @SerializedName("dust_threshold")
    val dustThreshold: String?,
    @SerializedName("expected_amount_out")
    val expectedAmountOut: String,
    @SerializedName("expiry")
    val expiry: BigInteger,
    @SerializedName("fee")
    val fee: Fees,
    @SerializedName("inbound_addresses") val inboundAddress: String?,
    @SerializedName("inbound_confirmation_blocks") val inboundConfirmationBlocks: BigInteger?,
    @SerializedName("inbound_confirmation_seconds") val inboundConfirmationSeconds: BigInteger?,
    @SerializedName("max_streaming_quantity") val maxStreamingQuantity: Int,
    val memo: String,
    @SerializedName("notes")
    val notes: String,
    @SerializedName("outbound_delay_blocks") val outboundDelayBlocks: BigInteger,
    @SerializedName("outbound_delay_seconds") val outboundDelaySeconds: BigInteger,
    @SerializedName("recommended_min_amount_in") val recommendedMinAmountIn: BigInteger,
    @SerializedName("slippage_bps") val slippageBps: BigInteger,
    @SerializedName("streaming_swap_blocks") val streamingSwapBlocks: BigInteger,
    @SerializedName("total_swap_seconds") val totalSwapSeconds: BigInteger?,
    val warning: String,
    @SerializedName("router")
    val router: String?,
)

internal data class Fees(
    @SerializedName("affiliate")
    val affiliate: String,
    @SerializedName("asset")
    val asset: String,
    @SerializedName("outbound")
    val outbound: String,
    @SerializedName("total")
    val total: String,
)