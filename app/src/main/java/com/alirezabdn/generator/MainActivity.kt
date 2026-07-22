package com.alirezabdn.generator

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alirezabdn.generator.data.DonationServiceDTO
import ir.ayantech.networking.v2.AyanApi
import ir.ayantech.networking.v2.api.onChangeState
import ir.ayantech.networking.v2.api.onFailure
import ir.ayantech.networking.v2.api.onSuccess
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.networking.datasource.DonationRemoteDataSource
import ir.ayantech.networking.datasource.impl.DonationRemoteDataSourceImpl
import ir.ayantech.networking.repository.DonationRepository
import ir.ayantech.networking.repository.impl.DonationRepositoryImpl
import kotlinx.coroutines.launch

const val TAG = "TAG_GENERATOR"

class MainActivity : AppCompatActivity() {

    /**
     *  <p>This is just a sample</p>
     */

    private val ayanAPI: AyanApi by lazy {
        AyanApi.Builder(
            context = this,
            baseUrl = "https://application.billingsystem.ayantech.ir/WebServices/Core.svc/",
        )
            .setInvokeUserToken { "user_token" }
            .build()
    }

    private val sampleRepository: DonationRepository by lazy {
        val remoteSource: DonationRemoteDataSource =
            DonationRemoteDataSourceImpl(
                ayanApi = ayanAPI
            )
        DonationRepositoryImpl(remoteDataSource = remoteSource)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val txt = findViewById<TextView>(R.id.txt_response)
        val btn = findViewById<Button>(R.id.btn_fetch)

        fun sample() {
            txt.text = getString(R.string.loading)
            lifecycleScope.launch {
                val requestBody = DonationServiceDTO.GetDonationRequestBody(id = "abc")
                sampleRepository.getDonationReferrerTypes(requestBody = requestBody).collect { ayanResult ->
                    ayanResult.onSuccess { successResponse ->

                        txt.text = buildString {
                            append("Success Response data count: ")
                            append(
                                successResponse.referrerTypeList?.count() ?: "Something wrong :)"
                            )
                        }
                    }
                    ayanResult.onFailure { exception ->
                        val ayanFailure = exception as? Failure
                        txt.text = ayanFailure?.failureStatus?.description ?: exception.message
                    }

                    ayanResult.onChangeState { stateChange ->
                        Log.d(TAG, "stateChange: ${stateChange.name}")
                    }
                }
            }
        }

        btn.setOnClickListener {
            sample()
        }

    }
}
