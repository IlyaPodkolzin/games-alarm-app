package com.example.smartalarm.data.data

import com.example.smartalarm.data.db.AlarmSimpleData
import com.example.smartalarm.data.repositories.getNearestDate
import java.time.LocalDateTime

data class AlarmData(
    var alarmSimpleData: AlarmSimpleData,
    var gamesList: ArrayList<Int>
) {
    var localDateTime: LocalDateTime

    init {
        val date = getNearestDate(alarmSimpleData.dayOfWeek)
        localDateTime = LocalDateTime.of(
            date[0],
            date[1],
            date[2],
            alarmSimpleData.timeHour,
            alarmSimpleData.timeMinute
        )
    }
}
