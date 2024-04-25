package com.voltix.wallet.presenter.device_list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.voltix.wallet.R
import com.voltix.wallet.app.ui.theme.appColor
import com.voltix.wallet.app.ui.theme.dimens
import com.voltix.wallet.app.ui.theme.menloFamily
import com.voltix.wallet.app.ui.theme.montserratFamily
import com.voltix.wallet.presenter.common.TopBar
import com.voltix.wallet.presenter.device_list.components.DeviceInfoItem
import com.voltix.wallet.presenter.navigation.Screen

@Composable
fun DeviceList(navController: NavHostController, itemCount: Int = 4) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val items = listOf(
        "1- iPad Pro 6th generation (This Device)",
        "2- iPad Pro (Pair Device)",
        "3- iPad Pro (Pair Device)",
        "4- iPad Pro (Backup Device)",
    ).subList(0, itemCount)
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.appColor.oxfordBlue800)
            .padding(MaterialTheme.dimens.medium1)
    ) {
        TopBar(centerText = "Keygen", startIcon = R.drawable.caret_left)

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.medium2))

        Text(
            text = "2 of 2 Vault",
            color = textColor,
            style = MaterialTheme.montserratFamily.bodyLarge
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.small2))

        Text(
            text = "With these devices",
            color = textColor,
            style = MaterialTheme.montserratFamily.bodyMedium
        )

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.medium1))

        LazyColumn {
            items.forEach {
                item {
                    DeviceInfoItem(it)
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.medium1))

        Text(
            style = MaterialTheme.menloFamily.bodyMedium,
            text = "You can only send transactions with these two devices present.",
            color = textColor
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.small3))
        Text(
            style = MaterialTheme.menloFamily.bodyMedium,
            text = "You do not have a 3rd backup device - so you should backup one vault share securely later.",
            color = textColor
        )


        Spacer(modifier = Modifier.weight(1.0f))


        Button(onClick = {
            if (itemCount + 1 <= 4) {
                navController.navigate(
                    Screen.DeviceList.route.replace(
                        oldValue = "{count}", newValue = "${itemCount + 1}"
                    )
                )
            } else {
                navController.navigate(
                    Screen.GeneratingKeyGen.route
                )
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Continue")
        }
    }
}