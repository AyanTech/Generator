package com.alirezabdn.generator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayannetworking.v2.AyanApi

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
    }
}