package com.ayham.player

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.widget.*
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.util.concurrent.Executors

data class MediaItemData(val title: String, val url: String, val kind: String, val image: String? = null)

class MainActivity : Activity() {
    private val purple = Color.rgb(139, 92, 246)
    private val bg = Color.rgb(16, 11, 28)
    private val panel = Color.rgb(31, 23, 49)
    private val client = OkHttpClient()
    private val io = Executors.newSingleThreadExecutor()
    private var server = ""
    private var username = ""
    private var password = ""
    private var language = "en"
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var lastItems: List<MediaItemData> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun baseLayout(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(34, 24, 34, 24)
            setBackgroundColor(bg)
            isFocusableInTouchMode = true
        }
        return root
    }

    private fun label(text: String, size: Float, color: Int = Color.WHITE): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
        }

    private fun field(hint: String, secret: Boolean = false): EditText =
        EditText(this).apply {
            this.hint = hint
            setSingleLine(true)
            textSize = 18f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(18, 10, 18, 10)
            setBackgroundColor(panel)
            if (secret) inputType = 129
        }

    private fun button(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple))
            setOnClickListener { action() }
            isFocusable = true
        }

    private fun showLogin() {
        baseLayout()
        root.gravity = Gravity.CENTER
        root.addView(label("AYHAM PLAYER", 34f, Color.rgb(196,181,253)))
        root.addView(label("YOUR ENTERTAINMENT. YOUR WAY.", 14f, Color.LTGRAY))
        val serverInput = field("Server URL (e.g. https://example.com:8080)")
        val userInput = field("Xtream username")
        val passInput = field("Xtream password", true)
        listOf(serverInput, userInput, passInput).forEach {
            root.addView(it, LinearLayout.LayoutParams(620, 62).apply { setMargins(0, 8, 0, 8) })
        }
        val lang = button("Language: English  /  العربية") {
            language = if (language == "en") "ar" else "en"
            itSafeToast(if (language == "ar") "تم اختيار العربية للواجهة التجريبية" else "English selected")
        }
        root.addView(lang)
        root.addView(button("CONNECT  /  اتصال") {
            server = serverInput.text.toString().trim().trimEnd('/')
            username = userInput.text.toString().trim()
            password = passInput.text.toString()
            if (server.isBlank() || username.isBlank() || password.isBlank()) {
                itSafeToast("Enter server URL, username and password.")
            } else {
                testConnection()
            }
        })
        root.addView(label("Use only an IPTV subscription you are authorized to access.", 12f, Color.LTGRAY))
        setContentView(root)
    }

    private fun itSafeToast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    private fun api(action: String): String {
        val u = URLEncoder.encode(username, "UTF-8")
        val p = URLEncoder.encode(password, "UTF-8")
        return "$server/player_api.php?username=$u&password=$p&action=$action"
    }

    private fun fetch(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            return response.body?.string() ?: throw Exception("Empty response")
        }
    }

    private fun testConnection() {
        itSafeToast("Connecting…")
        io.execute {
            try {
                val body = fetch(api(""))
                val json = JsonParser.parseString(body).asJsonObject
                val auth = json.getAsJsonObject("user_info")?.get("auth")?.asInt ?: 0
                runOnUiThread {
                    if (auth == 1) showHome()
                    else itSafeToast("Login was not accepted. Check server details.")
                }
            } catch (e: Exception) {
                runOnUiThread { itSafeToast("Connection failed. Check URL and internet.") }
            }
        }
    }

    private fun showHome() {
        baseLayout()
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("AYHAM PLAYER", 28f, Color.rgb(196,181,253)), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button(if (language == "en") "العربية" else "English") {
            language = if (language == "en") "ar" else "en"
            showHome()
        })
        root.addView(header)
        root.addView(label(if (language == "en") "A smarter screen for your entertainment." else "شاشتك الأذكى للترفيه", 16f, Color.LTGRAY))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(button(if (language == "en") "Live TV" else "القنوات") { loadStreams("get_live_streams", "live") })
        nav.addView(button(if (language == "en") "Movies" else "الأفلام") { loadStreams("get_vod_streams", "movie") })
        nav.addView(button(if (language == "en") "Series" else "المسلسلات") { loadStreams("get_series", "series") })
        nav.addView(button(if (language == "en") "Reconnect" else "إعادة الاتصال") { showLogin() })
        root.addView(nav)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroller = ScrollView(this)
        scroller.addView(content)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadStreams("get_vod_streams", "movie")
    }

    private fun loadStreams(action: String, kind: String) {
        content.removeAllViews()
        content.addView(label(if (language == "en") "Loading library…" else "جارٍ تحميل المكتبة…", 18f))
        io.execute {
            try {
                val arr = JsonParser.parseString(fetch(api(action))).asJsonArray
                val items = mutableListOf<MediaItemData>()
                for (el in arr) {
                    val obj = el.asJsonObject
                    val title = obj.get("name")?.asString ?: "Untitled"
                    val id = obj.get("stream_id")?.asString
                        ?: obj.get("series_id")?.asString ?: continue
                    val ext = if (kind == "movie") (obj.get("container_extension")?.asString ?: "mp4") else "ts"
                    val url = when (kind) {
                        "live" -> "$server/live/${URLEncoder.encode(username, "UTF-8")}/${URLEncoder.encode(password, "UTF-8")}/$id.ts"
                        "movie" -> "$server/movie/${URLEncoder.encode(username, "UTF-8")}/${URLEncoder.encode(password, "UTF-8")}/$id.$ext"
                        else -> ""
                    }
                    val image = obj.get("stream_icon")?.asString ?: obj.get("cover")?.asString
                    items.add(MediaItemData(title, url, kind, image))
                    if (items.size >= 120) break
                }
                runOnUiThread { renderItems(items, kind) }
            } catch (e: Exception) {
                runOnUiThread {
                    content.removeAllViews()
                    content.addView(label("Could not load this section. Your provider may not support this endpoint.", 18f))
                }
            }
        }
    }

    private fun renderItems(items: List<MediaItemData>, kind: String) {
        content.removeAllViews()
        if (items.isEmpty()) {
            content.addView(label("No items found.", 18f))
            return
        }
        val search = EditText(this).apply {
            hint = if (language == "en") "Search in English or Arabic…" else "ابحث بالعربية أو الإنجليزية…"
            setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); textSize = 16f
            setBackgroundColor(panel)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val q = s?.toString()?.lowercase() ?: ""
                    renderRows(items.filter { it.title.lowercase().contains(q) }, kind, false)
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        content.addView(search, LinearLayout.LayoutParams(-1, 60))
        renderRows(items, kind, true)
    }

    private fun renderRows(items: List<MediaItemData>, kind: String, clear: Boolean) {
        if (clear) {
            // keep the search field as first child
            while (content.childCount > 1) content.removeViewAt(1)
        } else {
            while (content.childCount > 1) content.removeViewAt(1)
        }
        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(10, 4, 10, 4)
                setBackgroundColor(panel)
                isFocusable = true
                isFocusableInTouchMode = true
                setOnClickListener {
                    if (item.url.isBlank()) itSafeToast("Series episode playback is planned for the next build.")
                    else startActivity(Intent(this@MainActivity, PlayerActivity::class.java).apply {
                        putExtra("title", item.title); putExtra("url", item.url)
                    })
                }
            }
            val title = label(item.title, 17f)
            row.addView(title, LinearLayout.LayoutParams(0, 60, 1f))
            row.addView(button(if (language == "en") "Play" else "تشغيل") {
                if (item.url.isNotBlank()) startActivity(Intent(this@MainActivity, PlayerActivity::class.java).apply {
                    putExtra("title", item.title); putExtra("url", item.url)
                }) else itSafeToast("Series episode playback is planned for the next build.")
            })
            content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 4, 0, 4) })
        }
    }
}
