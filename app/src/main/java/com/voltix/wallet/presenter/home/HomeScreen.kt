package com.voltix.wallet.presenter.home

import MultiColorButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.voltix.wallet.app.ui.theme.appColor
import com.voltix.wallet.app.ui.theme.dimens
import com.voltix.wallet.app.ui.theme.menloFamily
import com.voltix.wallet.app.ui.theme.montserratFamily
import com.voltix.wallet.presenter.navigation.Screen

@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: HomeViewModel = viewModel()
    val vaults = viewModel.vaults.asFlow().collectAsState(initial = emptyList()).value
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onBackground
    LaunchedEffect(key1 = viewModel) {
        viewModel.setData(context)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.appColor.oxfordBlue800)
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            LazyColumn() {
                items(vaults) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                    ) {
                        Text(
                            text = it.name,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            style = MaterialTheme.menloFamily.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(16.dp)
                        )
                    }
                }
            }
            MultiColorButton(
                text = "+ Add New Vault",
                minHeight = MaterialTheme.dimens.minHeightButton,
                backgroundColor = MaterialTheme.appColor.turquoise800,
                textColor = MaterialTheme.appColor.oxfordBlue800,
                iconColor = MaterialTheme.appColor.turquoise800,
                textStyle = MaterialTheme.montserratFamily.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.dimens.marginMedium,
                        end = MaterialTheme.dimens.marginMedium,
                        bottom = MaterialTheme.dimens.marginMedium,
                    )
            ) {
                navController.navigate(route = Screen.CreateNewVault.route)
            }

        }
    }
}

@Preview(showBackground = true, name = "HomeScreen")
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    HomeScreen(navController = navController)
}