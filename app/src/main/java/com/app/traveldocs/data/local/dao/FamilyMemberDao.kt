package com.app.traveldocs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.traveldocs.data.local.entity.FamilyMemberEntity

@Dao
interface FamilyMemberDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(member: FamilyMemberEntity)

    @Query("SELECT * FROM family_members WHERE id = :memberId")
    suspend fun getById(memberId: String): FamilyMemberEntity?

    @Update
    suspend fun update(member: FamilyMemberEntity)

    @Query("SELECT * FROM family_members")
    suspend fun getAll(): List<FamilyMemberEntity>

    @Query("DELETE FROM family_members WHERE id = :memberId")
    suspend fun delete(memberId: String)
}
