package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val firstNumEditText: EditText = findViewById(R.id.firstNumEditText)
        val secondNumEditText: EditText = findViewById(R.id.secondNumEditText)
        val addButton: Button = findViewById(R.id.AddBtn)
        val resultTextView: TextView = findViewById(R.id.resultTextView)

        addButton.setOnClickListener {
            val firstNum = firstNumEditText.text.toString().toIntOrNull()
            val secondNum = secondNumEditText.text.toString().toIntOrNull()
            if (firstNum != null && secondNum != null) {
                val sum = firstNum + secondNum //test
                resultTextView.text = "Result: $sum"
            } else {
                resultTextView.text = "Please enter valid numbers!"
            }
        }
    }
}
