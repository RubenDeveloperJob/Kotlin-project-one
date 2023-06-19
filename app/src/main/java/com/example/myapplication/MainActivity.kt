package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var buttonGreet: Button
    private lateinit var editTextName: EditText
    private lateinit var textViewGreeting: TextView
    private lateinit var imageViewBackground: ImageView

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonGreet = findViewById(R.id.buttonGreet)
        editTextName = findViewById(R.id.editTextName)
        textViewGreeting = findViewById(R.id.textViewGreeting)
        imageViewBackground = findViewById(R.id.imageViewBackground)

        dbHelper = DatabaseHelper(this)

        buttonGreet.setOnClickListener {
            val name = editTextName.text.toString()
            if (name.isEmpty()) {
                Toast.makeText(applicationContext, "No hay nombre", Toast.LENGTH_SHORT).show()
            } else {
                val greeting = getGreetingByTimeOfDay() + " $name! "
                Toast.makeText(applicationContext, greeting, Toast.LENGTH_SHORT).show()
                textViewGreeting.text = greeting
                textViewGreeting.visibility = View.VISIBLE

                val fadeInAnimation = AlphaAnimation(0f, 1f)
                fadeInAnimation.duration = 3000
                textViewGreeting.startAnimation(fadeInAnimation)

                val backgroundImageId = when (getGreetingByTimeOfDay()) {
                    "Buenos días" -> R.drawable.sol
                    "Buenas tardes" -> R.drawable.tarde
                    "Buenas noches" -> R.drawable.noche
                    else -> R.drawable.welcome
                }
                imageViewBackground.setImageResource(backgroundImageId)

                val currentTime =
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                saveGreetingToDatabase(name, currentTime, greeting)
                showGreetingsHistory()
            }
        }
    }

    private fun showGreetingsHistory() {
        val dbHelper = DatabaseHelper(this)
        val greetings = dbHelper.getAllGreetings()
        var contador=0

        // Aquí puedes mostrar los saludos en una lista, en un TextView o en cualquier otro componente visual
        for (greeting in greetings) {
            println("${greeting.name} - ${greeting.time}: ${greeting.message}$contador")
            contador=contador+1
        }
    }

    private fun getGreetingByTimeOfDay(): String {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hourOfDay) {
            in 0..11 -> "Buenos días"
            in 12..17 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    private fun saveGreetingToDatabase(name: String, time: String, message: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NAME, name)
            put(DatabaseHelper.COLUMN_TIME, time)
            put(DatabaseHelper.COLUMN_MESSAGE, message)
        }
        val newRowId = db.insert(DatabaseHelper.TABLE_NAME, null, values)
        if (newRowId == -1L) {
            Toast.makeText(applicationContext, "Error al guardar el saludo", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "GreetingDatabase"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "greetings"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_TIME = "time"
        const val COLUMN_MESSAGE = "message"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME " +
                "($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT, " +
                "$COLUMN_TIME TEXT, " +
                "$COLUMN_MESSAGE TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME"
        db.execSQL(dropTableQuery)
        onCreate(db)
    }

    fun getAllGreetings(): List<Greeting> {
        val greetings = mutableListOf<Greeting>()
        val selectQuery = "SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC"
        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(COLUMN_ID))
                val name = it.getString(it.getColumnIndex(COLUMN_NAME))
                val time = it.getString(it.getColumnIndex(COLUMN_TIME))
                val message = it.getString(it.getColumnIndex(COLUMN_MESSAGE))
                val greeting = Greeting(id, name, time, message)
                greetings.add(greeting)
            }
        }
        return greetings
    }
}

data class Greeting(
    val id: Long,
    val name: String,
    val time: String,
    val message: String
)