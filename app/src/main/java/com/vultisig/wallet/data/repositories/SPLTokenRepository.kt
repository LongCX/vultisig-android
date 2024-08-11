package com.vultisig.wallet.data.repositories

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.vultisig.wallet.data.api.SolanaApi
import com.vultisig.wallet.data.db.dao.TokenValueDao
import com.vultisig.wallet.data.db.models.TokenValueEntity
import com.vultisig.wallet.data.models.SplResponseJson
import com.vultisig.wallet.data.models.SplTokenJson
import com.vultisig.wallet.models.Chain
import com.vultisig.wallet.models.Coin
import com.vultisig.wallet.models.Vault
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

private data class SplTokenResponse(
    val mint: String,
    val amount: BigInteger,
)

internal interface SplTokenRepository {
    suspend fun getTokens(address: String, vault: Vault): List<Coin>
    suspend fun getCachedBalance(coin: Coin): BigInteger
    suspend fun getBalance(coin: Coin): BigInteger?
}

internal class SplTokenRepositoryImpl @Inject constructor(
    private val solanaApi: SolanaApi,
    private val chainAccountAddressRepository: ChainAccountAddressRepository,
    private val tokenValueDao: TokenValueDao,
    private val gson: Gson,
) : SplTokenRepository {

    override suspend fun getTokens(address: String, vault: Vault): List<Coin> {
        val rawSPLTokens = solanaApi.getSPLTokens(address) ?: return emptyList()
        val splTokenResponse = gson
            .fromJson(
                rawSPLTokens, JsonArray::class.java
            ).map { processRawSPLToken(it.toString()) }

        val result = solanaApi.getSPLTokensInfo(splTokenResponse.map { it.mint })
        val splTokens = splTokenResponse.map { key ->
            val coin = createCoin(result.first { key.mint == it.mint }, key.mint, vault)
            tokenValueDao.insertTokenValue(
                TokenValueEntity(
                    Chain.solana.id,
                    coin.address,
                    coin.ticker,
                    key.amount.toString()
                )
            )
            coin
        }
        return splTokens
    }

    override suspend fun getBalance(coin: Coin): BigInteger? {
        return solanaApi.getSPLTokenBalance(
            coin.address,
            coin.contractAddress
        )?.toBigInteger()
    }

    override suspend fun getCachedBalance(coin: Coin): BigInteger {
        return try {
            tokenValueDao.getTokenValue(
                Chain.solana.id,
                coin.address,
                coin.ticker,
            )!!.toBigInteger()
        } catch (e: Exception) {
            Timber.e("get spl balance error", e)
            BigInteger.ZERO
        }
    }

    private suspend fun createCoin(
        tokenResponse: SplTokenJson,
        contractAddress: String,
        vault: Vault
    ): Coin {
        val coin = Coin(
            chain = Chain.solana,
            ticker = tokenResponse.tokenList.ticker,
            logo = tokenResponse.tokenList.logo,
            decimal = tokenResponse.decimals,
            priceProviderID = tokenResponse.tokenList.extensions.coingeckoId,
            contractAddress = contractAddress,
            isNativeToken = false,
            address = "",
            hexPublicKey = ""
        )
        val (derivedAddress, derivedPublicKey) = chainAccountAddressRepository
            .getAddress(coin, vault)
        return coin.copy(address = derivedAddress, hexPublicKey = derivedPublicKey)
    }

    private fun processRawSPLToken(res: String): SplTokenResponse {
        val response = gson.fromJson(res, SplResponseJson::class.java)
        val amount = response.account.data.parsed.info.tokenAmount.amount.toBigInteger()
        val mint = response.account.data.parsed.info.mint
        return SplTokenResponse(mint, amount)
    }
}
