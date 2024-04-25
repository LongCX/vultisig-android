package com.voltix.wallet.presenter.welcome

import com.voltix.wallet.domain.on_board.models.OnBoardPage

data class WelcomeState(
    val pages: List<OnBoardPage> = emptyList()
)