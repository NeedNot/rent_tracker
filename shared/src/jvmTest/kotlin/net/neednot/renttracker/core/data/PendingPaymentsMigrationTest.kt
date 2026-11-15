package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingPaymentsMigrationTest {
    private val directory = createTempDirectory("migration").toFile()
    private val databaseFile = File(directory, RentTrackerDatabase.FILE_NAME)

    @AfterTest
    fun deleteDatabase() {
        directory.deleteRecursively()
    }

    @Test
    fun versionSixOpensAsVersionSevenWithAnEmptyQueue() =
        runTest {
            BundledSQLiteDriver().open(databaseFile.path).use { connection ->
                connection.createSchema(version = 6)
                connection.execSQL(
                    "INSERT INTO pending_payments VALUES ('c1', 't1', 150000, '2026-03-02', 'Check', NULL, 'Pending')",
                )
            }

            val database =
                Room
                    .databaseBuilder<RentTrackerDatabase>(name = databaseFile.path)
                    .setDriver(BundledSQLiteDriver())
                    .addMigrations(PendingPaymentsMigration)
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .build()

            assertEquals(emptyList(), database.pendingPaymentDao().observeAll().first())
            database.close()
        }

    private fun SQLiteConnection.createSchema(version: Int) {
        val schema = Json.parseToJsonElement(File(SCHEMA_DIRECTORY, "$version.json").readText()).jsonObject
        val database = schema.getValue("database").jsonObject
        database
            .getValue("entities")
            .jsonArray
            .map { it.jsonObject }
            .forEach { entity -> createEntity(entity) }
        database.getValue("setupQueries").jsonArray.forEach { query -> execSQL(query.jsonPrimitive.content) }
        execSQL("PRAGMA user_version = $version")
    }

    private fun SQLiteConnection.createEntity(entity: JsonObject) {
        val tableName = entity.getValue("tableName").jsonPrimitive.content
        execSQL(
            entity
                .getValue("createSql")
                .jsonPrimitive.content
                .withTable(tableName),
        )
        entity["indices"]?.jsonArray?.forEach { index ->
            execSQL(
                index.jsonObject
                    .getValue("createSql")
                    .jsonPrimitive.content
                    .withTable(tableName),
            )
        }
    }

    private fun String.withTable(tableName: String): String = replace("\${TABLE_NAME}", tableName)

    private companion object {
        const val SCHEMA_DIRECTORY = "schemas/net.neednot.renttracker.core.data.RentTrackerDatabase"
    }
}
