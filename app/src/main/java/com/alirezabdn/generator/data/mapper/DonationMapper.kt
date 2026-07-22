package com.alirezabdn.generator.data.mapper

import com.alirezabdn.generator.data.remote.dto.DonationServiceDTO
import com.alirezabdn.generator.domain.model.ReferrerType

/** Maps the remote DTO into the UI model consumed by presentation. */
internal fun DonationServiceDTO.DonationResponseModel.toUIModel(): List<ReferrerType> {
    val items = mutableListOf<ReferrerType>()
    this.referrerTypeList?.forEach { item ->
        items.add(
            ReferrerType(
                name = item.name,
                displayName = item.showName,
            )
        )
    }
    return items

}
