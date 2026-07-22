package com.alirezabdn.generator.data.mapper

import com.alirezabdn.generator.data.remote.dto.DonationServiceDTO
import com.alirezabdn.generator.domain.model.ReferrerType
import org.junit.Assert.assertEquals
import org.junit.Test

class DonationMapperTest {

    @Test
    fun `toUIModel maps every referrer type and preserves order`() {
        val response = DonationServiceDTO.DonationResponseModel(
            referrerTypeList = listOf(
                DonationServiceDTO.ReferrerType(name = "friend", showName = "Friend"),
                DonationServiceDTO.ReferrerType(name = "search", showName = "Search engine"),
            ),
        )

        val result = response.toUIModel()

        assertEquals(
            listOf(
                ReferrerType(name = "friend", displayName = "Friend"),
                ReferrerType(name = "search", displayName = "Search engine"),
            ),
            result,
        )
    }

    @Test
    fun `toUIModel returns an empty list when referrer types are absent`() {
        val response = DonationServiceDTO.DonationResponseModel(referrerTypeList = null)

        assertEquals(emptyList<ReferrerType>(), response.toUIModel())
    }
}
