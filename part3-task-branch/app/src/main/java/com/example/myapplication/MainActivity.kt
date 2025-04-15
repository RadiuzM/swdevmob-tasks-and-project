package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val secondActivityBtn: Button = findViewById(R.id.secondActivityBtn)
        secondActivityBtn.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            intent.putExtra("message.SOMETHING", "HELLO YOU")
            startActivity(intent)
        }

        val googleBtn: Button = findViewById(R.id.googleBtn)
        googleBtn.setOnClickListener {
            val google = "https://www.google.com"
            val webAddress = Uri.parse(google)
            val goToGoogle = Intent(Intent.ACTION_VIEW, webAddress)

            startActivity(goToGoogle)
        }
    }
}
