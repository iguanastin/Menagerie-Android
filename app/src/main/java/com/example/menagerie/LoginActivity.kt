package com.example.menagerie

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

const val ADDRESS = "com.example.menagerie.IP_ADDRESS"

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        findViewById<EditText>(R.id.addressText).onSubmit { findViewById<Button>(R.id.connectButton).performClick() }
    }

    fun connect(view: View) {
        val address: CharSequence = findViewById<EditText>(R.id.addressText).text

        if (Pattern.matches("[a-zA-Z0-9.\\-]+:[0-9]+", address)) {
            startActivity(Intent(this, SearchActivity::class.java).apply {
                putExtra(ADDRESS, address)
            })
        } else {
            AlertDialog.Builder(view.context).setTitle("Error")
                .setMessage(
                    "Invalid address. Expected format:\n" +
                            "   example.com:12345\n" +
                            "or\n" +
                            "   123.45.67.89:12345"
                )
                .setNeutralButton("Ok") { _: DialogInterface?, _: Int -> findViewById<EditText>(R.id.addressText).requestFocus() }
                .create().show()
        }
    }

}
