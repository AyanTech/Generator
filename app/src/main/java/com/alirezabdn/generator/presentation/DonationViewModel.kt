package com.alirezabdn.generator.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alirezabdn.generator.domain.model.ReferrerType
import com.alirezabdn.generator.domain.usecase.GetDonationReferrerTypesUseCaseImpl
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import kotlinx.coroutines.Job
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
            getDonationReferrerTypes(donationId).collect { result ->
                mutableUiState.value = result.toUiState()
            }
        }
    }

    private fun AyanAPIResult<List<ReferrerType>, ApiCallStatus, Exception>.toUiState(): DonationUiState =
        when (this) {
            is AyanAPIResult.Success -> DonationUiState.Content(this.value)
            is AyanAPIResult.Error -> {
                DonationUiState.Error(ayanFailure.message ?: "")
            }

            is AyanAPIResult.ChangeState -> {
                if (this.state == ApiCallStatus.LOADING) {
                    DonationUiState.Loading
                } else {
                    DonationUiState.Idle
                }
            }
        }

    private companion object {
        const val DEFAULT_DONATION_ID = "abc"
    }
}
