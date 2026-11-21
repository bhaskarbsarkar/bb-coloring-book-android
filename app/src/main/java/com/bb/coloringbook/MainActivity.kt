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
    private lateinit var backupBtn: Button
    private val toolButtons = mutableMapOf<Tool, Button>()
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("bb", MODE_PRIVATE) }
    private val recent = mutableListOf<Int>()
    private val d get() = resources.displayMetrics.density
    private val store by lazy { ProjectStore(filesDir) }
    private var currentId: String? = null
    private val autoSave = Runnable { saveCurrent() }

    private val palette = longArrayOf(
        0xFF000000, 0xFF424242, 0xFF9E9E9E, 0xFFFFFFFF,
        0xFFB71C1C, 0xFFE53935, 0xFFFF8A80, 0xFFF8BBD0, 0xFFEC407A, 0xFFAD1457,
        0xFF6A1B9A, 0xFFAB47BC, 0xFFCE93D8, 0xFF4527A0, 0xFF7E57C2,
        0xFF1A237E, 0xFF1E88E5, 0xFF90CAF9, 0xFF00ACC1, 0xFF80DEEA, 0xFF00897B,
        0xFF1B5E20, 0xFF43A047, 0xFFA5D6A7, 0xFFC0CA33, 0xFFE6EE9C,
        0xFFFDD835, 0xFFFFF59D, 0xFFFFB300, 0xFFFB8C00, 0xFFFFCC80, 0xFFF4511E,
        0xFF4E342E, 0xFF8D6E63, 0xFFD7B19D, 0xFFFFE0BD, 0xFFF1C27D, 0xFFC68642,
    ).map { it.toInt() }.toIntArray()

    companion object { private const val REQ_OPEN = 1; private const val REQ_RESTORE = 2; private const val MAX_DIM = 2048 }

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
        restoreLastProject(openAfter = incoming)
        if (!prefs.getBoolean("warnedUninstall", false)) main.postDelayed({ showUninstallWarning() }, 1200)
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
        top.addView(button("📚 My Drawings") { showDrawings() })
        backupBtn = button("⚠ Back up") { showBackupDialog() }.also { top.addView(it) }
        top.addView(button("📂 New from picture") { openPicker() })
        top.addView(button("🌸 New mandala") { startProject(SampleArt.mandala(), "Mandala") })
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
        top.addView(button("💾 Save") { saveCurrent(); toast("Progress saved") })
        top.addView(button("🖼 Export") { saveToGallery() })
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
    override fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean) {
        if (::undoBtn.isInitialized) refreshUi()
        if (canUndo) { main.removeCallbacks(autoSave); main.postDelayed(autoSave, 3000) }
    }

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
        if (requestCode == REQ_RESTORE && resultCode == RESULT_OK) data?.data?.let { restoreBackup(it) }
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
                else startProject(bmp, displayName(uri))
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

    // =====================================================================================
    // Saved drawings (projects)
    // =====================================================================================

    private fun displayName(uri: Uri): String = runCatching {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull()?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Drawing"

    /** Saves the colors of the open drawing in the background. */
    private fun saveCurrent() {
        main.removeCallbacks(autoSave)
        val id = currentId ?: return
        val paint = canvasView.paintLayer?.copy(Bitmap.Config.ARGB_8888, false) ?: return
        val preview = canvasView.renderResult() ?: return
        io.execute { runCatching { store.saveProgress(id, paint, preview) }; updateBackupState() }
    }

    /** Saves the open drawing, then starts a new one from [art]. */
    private fun startProject(art: Bitmap, name: String) {
        saveCurrent()
        io.execute {
            val id = runCatching { store.create(art, name) }.getOrNull()
            main.post {
                if (id == null) { toast("Could not create drawing"); return@post }
                currentId = id; prefs.edit().putString("current", id).apply()
                canvasView.setImage(art)
                saveCurrent()
            }
        }
    }

    private fun openProject(id: String) {
        if (id == currentId) return
        saveCurrent()
        io.execute {
            val data = runCatching { store.load(id) }.getOrNull()
            main.post {
                if (data == null) { toast("Could not open drawing"); return@post }
                currentId = id; prefs.edit().putString("current", id).apply()
                canvasView.setImage(data.first, data.second)
            }
        }
    }

    private fun restoreLastProject(openAfter: Uri?) {
        io.execute {
            // One-time migration from the single autosave of version 1.0.
            val oldArt = File(filesDir, "current_art.png"); val oldPaint = File(filesDir, "current_paint.png")
            if (oldArt.exists()) runCatching {
                val art = BitmapFactory.decodeFile(oldArt.path)
                val paint = if (oldPaint.exists()) BitmapFactory.decodeFile(oldPaint.path) else null
                val id = store.create(art, "My first drawing")
                if (paint != null) store.saveProgress(id, paint, paint)
                prefs.edit().putString("current", id).apply()
                oldArt.delete(); oldPaint.delete()
            }
            val wanted = prefs.getString("current", null)
            val id = if (store.exists(wanted)) wanted else store.list().firstOrNull()?.id
            main.post {
                when {
                    openAfter != null -> loadUri(openAfter)
                    id != null -> openProject(id)
                    else -> startProject(SampleArt.mandala(), "Mandala")
                }
            }
        }
    }

    /** For tests. */
    internal val projectStore get() = store
    internal val openProjectId get() = currentId
    internal fun newProjectForTest(art: Bitmap, name: String) = startProject(art, name)
    internal fun openProjectForTest(id: String) = openProject(id)

    private fun showDrawings() {
        saveCurrent()
        // Wait for the pending save so the current thumbnail is fresh.
        io.execute { val list = store.list(); main.post { showDrawingsDialog(list) } }
    }

    private fun showDrawingsDialog(projects: List<ProjectStore.Project>) {
        var dialog: AlertDialog? = null
        val fmt = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        val grid = GridView(this).apply {
            numColumns = GridView.AUTO_FIT; columnWidth = dp(180)
            horizontalSpacing = dp(12); verticalSpacing = dp(12); stretchMode = GridView.STRETCH_COLUMN_WIDTH
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        grid.adapter = object : BaseAdapter() {
            override fun getCount() = projects.size
            override fun getItem(i: Int) = projects[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, convert: View?, parent: android.view.ViewGroup): View {
                val p = projects[i]
                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8))
                    background = GradientDrawable().apply {
                        cornerRadius = 12 * d; setColor(Color.WHITE)
                        if (p.id == currentId) setStroke(dp(3), 0xFF6750A4.toInt()) else setStroke(dp(1), 0x22000000)
                    }
                }
                val img = ImageView(this@MainActivity).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    if (p.thumb.exists()) setImageBitmap(BitmapFactory.decodeFile(p.thumb.path))
                }
                card.addView(img, LinearLayout.LayoutParams(-1, dp(150)))
                card.addView(TextView(this@MainActivity).apply {
                    text = if (p.id == currentId) "${p.name}  (open)" else p.name
                    textSize = 15f; setTextColor(Color.BLACK); maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                card.addView(TextView(this@MainActivity).apply { text = fmt.format(java.util.Date(p.updated)); textSize = 12f })
                return card
            }
        }
        grid.setOnItemClickListener { _, _, i, _ -> dialog?.dismiss(); openProject(projects[i].id) }
        grid.setOnItemLongClickListener { _, _, i, _ -> dialog?.dismiss(); projectOptions(projects[i]); true }

        val content: View = if (projects.isEmpty()) TextView(this).apply {
            text = "No saved drawings yet."; setPadding(dp(24), dp(24), dp(24), dp(24))
        } else grid
        dialog = AlertDialog.Builder(this)
            .setTitle("My Drawings  ·  tap to open, long-press to rename/delete")
            .setView(content)
            .setPositiveButton("New from picture") { _, _ -> openPicker() }
            .setNeutralButton("New mandala") { _, _ -> startProject(SampleArt.mandala(), "Mandala") }
            .setNegativeButton("Close", null)
            .show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), (resources.displayMetrics.heightPixels * 0.85).toInt())
    }

    private fun projectOptions(p: ProjectStore.Project) {
        AlertDialog.Builder(this).setTitle(p.name)
            .setItems(arrayOf("Open", "Rename", "Delete")) { _, which ->
                when (which) {
                    0 -> openProject(p.id)
                    1 -> {
                        val input = EditText(this).apply { setText(p.name); setSingleLine(); selectAll() }
                        AlertDialog.Builder(this).setTitle("Rename").setView(input)
                            .setPositiveButton("OK") { _, _ ->
                                val n = input.text.toString().trim()
                                if (n.isNotEmpty()) io.execute { store.rename(p.id, n); main.post { showDrawings() } }
                            }.setNegativeButton("Cancel", null).show()
                    }
                    2 -> confirm("Delete \"${p.name}\"? This cannot be undone.") {
                        val wasOpen = p.id == currentId
                        if (wasOpen) { currentId = null; main.removeCallbacks(autoSave) }
                        io.execute {
                            store.delete(p.id)
                            val next = store.list().firstOrNull()?.id
                            main.post {
                                if (wasOpen) { if (next != null) openProject(next) else startProject(SampleArt.mandala(), "Mandala") }
                                showDrawings()
                            }
                        }
                    }
                }
            }.show()
    }

    override fun onPause() { super.onPause(); saveCurrent() }

    override fun onResume() { super.onResume(); io.execute { updateBackupState() } }

    // =====================================================================================
    // Backup guardrail (drawings live inside the app and are deleted on uninstall)
    // =====================================================================================

    /** Call on the io thread. Highlights the backup button when there are unbacked changes. */
    private fun updateBackupState() {
        val pending = store.lastChange() > prefs.getLong("lastBackup", 0L)
        main.post {
            if (!::backupBtn.isInitialized) return@post
            backupBtn.text = if (pending) "⚠ Back up" else "✅ Backed up"
            backupBtn.setTextColor(if (pending) 0xFFB3261E.toInt() else 0xFF1B5E20.toInt())
            backupBtn.background = roundRect(if (pending) 0xFFFFDAD6.toInt() else Color.TRANSPARENT)
        }
    }

    internal fun showUninstallWarning() {
        prefs.edit().putBoolean("warnedUninstall", true).apply()
        AlertDialog.Builder(this)
            .setTitle("⚠ Keep your drawings safe")
            .setMessage(
                "Your drawings are stored inside this app. Uninstalling the app (or clearing its data) " +
                "deletes them permanently.\n\n" +
                "Android doesn't let apps show their own warning at uninstall time, so:\n" +
                "• Use the ⚠ Back up button to save a backup file to your Downloads folder. " +
                "It survives uninstalling and can be restored later.\n" +
                "• If Android asks \"Keep app data?\" when uninstalling, tick it.\n\n" +
                "The button turns red whenever you have changes that aren't backed up."
            )
            .setPositiveButton("Back up now") { _, _ -> backupZip() }
            .setNegativeButton("Got it", null)
            .show()
    }

    private fun showBackupDialog() {
        var dialog: AlertDialog? = null
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), dp(8)) }
        val last = prefs.getLong("lastBackup", 0L)
        box.addView(TextView(this).apply {
            textSize = 15f
            text = "⚠ Uninstalling this app deletes all drawings stored in it. Keep a backup outside the app.\n\n" +
                "Last backup: " + (if (last == 0L) "never" else java.text.DateFormat.getDateTimeInstance().format(java.util.Date(last)))
        })
        fun action(title: String, sub: String, run: () -> Unit) {
            box.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12))
                background = roundRect(0xFFF3EDF7.toInt())
                addView(TextView(context).apply { text = title; textSize = 17f; setTextColor(Color.BLACK) })
                addView(TextView(context).apply { text = sub; textSize = 13f })
                setOnClickListener { dialog?.dismiss(); run() }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        }
        action("💾 Save backup file (recommended)",
            "One .zip in Download/BB Coloring Book with every drawing. Restore it later to keep coloring.") { backupZip() }
        action("🖼 Export all as PNG pictures",
            "Finished pictures go to Pictures/BB Coloring Book. Good for sharing, but they can't be resumed.") { exportAllPng() }
        action("♻ Restore from backup file",
            "Adds the drawings from a backup .zip. Drawings already here are kept.") { pickBackupFile() }
        dialog = AlertDialog.Builder(this).setTitle("Back up your drawings")
            .setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton("Close", null).show()
    }

    private fun backupZip() {
        saveCurrent()
        toast("Creating backup…")
        io.execute { // runs after the pending save
            val stamp = java.text.SimpleDateFormat("yyyy-MM-dd_HHmm", java.util.Locale.US).format(java.util.Date())
            val result = runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "BBColoringBook-backup-$stamp.zip")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/BB Coloring Book")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
                val n = contentResolver.openOutputStream(uri)!!.use { store.exportZip(it) }
                values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
                n
            }
            if (result.isSuccess) prefs.edit().putLong("lastBackup", System.currentTimeMillis()).apply()
            updateBackupState()
            main.post {
                if (result.isSuccess) toast("Backed up ${result.getOrNull()} drawing(s) to Download/BB Coloring Book")
                else toast("Backup failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun exportAllPng() {
        saveCurrent()
        toast("Exporting pictures…")
        io.execute {
            var ok = 0
            val projects = store.list()
            for (p in projects) runCatching {
                val bmp = store.render(p.id)!!
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "${p.name.replace(Regex("[^A-Za-z0-9 _-]"), "_")}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BB Coloring Book")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
                ok++
            }
            main.post { toast("Exported $ok of ${projects.size} picture(s) to Pictures/BB Coloring Book") }
        }
    }

    @Suppress("DEPRECATION")
    private fun pickBackupFile() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        }
        startActivityForResult(Intent.createChooser(i, "Choose a BB Coloring Book backup (.zip)"), REQ_RESTORE)
    }

    internal fun restoreBackup(uri: Uri) {
        saveCurrent()
        io.execute {
            val r = runCatching { contentResolver.openInputStream(uri)!!.use { store.importZip(it) } }
            main.post {
                val n = r.getOrNull()
                when {
                    n == null -> toast("That file isn't a valid backup")
                    n == 0 -> toast("Nothing new to restore (drawings already here)")
                    else -> { toast("Restored $n drawing(s)"); showDrawings() }
                }
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); io.shutdown() }
}

