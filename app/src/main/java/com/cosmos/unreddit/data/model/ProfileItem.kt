package com.cosmos.unreddit.data.model

import com.cosmos.unreddit.data.model.db.Profile

sealed class ProfileItem {

    data class UserProfile(val profile: Profile) : ProfileItem()

    object NewProfile : ProfileItem()
}
