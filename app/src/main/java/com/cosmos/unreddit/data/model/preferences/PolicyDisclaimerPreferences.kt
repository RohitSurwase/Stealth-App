package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

data class PolicyDisclaimerPreferences(
    val policyDisclaimerShown: Boolean
) {
    object PreferencesKeys {
        val POLICY_DISCLAIMER_SHOWN = booleanPreferencesKey("policy_disclaimer_shown")
    }
}
