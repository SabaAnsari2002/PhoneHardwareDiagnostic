package com.phonehardware.diagnostic.di

import android.content.Context
import androidx.room.Room
import com.phonehardware.diagnostic.data.LocalDatabase
import com.phonehardware.diagnostic.data.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Modules {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): LocalDatabase =
        Room.databaseBuilder(context, LocalDatabase::class.java, "diagnostics.db").build()
    @Provides fun dao(db: LocalDatabase): SessionDao = db.dao()
}
