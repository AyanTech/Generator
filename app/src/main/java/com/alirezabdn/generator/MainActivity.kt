package com.alirezabdn.generator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ir.ayantech.ayannetworking.v2.AyanApi
import ir.ayantech.ayannetworking.v2.api.onChangeState
import ir.ayantech.ayannetworking.v2.api.onFailure
import ir.ayantech.ayannetworking.v2.api.onSuccess
import ir.ayantech.ayannetworking.v2.helpers.Failure
import ir.ayantech.networking.callGetEndUserInquiryHistoryDetail
import ir.ayantech.networking.callOnlyInputApi
import ir.ayantech.networking.callOnlyOutputApi
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var ayanAPI: AyanApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ayanAPI = AyanApi.Builder(
            context = this,
            baseUrl = "https://application.billingsystem.ayantech.ir/WebServices/Core.svc/",
        )
            .setInvokeUserToken { "" }
            .build()
        lifecycleScope.launch {
            ayanAPI.callOnlyInputApi(OnlyInputApi.Input("test")).collect { result ->

                result.onSuccess { successResponse ->

                }
                result.onFailure { exception ->
                    val failure = exception as? Failure
                    println(failure?.failureMessage)
                }

                result.onChangeState { state ->

                }
            }
        }

        lifecycleScope.launch {
            ayanAPI.callGetEndUserInquiryHistoryDetail(
                GetEndUserInquiryHistoryDetail.Input("Test")
            ).collect { result ->

                result.onSuccess { successResponse ->
                    println(successResponse)
                }
                result.onFailure { exception ->
                    val failure = exception as? Failure
                    println(failure?.failureMessage)
                }

                result.onChangeState { state ->
                    println(state)
                }
            }
        }

        lifecycleScope.launch {
            ayanAPI.callOnlyOutputApi().collect { result ->

                result.onSuccess { successResponse ->
                    println(successResponse)
                }
                result.onFailure { exception ->
                    val failure = exception as? Failure
                    println(failure?.failureMessage)
                }

                result.onChangeState { state ->
                    println(state)
                }
            }
        }
    }
}