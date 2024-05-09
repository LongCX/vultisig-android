package com.vultisig.wallet.presenter.keysign

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.vultisig.wallet.presenter.common.KeepScreenOn

@Composable
fun Keysign(navController: NavHostController) {
    KeepScreenOn()
    val context: Context = LocalContext.current.applicationContext
    LaunchedEffect(key1 = Unit) {
        // kick it off to generate key

    }
}