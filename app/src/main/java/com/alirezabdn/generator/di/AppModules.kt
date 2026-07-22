package com.alirezabdn.generator.di

import com.alirezabdn.generator.domain.usecase.GetDonationReferrerTypesUseCaseImpl
import com.alirezabdn.generator.presentation.DonationViewModel
import ir.ayantech.networking.datasource.DonationRemoteDataSource
import ir.ayantech.networking.datasource.impl.DonationRemoteDataSourceImpl
import ir.ayantech.networking.repository.DonationRepository as GeneratedDonationRepository
import ir.ayantech.networking.repository.impl.DonationRepositoryImpl as GeneratedDonationRepositoryImpl
import ir.ayantech.networking.v2.AyanApi
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private const val BASE_URL =
    "https://application.billingsystem.ayantech.ir/WebServices/Core.svc/"

/** Provides Ayan Networking and the KSP-generated data layer. */
val networkModule = module {
    single {
        AyanApi.Builder(
            context = get(),
            baseUrl = BASE_URL,
        )
            .setInvokeUserToken { "user_token" }
            .build()
    }
    single<DonationRemoteDataSource> {
        DonationRemoteDataSourceImpl(ayanApi = get())
    }
    single<GeneratedDonationRepository> {
        GeneratedDonationRepositoryImpl(remoteDataSource = get())
    }
}

/** Binds the generated repository to domain abstractions and use cases. */
val domainModule = module {
    factory {
        GetDonationReferrerTypesUseCaseImpl(repository = get())
    }
}

/** Provides lifecycle-aware presentation dependencies. */
val presentationModule = module {
    viewModelOf(::DonationViewModel)
}

val appModules = listOf(networkModule, domainModule, presentationModule)
