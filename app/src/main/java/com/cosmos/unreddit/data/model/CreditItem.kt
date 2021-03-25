package com.cosmos.unreddit.data.model

import androidx.annotation.StringRes

sealed class CreditItem {

    data class Section(@StringRes val title: Int) : CreditItem()

    data class Credit(
        val title: String,

        val author: String,

        val description: String,

        val link: String,

        val license: LicenseType,

        val licenseLink: String
    ) : CreditItem() {
        enum class LicenseType(val license: String) {
            APACHE_V2("Apache License 2.0"),
            MIT("MIT License"),
            OPEN_FONT_V1_1("SIL Open Font License v1.10"),
            OTHER("")
        }
    }
}
