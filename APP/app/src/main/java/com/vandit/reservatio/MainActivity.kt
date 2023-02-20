package com.vandit.reservatio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vandit.reservatio.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}