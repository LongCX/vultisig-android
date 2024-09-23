package com.vultisig.wallet.data.api.models.cosmos

import com.vultisig.wallet.data.utils.BigIntegerSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class PolkadotGetNonceJson(
    @SerialName("result")
    @Serializable(with = BigIntegerSerializer::class)
    val result: BigInteger,
)

@Serializable
data class PolkadotGetBlockHashJson(
    @SerialName("result")
    val result: String,
)

@Serializable
data class PolkadotGetRunTimeVersionJson(
    @SerialName("result")
    val result: PolkadotGetRunTimeVersionResultJson,
)

@Serializable
data class PolkadotGetRunTimeVersionResultJson(
    @SerialName("transactionVersion")
    @Serializable(with = BigIntegerSerializer::class)
    val transactionVersion: BigInteger,
    @SerialName("specVersion")
    @Serializable(with = BigIntegerSerializer::class)
    val specVersion: BigInteger,
)

@Serializable
data class PolkadotGetBlockHeaderJson(
    @SerialName("result")
    val result: PolkadotGetBlockHeaderNumberJson,
)

@Serializable
data class PolkadotGetBlockHeaderNumberJson(
    @SerialName("number")
    val number: String,
)


@Serializable
data class PolkadotBroadcastTransactionJson(
    @SerialName("result")
    val result: String?,
    @SerialName("error")
    val error: PolkadotBroadcastTransactionErrorJson?,
)

@Serializable
data class PolkadotBroadcastTransactionErrorJson(
    @SerialName("code")
    val code: Int,
)