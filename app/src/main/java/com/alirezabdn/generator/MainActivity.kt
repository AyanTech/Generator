package com.alirezabdn.generator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.networking.*

class MainActivity : AppCompatActivity() {

    lateinit var ayanAPI: AyanApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ayanAPI = AyanApi(
            context = this,
            getUserToken = {
                ""
            },
            defaultBaseUrl = "https://application.billingsystem.ayantech.ir/WebServices/Core.svc/",
        )
        ayanAPI.simpleCallOnlyInputApi(OnlyInputApi.Input("test")) { }
        ayanAPI.callOnlyInputApi(OnlyInputApi.Input("test")) {
            success { }
            failure { }
            changeStatusCallback { }
        }
        ayanAPI.simpleCallGetEndUserInquiryHistoryDetail(
            GetEndUserInquiryHistoryDetail.Input("Test")
        ) {

        }
        ayanAPI.callGetEndUserInquiryHistoryDetail(
            GetEndUserInquiryHistoryDetail.Input("Test")
        ) {
            success { }
            failure { }
            changeStatusCallback { }
        }
        ayanAPI.simpleCallOnlyOutputApi {  }
        ayanAPI.callOnlyOutputApi {
            success { }
            failure { }
            changeStatusCallback { }
        }
    }
}