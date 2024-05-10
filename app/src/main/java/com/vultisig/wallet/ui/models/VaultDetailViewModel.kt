package com.vultisig.wallet.ui.models

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vultisig.wallet.data.on_board.db.VaultDB
import com.vultisig.wallet.data.repositories.ChainAccountsRepository
import com.vultisig.wallet.models.Coin
import com.vultisig.wallet.models.Vault
import com.vultisig.wallet.ui.models.mappers.ChainAccountToChainAccountUiModelMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
internal data class VaultDetailUiModel(
    val vaultName: String = "",
    val accounts: List<ChainAccountUiModel> = emptyList(),
)

data class ChainAccountUiModel(
    val chainName: String,
    @DrawableRes val logo: Int,
    val address: String,
    val nativeTokenAmount: String?,
    val fiatAmount: String?,
    val coins: List<Coin> = emptyList(),
)

@HiltViewModel
internal class VaultDetailViewModel @Inject constructor(
    private val vaultDb: VaultDB,
    private val chainAccountsRepository: ChainAccountsRepository,
    private val chainAccountToUiModelMapper: ChainAccountToChainAccountUiModelMapper,
) : ViewModel() {
    var vault: Vault? by mutableStateOf(null)
    var isRefreshing: Boolean by mutableStateOf(false)

    val uiState = MutableStateFlow(VaultDetailUiModel())
    var currentVault: MutableState<Vault> = mutableStateOf(Vault("temp"))
    fun loadData(vaultId: String) {
        loadVaultName(vaultId)
        loadAccounts(vaultId)
    }

    fun refreshData() {
        viewModelScope.launch {
            isRefreshing = true
            // TODO: add refresh logic here
            delay(3000)
            isRefreshing = false
        }
    }

    private fun loadVaultName(vaultId: String) {
        viewModelScope.launch {
            val vault = requireNotNull(vaultDb.select(vaultId))
            currentVault.value = vault
            this@VaultDetailViewModel.vault = vault
            uiState.update { it.copy(vaultName = vault.name) }
        }
    }

    private fun loadAccounts(vaultId: String) {
        viewModelScope.launch {
            chainAccountsRepository.loadChainAccounts(vaultId)
                .map { it.map(chainAccountToUiModelMapper::map) }
                .collect { accounts ->
                    uiState.update { it.copy(accounts = accounts) }
                }
        }
    }

}