package com.alirezabdn.generator.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alirezabdn.generator.domain.usecase.GetDonationReferrerTypesUseCaseImpl
import ir.ayantech.networking.v2.helpers.Failure
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Coordinates donation loading and exposes presentation-only state to the activity. */
class DonationViewModel(
    private val getDonationReferrerTypes: GetDonationReferrerTypesUseCaseImpl,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<DonationUiState>(DonationUiState.Idle)
    val uiState: StateFlow<DonationUiState> = mutableUiState.asStateFlow()

    private var loadJob: Job? = null

    /** Loads referrer types, replacing any request already in progress. */
    fun loadReferrerTypes(donationId: String = DEFAULT_DONATION_ID) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.value = DonationUiState.Loading
            val result = getDonationReferrerTypes(donationId)
            mutableUiState.value = result.fold(
                onSuccess = { DonationUiState.Content(it) },
                onFailure = { failure ->
                    val message = (failure as? Failure)?.failureMessage ?: failure.message.orEmpty()
                    DonationUiState.Error(message)
                },
            )
        }
    }

    private companion object {
        const val DEFAULT_DONATION_ID = "abc"
    }
}
