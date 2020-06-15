package com.jk.blefeeder.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LocalSet(
    var bleName: String?,
    var bleRssi: String?,
    var bleVersion: String?,
    var feedNum: String?,
    var recordTime: String?
) {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}