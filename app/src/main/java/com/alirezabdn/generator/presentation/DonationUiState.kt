package com.alirezabdn.generator.presentation

import com.alirezabdn.generator.domain.model.ReferrerType

/** Immutable screen states rendered by [MainActivity]. */
sealed interface DonationUiState {
    data object Idle : DonationUiState

    data object Loading : DonationUiState

    data class Content(
        val referrerTypes: List<ReferrerType>,
    ) : DonationUiState

    data class Error(
        val message: String,
    ) : DonationUiState
}
