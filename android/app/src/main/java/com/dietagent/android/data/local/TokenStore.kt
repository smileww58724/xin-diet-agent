package com.dietagent.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

/** JWT token 与登录信息的本地持久化（对应 Web 端 localStorage） */
class TokenStore(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USERNAME = stringPreferencesKey("username")
        val NICKNAME = stringPreferencesKey("nickname")
        val USER_ID = longPreferencesKey("userId")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }

    suspend fun getToken(): String? = context.dataStore.data.first()[Keys.TOKEN]
    suspend fun getUserId(): Long? = context.dataStore.data.first()[Keys.USER_ID]

    suspend fun save(token: String, username: String, nickname: String?, userId: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USERNAME] = username
            prefs[Keys.NICKNAME] = nickname ?: username
            prefs[Keys.USER_ID] = userId
        }
    }

    suspend fun saveNickname(nickname: String) {
        context.dataStore.edit { it[Keys.NICKNAME] = nickname }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
