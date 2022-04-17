package com.cosmos.unreddit.data.model

import com.cosmos.unreddit.data.model.backup.BackupType

data class BackupTypeItem(
    val type: BackupType,

    var selected: Boolean = false
)
