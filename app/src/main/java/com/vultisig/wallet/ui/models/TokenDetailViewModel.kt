package com.vultisig.wallet.ui.models

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vultisig.wallet.data.repositories.AccountsRepository
import com.vultisig.wallet.models.Chain
import com.vultisig.wallet.models.Coins
import com.vultisig.wallet.models.IsSwapSupported
import com.vultisig.wallet.models.isDepositSupported
import com.vultisig.wallet.models.logo
import com.vultisig.wallet.ui.models.mappers.FiatValueToStringMapper
import com.vultisig.wallet.ui.models.mappers.TokenValueToDecimalUiStringMapper
import com.vultisig.wallet.ui.navigation.Destination
import com.vultisig.wallet.ui.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Immutable
internal data class TokenDetailUiModel(
    val token: ChainTokenUiModel = ChainTokenUiModel(),
    val canDeposit: Boolean = false,
    val canSwap: Boolean = false,
)

@HiltViewModel
internal class TokenDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: Navigator<Destination>,
    private val fiatValueToStringMapper: FiatValueToStringMapper,
    private val mapTokenValueToDecimalUiString: TokenValueToDecimalUiStringMapper,
    private val accountsRepository: AccountsRepository,
) : ViewModel() {
    private val chainRaw: String =
        requireNotNull(savedStateHandle.get<String>(Destination.ARG_CHAIN_ID))
    private val vaultId: String =
        requireNotNull(savedStateHandle.get<String>(Destination.ARG_VAULT_ID))
    private val tokenId: String =
        requireNotNull(savedStateHandle.get<String>(Destination.ARG_TOKEN_ID))

    val uiState = MutableStateFlow(TokenDetailUiModel())

    private var loadDataJob: Job? = null

    fun refresh() {
        loadData()
    }

    fun send() {
        viewModelScope.launch {
            navigator.navigate(
                Destination.Send(
                    vaultId = vaultId,
                    chainId = chainRaw,
                )
            )
        }
    }

    fun swap() {
        viewModelScope.launch {
            navigator.navigate(
                Destination.Swap(
                    vaultId = vaultId,
                    chainId = chainRaw,
                )
            )
        }
    }


    fun deposit() {
        viewModelScope.launch {
            navigator.navigate(
                Destination.Deposit(
                    vaultId = vaultId,
                    chainId = chainRaw,
                )
            )
        }
    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            val chain = requireNotNull(Chain.fromRaw(chainRaw))

            accountsRepository.loadAddress(
                vaultId = vaultId,
                chain = chain,
            ).catch {
                // TODO handle error
                Timber.e(it)
            }.collect { address ->
                val token = address.accounts
                    .first { it.token.id == tokenId }
                    .let { account ->
                        val token = account.token
                        ChainTokenUiModel(
                            id = token.id,
                            name = token.ticker,
                            balance = account.tokenValue
                                ?.let(mapTokenValueToDecimalUiString)
                                ?: "",
                            fiatBalance = account.fiatValue
                                ?.let(fiatValueToStringMapper::map),
                            tokenLogo = Coins.getCoinLogo(token.logo),
                            chainLogo = chain.logo,
                        )
                    }

                uiState.update {
                    it.copy(
                        token = token,
                        canDeposit = chain.isDepositSupported,
                        canSwap = chain.IsSwapSupported,
                    )
                }
            }
        }
    }

}