package com.vultisig.wallet.presenter.keysign

import android.net.nsd.NsdManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.vultisig.wallet.common.DeepLinkHelper
import com.vultisig.wallet.common.Endpoints
import com.vultisig.wallet.common.unzip
import com.vultisig.wallet.data.api.BlockChairApi
import com.vultisig.wallet.data.api.CosmosApiFactory
import com.vultisig.wallet.data.api.EvmApiFactory
import com.vultisig.wallet.data.api.MayaChainApi
import com.vultisig.wallet.data.api.SolanaApi
import com.vultisig.wallet.data.api.ThorChainApi
import com.vultisig.wallet.data.on_board.db.VaultDB
import com.vultisig.wallet.models.TssKeysignType
import com.vultisig.wallet.models.Vault
import com.vultisig.wallet.presenter.keygen.MediatorServiceDiscoveryListener
import com.vultisig.wallet.tss.TssKeyType
import com.vultisig.wallet.ui.navigation.Destination
import com.vultisig.wallet.ui.navigation.Navigator
import com.vultisig.wallet.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

enum class JoinKeysignState {
    DiscoveryingSessionID, DiscoverService, JoinKeysign, WaitingForKeysignStart, Keysign, FailedToStart, Error
}


@HiltViewModel
internal class JoinKeysignViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: Navigator<Destination>,

    private val vaultDB: VaultDB,
    private val gson: Gson,
    private val thorChainApi: ThorChainApi,
    private val blockChairApi: BlockChairApi,
    private val evmApiFactory: EvmApiFactory,
    private val mayaChainApi: MayaChainApi,
    private val cosmosApiFactory: CosmosApiFactory,
    private val solanaApi: SolanaApi,
) : ViewModel() {
    private val vaultId: String = requireNotNull(savedStateHandle[Screen.JoinKeysign.ARG_VAULT_ID])
    private var _currentVault: Vault = Vault("temp vault")
    var currentState: MutableState<JoinKeysignState> =
        mutableStateOf(JoinKeysignState.DiscoveryingSessionID)
    var errorMessage: MutableState<String> = mutableStateOf("")
    private var _localPartyID: String = ""
    private var _sessionID: String = ""
    private var _serviceName: String = ""
    private var _useVultisigRelay: Boolean = false
    private var _encryptionKeyHex: String = ""
    private var _serverAddress: String = ""
    private var _keysignCommittee: List<String> = emptyList()
    private var _discoveryListener: MediatorServiceDiscoveryListener? = null
    private var _nsdManager: NsdManager? = null
    private var _keysignPayload: KeysignPayload? = null
    private var _jobWaitingForKeysignStart: Job? = null
    private var isScanStarted = false

    val keysignPayload: KeysignPayload?
        get() = _keysignPayload
    val keysignViewModel: KeysignViewModel
        get() = KeysignViewModel(
            vault = _currentVault,
            keysignCommittee = _keysignCommittee,
            serverAddress = _serverAddress,
            sessionId = _sessionID,
            encryptionKeyHex = _encryptionKeyHex,
            messagesToSign = _keysignPayload!!.getKeysignMessages(_currentVault),
            keyType = _keysignPayload?.coin?.chain?.TssKeysignType ?: TssKeyType.ECDSA,
            keysignPayload = _keysignPayload!!,
            gson = gson,
            thorChainApi = thorChainApi,
            blockChairApi = blockChairApi,
            evmApiFactory = evmApiFactory,
            mayaChainApi = mayaChainApi,
            cosmosApiFactory = cosmosApiFactory,
            solanaApi = solanaApi,
        )

    fun setData() {
        vaultDB.select(vaultId)?.let {
            _currentVault = it
            _localPartyID = it.localPartyID
        }
    }

    fun startScan() {
        if (isScanStarted) return
        isScanStarted = true

        viewModelScope.launch {
            navigator.navigate(Destination.ScanQr)
        }
    }

    fun setScanResult(content: String) {
        try {
            val qrCodeContent = DeepLinkHelper(content).getJsonData()
            qrCodeContent ?: run {
                throw Exception("Invalid QR code content")
            }
            val rawJson = qrCodeContent.decodeBase64Bytes().unzip().toString()
            Timber.d(
                "QR code content: %s", rawJson
            )
            val payload = gson.fromJson(
                rawJson,
                KeysignMesssage::class.java
            )
            if (_currentVault.pubKeyECDSA != payload.payload.vaultPublicKeyECDSA) {
                errorMessage.value = "Wrong vault"
                currentState.value = JoinKeysignState.Error
                return
            }
            this._keysignPayload = payload.payload
            this._sessionID = payload.sessionID
            this._serviceName = payload.serviceName
            this._useVultisigRelay = payload.usevultisigRelay
            this._encryptionKeyHex = payload.encryptionKeyHex
            if (_useVultisigRelay) {
                this._serverAddress = Endpoints.VULTISIG_RELAY
                currentState.value = JoinKeysignState.JoinKeysign
            } else {
                currentState.value = JoinKeysignState.DiscoverService
            }
        } catch (e: Exception) {
            errorMessage.value = "Invalid QR code content"
            currentState.value = JoinKeysignState.Error
        }
    }

    private fun onServerAddressDiscovered(addr: String) {
        _serverAddress = addr
        currentState.value = JoinKeysignState.JoinKeysign
        // discovery finished
        _discoveryListener?.let { _nsdManager?.stopServiceDiscovery(it) }
    }

    fun discoveryMediator(nsdManager: NsdManager) {
        _discoveryListener =
            MediatorServiceDiscoveryListener(nsdManager, _serviceName, ::onServerAddressDiscovered)
        _nsdManager = nsdManager
        nsdManager.discoverServices(
            "_http._tcp.", NsdManager.PROTOCOL_DNS_SD, _discoveryListener
        )
    }

    suspend fun joinKeysign() {
        withContext(Dispatchers.IO) {
            try {
                val serverUrl = URL("${_serverAddress}/$_sessionID")
                Timber.tag("JoinKeysignViewModel").d("Joining keysign at $serverUrl")
                val payload = listOf(_localPartyID)

                val client = OkHttpClient().newBuilder().retryOnConnectionFailure(true).build()
                val request = okhttp3.Request.Builder().method(
                    "POST", gson.toJson(payload).toRequestBody("application/json".toMediaType())
                ).url(serverUrl).build()
                client.newCall(request).execute().use {
                    Timber.tag("JoinKeysignViewModel").d("Join keysign: Response code: %s", it.code)
                }
                currentState.value = JoinKeysignState.WaitingForKeysignStart
            } catch (e: Exception) {
                Timber.tag("JoinKeysignViewModel")
                    .e("Failed to join keysign: %s", e.stackTraceToString())
                errorMessage.value = "Failed to join keysign"
                currentState.value = JoinKeysignState.FailedToStart
            }
        }
    }

    fun cleanUp() {
        _jobWaitingForKeysignStart?.cancel()
    }

    suspend fun waitForKeysignToStart() {
        _jobWaitingForKeysignStart = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    if (checkKeygenStarted()) {
                        currentState.value = JoinKeysignState.Keysign
                        return@withContext
                    }
                    // backoff 1s
                    Thread.sleep(1000)
                }
            }
        }
    }

    private fun checkKeygenStarted(): Boolean {
        try {
            val serverURL = "$_serverAddress/start/$_sessionID"
            Timber.tag("JoinKeysignViewModel").d("Checking keysign start at %s", serverURL)
            val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()
            val request = okhttp3.Request.Builder().url(serverURL).get().build()
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    HttpURLConnection.HTTP_OK -> {
                        Timber.d("Keysign started")
                        response.body?.let {
                            val result = it.string()
                            val tokenType = object : TypeToken<List<String>>() {}.type
                            this._keysignCommittee = gson.fromJson(result, tokenType)
                            Timber.d("Keysign committee: $_keysignCommittee")
                            Timber.d("local party: $_localPartyID")
                            if (this._keysignCommittee.contains(_localPartyID)) {
                                return true
                            }
                        }
                    }

                    else -> {
                        Timber.d("Failed to check start keysign: Response code: ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(
                "Failed to check keysign start: ${e.stackTraceToString()}"
            )
        }
        return false
    }
}