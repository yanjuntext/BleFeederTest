package com.jk.blefeeder.room

import androidx.room.*
import com.jk.blefeeder.bean.LocalSet

@Dao
interface LocalSetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(localSet:LocalSet)

    @Query("DELETE FROM localset")
    fun deleteAll()

    @Query("Select * from localset")
    fun queryAll():MutableList<LocalSet>

    @Update
    fun update(localSet:LocalSet)

}