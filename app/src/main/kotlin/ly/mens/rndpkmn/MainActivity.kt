package ly.mens.rndpkmn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.dabomstew.pkrandom.Utils.testForRequiredConfigs
import com.dabomstew.pkrandom.romhandlers.*
import ly.mens.rndpkmn.R.string.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.File

private val defaultDir by lazy {
    // TODO: Default to /sdcard if ROMs directory not present
    File(Environment.getExternalStorageDirectory(), "ROMs").canonicalPath
    // TODO: Allow user to choose a directory more easily
}

private val romHandlerFactories = listOf(
        Gen1RomHandler.Factory(),
        Gen2RomHandler.Factory(),
        Gen3RomHandler.Factory(),
        Gen4RomHandler.Factory(),
        Gen5RomHandler.Factory())

class MainActivity : AppCompatActivity() {
    private var saveDir: File? = null
    private var romHandler: RomHandler? = null
    private val ui by lazy { MainActivityUI() }
    var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.setProperty("pkrandom.root", filesDir.canonicalPath)
        if (BuildConfig.DEBUG) {
            testForRequiredConfigs()
        }
        ui.setContentView(this)
        checkPermission()
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            // TODO: UX flow
        }
    }

    fun listRoms(directory: File): List<File>? {
        if (!directory.exists() || !directory.isDirectory) {
            toast(error_invalid_dir)
            return null
        }
        val files = directory.listFiles(File::isRomFile)
        if (files.isEmpty()) {
            longToast(error_no_roms, directory)
        }
        return files.toList()
    }

    fun loadRom(romPath: String) {
        val handler = romHandlerFactories.firstOrNull { it.isLoadable(romPath) }?.create(random)
        if (handler == null) {
            longToast(error_invalid_rom, romPath)
            return
        }
        loading = true
        doAsync {
            handler.loadRom(romPath)
            runOnUiThread {
                saveDir = File(romPath).parentFile
                romHandler = handler
                loading = false
                toast(rom_loaded, handler.romName)
            }
        }
    }

    fun saveRom() {
        // TODO: Move this to separate activity with randomization options
        val saveDir = this.saveDir
        if (saveDir == null) {
            toast(error_not_loaded)
            return
        }
        doAsync {
            romHandler?.apply {
                randomizeFieldItems(true)
                if (canChangeStaticPokemon()) {
                    randomizeStaticPokemon(true)
                }
                if (canChangeStarters()) {
                    starters = List(3) { random2EvosPokemon() }
                }
                area1to1Encounters(false, true, false, false, false)
                // TODO: Allow choosing file name and output directory
                val output = File(saveDir, "$romName Random.$defaultExtension").canonicalPath
                saveRom(output)
                runOnUiThread {
                    toast(rom_saved, output)
                }
            }
        }
    }
}

class MainActivityUI: AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = ui.apply {
        verticalLayout {
            val romsDir = editText(defaultDir)
            button(action_open_rom) {
                onClick {
                    if (owner.loading) { return@onClick }
                    owner.listRoms(File(romsDir.text.toString()))?.apply {
                        selector(ctx.getString(action_open_rom), map(File::nameWithoutExtension)) { _, index ->
                            owner.loadRom(get(index).absolutePath)
                        }
                    }
                }
            }
            button(action_save_rom) {
                onClick {
                    owner.saveRom()
                }
            }
        }
    }.view
}
