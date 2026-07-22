package com.alirezabdn.generator.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alirezabdn.generator.R
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Renders [DonationUiState] and delegates user actions to a Koin-injected ViewModel. */
class MainActivity : AppCompatActivity() {
    private val viewModel: DonationViewModel by viewModel()

    private lateinit var responseText: TextView
    private lateinit var fetchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        responseText = findViewById(R.id.txt_response)
        fetchButton = findViewById(R.id.btn_fetch)

        fetchButton.setOnClickListener {
            viewModel.loadReferrerTypes()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: DonationUiState) {
        fetchButton.isEnabled = state !is DonationUiState.Loading
        responseText.text = when (state) {
            DonationUiState.Loading -> getString(R.string.loading)
            is DonationUiState.Content -> state.contentText()
            is DonationUiState.Error -> getString(R.string.donation_error, state.message)
            else -> {
                responseText.text
            }
        }
    }

    private fun DonationUiState.Content.contentText(): String {
        if (referrerTypes.isEmpty()) return getString(R.string.donation_empty)

        val names = referrerTypes.joinToString(separator = "\n") { referrerType ->
            "• ${referrerType.displayName}"
        }
        return getString(R.string.donation_loaded, referrerTypes.size, names)
    }
}
