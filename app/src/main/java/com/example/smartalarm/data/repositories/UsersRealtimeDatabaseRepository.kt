package com.example.smartalarm.data.repositories

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.smartalarm.data.data.AccountData
import com.example.smartalarm.data.data.AlarmData
import com.example.smartalarm.data.data.RecordInternetData
import com.example.smartalarm.data.data.arrayToString
import com.example.smartalarm.data.data.getRecordsList
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UsersRealtimeDatabaseRepository {
    private val usersDatabase = FirebaseDatabase
        .getInstance("https://smartalarm-ccdbb-default-rtdb.europe-west1.firebasedatabase.app/")
        .getReference("users")

    private val topRecordsDatabase = FirebaseDatabase
        .getInstance("https://smartalarm-ccdbb-default-rtdb.europe-west1.firebasedatabase.app/")
        .getReference("top_records")

    suspend fun addUser(account: AccountData?) = withContext(Dispatchers.IO) {
        if (account != null) {
            usersDatabase.child(account.uid!!).get().addOnSuccessListener {
                Log.i("firebase", "Got value")
                if (!it.exists())
                    usersDatabase.child(account.uid!!).setValue(account)
            }
        }
    }

    suspend fun getUser(uri: String, user: MutableLiveData<AccountData>) =
        withContext(Dispatchers.IO) {
            usersDatabase.child(uri).get().addOnSuccessListener {
                user.postValue(it.getValue(AccountData::class.java))
            }
        }

    suspend fun getTopRecords(userList: MutableLiveData<List<AccountData>>) =
        withContext(Dispatchers.IO) {
            topRecordsDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.postValue(
                        snapshot.children.map { dataSnapshot ->
                            dataSnapshot.getValue(AccountData::class.java)!!
                        }
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("firebase", error.toString())
                }

            })
        }

    suspend fun getAllUsers(userList: MutableLiveData<List<AccountData>>) =
        withContext(Dispatchers.IO) {
            usersDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.postValue(
                        snapshot.children.map { dataSnapshot ->
                            dataSnapshot.getValue(AccountData::class.java)!!
                        }
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("firebase", error.toString())
                }

            })
        }

    suspend fun updateUserRecords(account: AccountData, recordInternetData: RecordInternetData) =
        withContext(Dispatchers.IO) {
            val recordUserDb = usersDatabase.child(account.uid!!)
            val topRecordDb = topRecordsDatabase.child(recordInternetData.gameId.toString())
            recordUserDb.get().addOnSuccessListener {
                val current = it.getValue(AccountData::class.java)

                if (current?.records == "null") {
                    recordInternetData.id = 0
                    current.records = recordInternetData.toString()
                } else {
                    val recordsList = getRecordsList(current?.records!!)
                    recordInternetData.id = recordsList[recordsList.size - 1]?.id!! + 1
                    current.records += "/$recordInternetData"
                }

                recordUserDb.updateChildren(
                    mapOf(
                        "email" to current.email,
                        "name" to current.name,
                        "photo" to current.photo,
                        "records" to current.records,
                        "uid" to current.uid
                    )
                ).addOnSuccessListener {
                    topRecordDb.get().addOnSuccessListener {
                        current.records = recordInternetData.toString()
                        if (it.exists()) {
                            if (RecordInternetData(it.getValue(AccountData::class.java)?.records!!).record!! <
                                recordInternetData.record!!
                            )
                                topRecordDb.updateChildren(
                                    mapOf(
                                        "email" to current.email,
                                        "name" to current.name,
                                        "photo" to current.photo,
                                        "records" to current.records,
                                        "uid" to current.uid
                                    )
                                )
                        } else {
                            topRecordDb.setValue(current)
                        }
                    }

                }
            }
        }

    suspend fun addAlarmsToUser(
        account: AccountData,
        alarms: ArrayList<AlarmData>,
        result: MutableLiveData<Boolean?>
    ) =
        withContext(Dispatchers.IO) {
            deleteAlarmsOfUser(account)
            val userAlarms = usersDatabase.child(account.uid!!).child("alarms")
            for (alarm in alarms) {
                userAlarms.child(alarm.id.toString()).setValue(alarm).addOnCompleteListener {
                    result.postValue(it.isSuccessful)
                }
            }
        }

    suspend fun deleteAlarmsOfUser(account: AccountData) = withContext(Dispatchers.IO) {
        usersDatabase.child(account.uid!!).child("alarms").removeValue()
    }

    suspend fun getAlarms(account: AccountData, alarmsList: MutableLiveData<List<AlarmData>>) =
        withContext(Dispatchers.IO) {
            val userAlarms = usersDatabase.child(account.uid!!).child("alarms")
            userAlarms.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    alarmsList.postValue(
                        snapshot.children.map { dataSnapshot ->
                            dataSnapshot.getValue(AlarmData::class.java)!!
                        }
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("firebase", error.toString())
                }
            })
        }

    suspend fun deleteRecordOfUser(accountData: AccountData) =
        withContext(Dispatchers.IO) {
            val newRecord = RecordInternetData(accountData.records!!)
            val userRecords = usersDatabase.child(accountData.uid!!).child("records")
            userRecords.get().addOnSuccessListener {
                val current = it.getValue(String::class.java)
                val records = getRecordsList(current!!)
                for (i in records.indices) {
                    if (records[i]?.id!! == newRecord.id)
                        records.removeAt(i)
                }
                userRecords.setValue(arrayToString(records))
            }

            val topOfGame = topRecordsDatabase.child(newRecord.gameId.toString())
            topOfGame.get().addOnSuccessListener {
                val currentTop = it.getValue(AccountData::class.java)
                val currentRecord = RecordInternetData(currentTop?.records!!)

                if (currentTop.uid == accountData.uid && currentRecord.id == newRecord.id) {
                    currentTop.name = "Аноним"
                    currentTop.uid = ""
                    currentTop.email = ""
                    currentTop.photo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRHH_48-fhI2OTTKlHo-FagFrwi3LcF6gf8jx142YctSw&s"
                }
            }
        }
}