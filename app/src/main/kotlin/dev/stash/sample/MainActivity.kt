package dev.stash.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.stash.core.Stash
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val output = TextView(this).apply {
            textSize = 18f
            setPadding(40, 60, 40, 40)
            text = "Stash sample\\nTesting storage..."
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(output)
        })

        val stash = Stash.create(applicationContext)

        @Serializable
        data class Settings(
            val is_on: Boolean
        )

        lifecycleScope.launch {
            stash.clear()
            stash.put("username", "moka")
            stash.put("coins", 500)
            stash.save("settings", Settings(true))

            val username = stash.get("username", "Guest")
            val user = stash.load("settings", Settings::class.java)

            output.text = """
            Username: $username
            
            Coins: ${stash.get("coins", 0)}
            
            Settings: ${user?.is_on}
            """.trimIndent()
        }
    }
}
