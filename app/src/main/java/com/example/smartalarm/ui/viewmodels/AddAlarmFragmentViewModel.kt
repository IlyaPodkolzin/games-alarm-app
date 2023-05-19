package com.example.smartalarm.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartalarm.data.data.AlarmData
import com.example.smartalarm.data.db.ALL_GAMES
import com.example.smartalarm.data.db.AlarmSimpleData
import com.example.smartalarm.data.db.AlarmsDB
import com.example.smartalarm.data.repositories.AlarmCreateRepository
import com.example.smartalarm.data.repositories.AlarmDbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddAlarmFragmentViewModel(application: Application) : AndroidViewModel(application) {

    var currentAlarm: AlarmData? = null
    var gamesList: ArrayList<Int> = ArrayList()

    init {
        for (i in ALL_GAMES.indices)
            gamesList.add(1)
    }

    private val alarmDbRepository = AlarmDbRepository(
        AlarmsDB.getInstance(getApplication())?.alarmsDao()!!
    )

    suspend fun insertOrUpdateAlarmToDb(
        timeHour: Int,
        timeMinute: Int,
        dayOfWeek: Int,
        name: String,
        isVibration: Boolean,
        isRisingVolume: Boolean,
        activateDate: String?
    ) = withContext(Dispatchers.IO) {
        val alarm = AlarmSimpleData(
            timeHour = timeHour,
            timeMinute = timeMinute,
            dayOfWeek = dayOfWeek,
            name = name.ifEmpty { "Будильник" },
            activateDate = activateDate,
            isVibration = isVibration,
            isRisingVolume = isRisingVolume,
            recordScore = null,
            recordSeconds = null
        )
        if (currentAlarm == null) {

            alarmDbRepository.insertAlarmToDb(AlarmData(alarm, gamesList))
//            val creator = AlarmCreateRepository(getApplication<Application>().applicationContext)
//            alarm?.let(creator::schedule)
        }
        else {
            alarm.id = currentAlarm!!.alarmSimpleData.id
            alarm.recordSeconds = currentAlarm!!.alarmSimpleData.recordSeconds
            alarm.recordScore = currentAlarm!!.alarmSimpleData.recordScore
            alarmDbRepository.updateAlarmInDbWithGames(AlarmData(alarm, gamesList))
        }
    }

    suspend fun getAlarm(id: Long): AlarmData = withContext(Dispatchers.IO) {
        return@withContext alarmDbRepository.getAlarmWithGames(id)
    }
}