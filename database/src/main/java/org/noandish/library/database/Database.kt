package org.noandish.library.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by AliasgahrMirzazade on 10/10/2018 AD.
 */
class Database(context: Context, val table: Table) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private var handler = Handler(Looper.getMainLooper())
    private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + table.table_name

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(getQuerySalCreateEntries(table))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    @Throws(SQLiteConstraintException::class)
    fun insert(tableItem: HashMap<String, Any>, response: InsertResponse) {
        Thread {
            // Gets the data repository in write mode
            val db = writableDatabase
            if (tableItem.containsKey(KEY_ID)) {
                throw Exception("Can't use Database.KEY_ID or id string for key")
            }
            val values = ContentValues()
            for (item in tableItem) {
                when {
                    item.value is String -> values.put(item.key, item.value as String)
                    item.value is Int -> values.put(item.key, item.value as Int)
                    item.value is Short -> values.put(item.key, item.value as Short)
                    item.value is Long -> values.put(item.key, item.value as Long)
                    item.value is Float -> values.put(item.key, item.value as Float)
                    item.value is Double -> values.put(item.key, item.value as Double)
                    item.value is Byte -> values.put(item.key, item.value as Byte)
                    item.value is Boolean -> values.put(item.key, item.value as Boolean)
                    item.value is ByteArray -> values.put(item.key, item.value as ByteArray)
                    item.value is JSONObject -> values.put(item.key, "${(item.value as JSONObject)}")
                    item.value is JSONArray -> values.put(item.key, (item.value as JSONArray).toString())
                }
            }
            // Insert the new row, returning the primary key value of the new row
            val newRowId = db.insert(table.table_name, null, values)
            handler.post {
                response.inserted(newRowId)
            }
        }.start()
    }

    fun insertAll(tableItems: ArrayList<HashMap<String, Any>>, response: InsertAllResponse) {
        Thread {
            // Gets the data repository in write mode
            val db = writableDatabase
            var resulte_success_insert = ArrayList<InsertResponseItem>()
            for (tableItem in tableItems) {
                if (tableItem.containsKey(KEY_ID)) {
                    throw Exception("Can't use Database.KEY_ID or id string for key")
                }
                val values = ContentValues()
                for (item in tableItem) {
                    when {
                        item.value is String -> values.put(item.key, item.value as String)
                        item.value is Int -> values.put(item.key, item.value as Int)
                        item.value is Short -> values.put(item.key, item.value as Short)
                        item.value is Long -> values.put(item.key, item.value as Long)
                        item.value is Float -> values.put(item.key, item.value as Float)
                        item.value is Double -> values.put(item.key, item.value as Double)
                        item.value is Byte -> values.put(item.key, item.value as Byte)
                        item.value is Boolean -> values.put(item.key, item.value as Boolean)
                        item.value is ByteArray -> values.put(item.key, item.value as ByteArray)
                        item.value is JSONObject -> values.put(item.key, "${(item.value as JSONObject)}")
                        item.value is JSONArray -> values.put(item.key, (item.value as JSONArray).toString())
                    }
                }
                // Insert the new row, returning the primary key value of the new row
                Log.w("inser2t", values.toString())
                val newRowId = InsertResponseItem(db.insert(table.table_name, null, values),true)
                resulte_success_insert.add(newRowId)
            }

            handler.post {
                response.insertedAll(resulte_success_insert)
            }
        }.start()
    }

    /**
     * hashmap.put(KEY_TABLE_NAME,string) and hashmap.put(KEY_ID,int)
     *@sample delete(hashmap,response)
     *@return [response] is boolean value for success or field
     */
    @Throws(SQLiteConstraintException::class)
    fun delete(hashMap: HashMap<String, Any>, response: DeleteResponse) {
        if (!hashMap.containsKey(KEY_ID)) {
            throw Exception("KEY_ID can't find ; should put KEY_ID in HashMap")
        }
        Thread {
            val hashMaps = ArrayList<HashMap<String, Any>>()
            hashMaps.add(hashMap)

            val ids = arrayOfNulls<String>(hashMaps.size)
            for (item in 0 until hashMaps.size) {
                ids[item] = hashMaps[item].toString()
            }
            val selection = "$KEY_ID LIKE ?"
            // Specify arguments in placeholder order.
            val db = writableDatabase

            val result = db.delete(table.table_name, selection, ids)

            handler.post {
                response.deleted(result)
            }
        }.start()
    }

    /**
     * [deleteUser] return All id Success deleted
     */

    fun delete(hashMaps: ArrayList<HashMap<String, Any>>, response: DeleteArrayResponse) {
        Thread {

            val ids = arrayOfNulls<String>(hashMaps.size)
            for (item in 0 until hashMaps.size) {
                ids[item] = hashMaps[item].toString()
            }
            val selection = "$KEY_ID LIKE ?"
            // Specify arguments in placeholder order.
            val db = writableDatabase

            val result = db.delete(table.table_name, selection, ids)
            handler.post {
                response.deleted(result)
            }
        }.start()

    }
    fun deleteAll(response: DeleteResponse) {
        Thread {
            // Specify arguments in placeholder order.
            val db = writableDatabase
            val result = db.delete(table.table_name, null, null)
            handler.post {
                response.deleted(result)
            }
        }.start()

    }

    fun read(id: Int, response: ReadResponse) {
        Thread {
            val item = HashMap<String, Any>()
            val db = writableDatabase
            var cursor: Cursor? = null
            try {
                cursor = db.rawQuery("select * from ${table.table_name} WHERE $KEY_ID='$id'", null)
            } catch (e: SQLiteException) {
                // if table not yet present, create it
                db.execSQL(getQuerySalCreateEntries(table))
            }

            if (cursor!!.moveToFirst()) {
                while (cursor.isAfterLast == false) {
                    for (columnNames in cursor.columnNames) {
                        item[columnNames] = cursor.getColumnIndex(columnNames)
                    }
                    cursor.moveToNext()
                }

            }

            handler.post({
                response.read(item)
            })
        }.start()
    }

    fun readAll(response: ReadResponseAll) {
        val items = ArrayList<HashMap<String, Any>>()
        val db = writableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("select * from ${table.table_name}", null)
        } catch (e: SQLiteException) {
            db.execSQL(getQuerySalCreateEntries(table))
            response.read(ArrayList())
        }

        if (cursor!!.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                val item = HashMap<String, Any>()
                for (columnName in cursor.columnNames) {
                    item[columnName] = cursor.getString(cursor.getColumnIndex(columnName))
                }
                items.add(item)
                cursor.moveToNext()
            }
        }
        handler.post({
            response.read(items)
        })
    }

    /**
     * [rows] is  HashMap< nameRow , TypeRow >
     */
    fun getQuerySalCreateEntries(table: Table): String {
        var sql_create_entries = "CREATE TABLE " + table.table_name + " (" +
                KEY_ID + "  integer primary key autoincrement, "
        for (items in table.rows) {
            when (items.type_row) {
                Row.TYPE_STRING -> {
                    sql_create_entries += " ${items.name_row} text,"
                }
                Row.TYPE_INTEGER -> {
                    sql_create_entries += " ${items.name_row} integer,"
                }
            }
        }
        if (table.rows.size > 0 && sql_create_entries.substring(sql_create_entries.length - 1, sql_create_entries.length).equals(","))
            sql_create_entries = sql_create_entries.substring(0, sql_create_entries.length - 1)
        return "$sql_create_entries )"
    }

    companion object {
        val DATABASE_VERSION = 1
        val DATABASE_NAME = "FeedReader.db"
        val KEY_ID = "id"
    }

}