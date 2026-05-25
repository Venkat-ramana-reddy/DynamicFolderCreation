package com.factoryattendance.di

import android.content.Context
import androidx.room.Room
import com.factoryattendance.data.local.AppDatabase
import com.factoryattendance.data.local.dao.AttendanceDao
import com.factoryattendance.data.local.dao.ShiftDao
import com.factoryattendance.data.local.dao.WorkerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "attendance_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideWorkerDao(db: AppDatabase): WorkerDao = db.workerDao()
    @Provides fun provideAttendanceDao(db: AppDatabase): AttendanceDao = db.attendanceDao()
    @Provides fun provideShiftDao(db: AppDatabase): ShiftDao = db.shiftDao()
}
