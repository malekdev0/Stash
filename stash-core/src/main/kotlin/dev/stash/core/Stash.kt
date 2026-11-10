
package dev.stash.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/**
 * Stash - Simple Android storage.
 *
 * Supports primitive values and Kotlin objects.
 */
class Stash private constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val gson = Gson()

    companion object {
        private val stores =
            ConcurrentHashMap<String, DataStore<Preferences>>()

        @JvmStatic
        fun create(
            context: Context,
            fileName: String = "stash.preferences_pb"
        ): Stash {
            require(fileName.isNotBlank()) {
                "fileName cannot be blank."
            }

            val appContext = context.applicationContext
            val file = appContext.preferencesDataStoreFile(fileName)

            val dataStore = stores.computeIfAbsent(file.absolutePath) {
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(
                        SupervisorJob() + Dispatchers.IO
                    ),
                    produceFile = { file }
                )
            }

            return Stash(dataStore)
        }
    }

    // Primitive storage

    suspend fun put(key: String, value: Any) {
        requireKey(key)

        val typedKey = value.toPreferenceKey(key)

        dataStore.edit { preferences ->
            removeAllTypes(preferences, key)

            when (typedKey) {
                is TypedKey.StringKey ->
                    preferences[typedKey.key] = value as String

                is TypedKey.IntKey ->
                    preferences[typedKey.key] = value as Int

                is TypedKey.LongKey ->
                    preferences[typedKey.key] = value as Long

                is TypedKey.BooleanKey ->
                    preferences[typedKey.key] = value as Boolean

                is TypedKey.FloatKey ->
                    preferences[typedKey.key] = value as Float

                is TypedKey.DoubleKey ->
                    preferences[typedKey.key] = value as Double
            }
        }
    }

    suspend fun <T : Any> get(key: String, default: T): T {
        requireKey(key)
        return readValue(dataStore.data.first(), key, default)
    }

    fun <T : Any> observe(key: String, default: T): Flow<T> {
        requireKey(key)

        return dataStore.data.map { preferences ->
            readValue(preferences, key, default)
        }
    }

    // Object storage

    suspend fun <T : Any> save(key: String, value: T) {
        requireKey(key)

        val json = gson.toJson(value)

        dataStore.edit { preferences ->
            removeAllTypes(preferences, key)
            preferences[objectKey(key)] = json
        }
    }

    suspend fun <T : Any> load(
        key: String,
        type: Class<T>
    ): T? {
        requireKey(key)

        val json = dataStore.data.first()[objectKey(key)]
            ?: return null

        return gson.fromJson(json, type)
    }

    // Edit

    /**
     * Replaces the value stored under key with newValue.
     * Works with primitives and objects.
     */
    suspend fun <T : Any> edit(
        key: String,
        oldValue: T,
        newValue: T
    ) {
        requireKey(key)

        require(oldValue::class == newValue::class) {
            "oldValue and newValue must have the same type."
        }

        when (newValue) {
            is String,
            is Int,
            is Long,
            is Boolean,
            is Float,
            is Double -> put(key, newValue)

            else -> save(key, newValue)
        }
    }

    // Removal

    suspend fun remove(key: String) {
        requireKey(key)

        dataStore.edit { preferences ->
            removeAllTypes(preferences, key)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    // Internal helpers

    private fun requireKey(key: String) {
        require(key.isNotBlank()) {
            "key cannot be blank."
        }
    }

    private fun objectKey(key: String) =
        stringPreferencesKey("__stash_object__:$key")

    private fun removeAllTypes(
        preferences: MutablePreferences,
        key: String
    ) {
        preferences.remove(stringPreferencesKey(key))
        preferences.remove(intPreferencesKey(key))
        preferences.remove(longPreferencesKey(key))
        preferences.remove(booleanPreferencesKey(key))
        preferences.remove(floatPreferencesKey(key))
        preferences.remove(doublePreferencesKey(key))
        preferences.remove(objectKey(key))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> readValue(
        preferences: Preferences,
        key: String,
        default: T
    ): T {
        val result: Any = when (default) {
            is String -> preferences[stringPreferencesKey(key)] ?: default
            is Int -> preferences[intPreferencesKey(key)] ?: default
            is Long -> preferences[longPreferencesKey(key)] ?: default
            is Boolean -> preferences[booleanPreferencesKey(key)] ?: default
            is Float -> preferences[floatPreferencesKey(key)] ?: default
            is Double -> preferences[doublePreferencesKey(key)] ?: default

            else -> throw IllegalArgumentException(
                "Unsupported default type: ${default::class.qualifiedName}"
            )
        }

        return result as T
    }

    private fun Any.toPreferenceKey(key: String): TypedKey =
        when (this) {
            is String -> TypedKey.StringKey(stringPreferencesKey(key))
            is Int -> TypedKey.IntKey(intPreferencesKey(key))
            is Long -> TypedKey.LongKey(longPreferencesKey(key))
            is Boolean -> TypedKey.BooleanKey(booleanPreferencesKey(key))
            is Float -> TypedKey.FloatKey(floatPreferencesKey(key))
            is Double -> TypedKey.DoubleKey(doublePreferencesKey(key))

            else -> throw IllegalArgumentException(
                "Unsupported value type: ${this::class.qualifiedName}. " +
                        "Use save() for objects."
            )
        }

    private sealed interface TypedKey {
        data class StringKey(val key: Preferences.Key<String>) : TypedKey
        data class IntKey(val key: Preferences.Key<Int>) : TypedKey
        data class LongKey(val key: Preferences.Key<Long>) : TypedKey
        data class BooleanKey(val key: Preferences.Key<Boolean>) : TypedKey
        data class FloatKey(val key: Preferences.Key<Float>) : TypedKey
        data class DoubleKey(val key: Preferences.Key<Double>) : TypedKey
    }
}
