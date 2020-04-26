package com.example.menagerie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText

const val IP_ADDRESS = "com.example.menagerie.IP_ADDRESS"
const val PORT = "com.example.menagerie.PORT"

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun connect(view : View) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra(IP_ADDRESS, findViewById<EditText>(R.id.ipText).text)
            putExtra(PORT, findViewById<EditText>(R.id.portText).text)
        })
    }

}
