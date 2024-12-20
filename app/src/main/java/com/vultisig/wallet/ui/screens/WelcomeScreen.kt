package com.vultisig.wallet.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vultisig.wallet.R
import com.vultisig.wallet.data.models.OnBoardPage
import com.vultisig.wallet.ui.components.MultiColorButton
import com.vultisig.wallet.ui.components.PagerCircleIndicator
import com.vultisig.wallet.ui.components.UiSpacer
import com.vultisig.wallet.ui.models.UiEvent
import com.vultisig.wallet.ui.models.WelcomeViewModel
import com.vultisig.wallet.ui.theme.Theme


@ExperimentalAnimationApi
@Composable
internal fun WelcomeScreen(
    navController: NavHostController,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val pages = viewModel.state.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(key1 = Unit) {
        viewModel.channel.collect { uiEvent ->
            when (uiEvent) {
                is UiEvent.ScrollToNextPage -> {
                    if (pagerState.currentPage < pagerState.pageCount - 1)
                        pagerState.scrollToPage(pagerState.currentPage + 1)
                    else {
                        navController.popBackStack()
                        navController.navigate(uiEvent.screen.route)
                    }

                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.oxfordBlue800),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UiSpacer(size = 30.dp)

        Image(
            painter = painterResource(R.drawable.vultisig_icon_text),
            contentDescription = "Pager Image"
        )
        HorizontalPager(
            modifier = Modifier.weight(9f),
            state = pagerState,
            verticalAlignment = Alignment.Top
        ) { position ->
            PagerScreen(onBoardingPage = pages[position])
        }
        PagerCircleIndicator(
            currentIndex = pagerState.currentPage,
            size = pagerState.pageCount,
        )
        Spacer(
            modifier = Modifier
                .weight(0.3f)
        )
        MultiColorButton(
            text = stringResource(R.string.welcome_screen_next),
            textColor = Theme.colors.oxfordBlue800,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 16.dp,
                    horizontal = 16.dp,
                ),
            onClick = { viewModel.scrollToNextPage() }
        )
        Spacer(
            modifier = Modifier
                .weight(0.3f)
        )

        val isSkipVisible = pagerState.currentPage < pages.size - 1

        MultiColorButton(
            text = stringResource(R.string.welcome_screen_skip),
            backgroundColor = Theme.colors.oxfordBlue800,
            textColor = Theme.colors.turquoise800,
            iconColor = Theme.colors.oxfordBlue800,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isSkipVisible) 1f else 0f),
            onClick = if (isSkipVisible) {
                { viewModel.skip() }
            } else {
                { /* do nothing if invisible */ }
            },
        )

        UiSpacer(size = 24.dp)
    }
}

@Composable
private fun PagerScreen(onBoardingPage: OnBoardPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.weight(1.0f))
        Image(
            painter = painterResource(id = onBoardingPage.image),
            contentDescription = "Pager Image"
        )
        UiSpacer(size = 48.dp)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .padding(top = 20.dp),
            text = stringResource(id = onBoardingPage.description),
            style = Theme.montserrat.body1,
            color = Theme.colors.neutral0,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(1.0f))
    }
}
