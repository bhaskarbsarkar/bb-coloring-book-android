package com.bb.coloringbook

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity(), ColoringView.Listener {

    private lateinit var canvasView: ColoringView
    private lateinit var colorSwatch: View
    private lateinit var paletteRow: LinearLayout
    private lateinit var undoBtn: Button
    private lateinit var redoBtn: Button
    private lateinit var penBtn: Button
    private lateinit var insideBtn: Button
    private val toolButtons = mutableMapOf<Tool, Button>()
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("bb", MODE_PRIVATE) }
    private val recent = mutableListOf<Int>()
    private val d get() = resources.displayMetrics.density

    private val palette = longArrayOf(
        0xFF000000, 0xFF424242, 0xFF9E9E9E, 0xFFFFFFFF,
        0xFFB71C1C, 0xFFE53935, 0xFFFF8A80, 0xFFF8BBD0, 0xFFEC407A, 0xFFAD1457,
        0xFF6A1B9A, 0xFFAB47BC, 0xFFCE93D8, 0xFF4527A0, 0xFF7E57C2,
        0xFF1A237E, 0xFF1E88E5, 0xFF90CAF9, 0xFF00ACC1, 0xFF80DEEA, 0xFF00897B,
        0xFF1B5E20, 0xFF43A047, 0xFFA5D6A7, 0xFFC0CA33, 0xFFE6EE9C,
        0xFFFDD835, 0xFFFFF59D, 0xFFFFB300, 0xFFFB8C00, 0xFFFFCC80, 0xFFF4511E,
        0xFF4E342E, 0xFF8D6E63, 0xFFD7B19D, 0xFFFFE0BD, 0xFFF1C27D, 0xFFC68642,
    ).map { it.toInt() }.toIntArray()

    companion object { private const val REQ_OPEN = 1; private const val MAX_DIM = 2048 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recent.addAll(prefs.getString("recent", "")!!.split(',').filter { it.isNotBlank() }.map { it.toInt() })
        buildUi()
        canvasView.penOnly = prefs.getBoolean("penOnly", false)
        canvasView.stayInside = prefs.getBoolean("stayInside", true)
        canvasView.brushSize = prefs.getFloat("size", 24f)
        canvasView.color = prefs.getInt("color", canvasView.color)
        refreshUi()

        val incoming = incomingUri(intent)
        if (incoming != null) loadUri(incoming) else restoreAutosave()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingUri(intent)?.let { loadUri(it) }
    }

    @Suppress("DEPRECATION")
    private fun incomingUri(i: Intent?): Uri? = when (i?.action) {
        Intent.ACTION_SEND -> i.getParcelableExtra(Intent.EXTRA_STREAM)
        Intent.ACTION_VIEW -> i.data
        else -> null
    }

    // =====================================================================================
    // UI
    // =====================================================================================

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFF3EDF7.toInt()) }
        root.fitsSystemWindows = true

        // ---- top toolbar ----
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(button("📂 Open") { openPicker() })
        top.addView(button("🌸 Sample") { confirm("Load the sample mandala? Current coloring is replaced.") { canvasView.setImage(SampleArt.mandala()) } })
        top.addView(divider())
        undoBtn = button("↶ Undo") { canvasView.undo() }.also { top.addView(it) }
        redoBtn = button("↷ Redo") { canvasView.redo() }.also { top.addView(it) }
        top.addView(divider())
        listOf(Tool.BRUSH to "🖌 Brush", Tool.FILL to "🪣 Fill", Tool.ERASER to "🧽 Eraser", Tool.PICKER to "💧 Pick").forEach { (t, label) ->
            toolButtons[t] = button(label) { canvasView.tool = t; refreshUi() }.also { top.addView(it) }
        }
        top.addView(divider())
        insideBtn = button("") {
            canvasView.stayInside = !canvasView.stayInside
            prefs.edit().putBoolean("stayInside", canvasView.stayInside).apply(); refreshUi()
        }.also { top.addView(it) }
        penBtn = button("") {
            canvasView.penOnly = !canvasView.penOnly
            prefs.edit().putBoolean("penOnly", canvasView.penOnly).apply(); refreshUi()
        }.also { top.addView(it) }
        top.addView(divider())
        top.addView(button("⤢ Fit") { canvasView.fitToView() })
        top.addView(button("🗑 Clear") { confirm("Remove all colors?") { canvasView.clearColors() } })
        top.addView(button("💾 Save") { saveToGallery() })
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(top) },
            LinearLayout.LayoutParams(-1, -2))

        // ---- canvas ----
        canvasView = ColoringView(this).also { it.listener = this }
        root.addView(canvasView, LinearLayout.LayoutParams(-1, 0, 1f))

        // ---- bottom: color + size + palette ----
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        colorSwatch = View(this).apply { setOnClickListener { showColorDialog() }; contentDescription = "Current color" }
        bottom.addView(colorSwatch, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginEnd = dp(8) })

        val sizeLabel = TextView(this).apply { textSize = 14f; minWidth = dp(64) }
        val seek = SeekBar(this).apply {
            max = 119
            progress = (canvasView.brushSize - 1).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, v: Int, fromUser: Boolean) {
                    canvasView.brushSize = v + 1f; sizeLabel.text = "Size ${v + 1}"
                    if (fromUser) prefs.edit().putFloat("size", v + 1f).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        }
        sizeLabel.text = "Size ${canvasView.brushSize.toInt()}"
        bottom.addView(sizeLabel)
        bottom.addView(seek, LinearLayout.LayoutParams(dp(200), -2))

        paletteRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        bottom.addView(HorizontalScrollView(this).apply { addView(paletteRow) }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(bottom, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
        main.post { seek.progress = (canvasView.brushSize - 1).toInt() }
    }

    private fun dp(v: Int) = (v * d).toInt()

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 15f
        minWidth = 0; minimumWidth = 0
        setPadding(dp(12), 0, dp(12), 0)
        background = roundRect(Color.TRANSPARENT)
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(-2, dp(48)).apply { setMargins(dp(2), dp(4), dp(2), dp(4)) }
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(0x22000000)
        layoutParams = LinearLayout.LayoutParams(dp(1), dp(28)).apply { setMargins(dp(4), 0, dp(4), 0) }
    }

    private fun roundRect(fill: Int) = GradientDrawable().apply { cornerRadius = 12 * d; setColor(fill) }

    private fun swatchDrawable(c: Int, selected: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(c)
        setStroke(if (selected) dp(3) else dp(1), if (selected) 0xFF6750A4.toInt() else 0x33000000)
    }

    private fun refreshUi() {
        val sel = 0xFFD8C8F0.toInt()
        toolButtons.forEach { (t, b) -> b.background = roundRect(if (t == canvasView.tool) sel else Color.TRANSPARENT) }
        insideBtn.text = if (canvasView.stayInside) "🎯 Inside lines: ON" else "🎯 Inside lines: OFF"
        insideBtn.background = roundRect(if (canvasView.stayInside) sel else Color.TRANSPARENT)
        penBtn.text = if (canvasView.penOnly) "✍ Pen only: ON" else "✍ Pen only: OFF"
        penBtn.background = roundRect(if (canvasView.penOnly) sel else Color.TRANSPARENT)
        undoBtn.isEnabled = canvasView.canUndo(); undoBtn.alpha = if (undoBtn.isEnabled) 1f else 0.4f
        redoBtn.isEnabled = canvasView.canRedo(); redoBtn.alpha = if (redoBtn.isEnabled) 1f else 0.4f
        colorSwatch.background = GradientDrawable().apply {
            cornerRadius = 14 * d; setColor(canvasView.color); setStroke(dp(2), 0x55000000)
        }
        buildPalette()
    }

    private fun buildPalette() {
        paletteRow.removeAllViews()
        val add = TextView(this).apply {
            text = "+"; textSize = 22f; gravity = Gravity.CENTER
            background = swatchDrawable(Color.WHITE, false)
            setOnClickListener { showColorDialog() }
        }
        paletteRow.addView(add, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(6) })
        val colors = recent + palette.filter { it !in recent }
        colors.forEachIndexed { i, c ->
            val v = View(this).apply {
                background = swatchDrawable(c, c == canvasView.color)
                setOnClickListener { setColor(c, remember = false) }
            }
            val lp = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(6) }
            if (i == recent.size - 1) lp.marginEnd = dp(18) // gap between recent and palette
            paletteRow.addView(v, lp)
        }
    }

    private fun setColor(c: Int, remember: Boolean) {
        canvasView.color = c
        if (remember && c !in palette) {
            recent.remove(c); recent.add(0, c)
            while (recent.size > 12) recent.removeAt(recent.size - 1)
            prefs.edit().putString("recent", recent.joinToString(",")).apply()
        }
        prefs.edit().putInt("color", c).apply()
        if (canvasView.tool == Tool.ERASER || canvasView.tool == Tool.PICKER) canvasView.tool = Tool.BRUSH
        refreshUi()
    }

    private fun showColorDialog() {
        val picker = ColorPickerView(this, canvasView.color)
        val preview = View(this)
        val hex = EditText(this).apply {
            setSingleLine(); textSize = 16f; hint = "#RRGGBB"
            setText(String.format("#%06X", canvasView.color and 0xFFFFFF))
        }
        fun updatePreview(c: Int) { preview.background = roundRect(c) }
        updatePreview(picker.color)
        picker.onChange = { c -> updatePreview(c); hex.setText(String.format("#%06X", c and 0xFFFFFF)) }

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(preview, LinearLayout.LayoutParams(dp(64), dp(40)).apply { marginEnd = dp(12) })
        row.addView(hex, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "Apply hex"; isAllCaps = false
            setOnClickListener {
                runCatching { Color.parseColor(hex.text.toString().trim()) }.getOrNull()?.let { picker.setColor(it); updatePreview(it) }
            }
        })
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), 0)
            addView(picker, LinearLayout.LayoutParams(-1, -2))
            addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        }
        AlertDialog.Builder(this)
            .setTitle("Pick any color")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Use color") { _, _ -> setColor(picker.color, remember = true) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirm(msg: String, action: () -> Unit) {
        AlertDialog.Builder(this).setMessage(msg)
            .setPositiveButton("OK") { _, _ -> action() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    // ---- ColoringView.Listener ----
    override fun onColorPicked(color: Int) { setColor(color, remember = true); canvasView.tool = Tool.BRUSH; refreshUi() }
    override fun onStylusDetected() {
        prefs.edit().putBoolean("penOnly", true).apply()
        main.post { refreshUi(); toast("Stylus detected: pen draws, fingers move & zoom") }
    }
    override fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean) { if (::undoBtn.isInitialized) refreshUi() }

    // =====================================================================================
    // Import / export
    // =====================================================================================

    @Suppress("DEPRECATION")
    private fun openPicker() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(Intent.createChooser(i, "Choose a picture to color"), REQ_OPEN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OPEN && resultCode == RESULT_OK) data?.data?.let { loadUri(it) }
    }

    private fun loadUri(uri: Uri) {
        toast("Loading picture…")
        io.execute {
            val bmp = runCatching {
                val src = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { dec, info, _ ->
                    dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width; val h = info.size.height
                    val s = minOf(1f, MAX_DIM.toFloat() / maxOf(w, h))
                    if (s < 1f) dec.setTargetSize((w * s).toInt().coerceAtLeast(1), (h * s).toInt().coerceAtLeast(1))
                }
            }.getOrNull()
            main.post {
                if (bmp == null) toast("Could not open that picture")
                else canvasView.setImage(bmp)
            }
        }
    }

    private fun saveToGallery() {
        val out = canvasView.renderResult() ?: return
        io.execute {
            val ok = runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "coloring_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BB Coloring Book")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                contentResolver.openOutputStream(uri)!!.use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }.isSuccess
            main.post { toast(if (ok) "Saved to Pictures/BB Coloring Book" else "Save failed") }
        }
    }

    // ---- autosave of work in progress ----
    private val artFile get() = File(filesDir, "current_art.png")
    private val paintFile get() = File(filesDir, "current_paint.png")

    override fun onPause() {
        super.onPause()
        val art = canvasView.lineArt ?: return
        val artCopy = art.copy(Bitmap.Config.ARGB_8888, false)
        val paintCopy = canvasView.paintLayer!!.copy(Bitmap.Config.ARGB_8888, false)
        io.execute {
            runCatching {
                writePng(artCopy, artFile); writePng(paintCopy, paintFile)
            }
        }
    }

    private fun writePng(b: Bitmap, f: File) {
        val tmp = File(f.path + ".tmp")
        tmp.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tmp.renameTo(f)
    }

    private fun restoreAutosave() {
        io.execute {
            val art = if (artFile.exists()) BitmapFactory.decodeFile(artFile.path) else null
            val paint = if (art != null && paintFile.exists()) BitmapFactory.decodeFile(paintFile.path) else null
            main.post {
                if (art != null) canvasView.setImage(art, paint) else canvasView.setImage(SampleArt.mandala())
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); io.shutdown() }
}

