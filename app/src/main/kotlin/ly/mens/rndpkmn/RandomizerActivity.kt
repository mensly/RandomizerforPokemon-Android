package ly.mens.rndpkmn

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.dabomstew.pkrandom.romhandlers.RomHandler
import org.jetbrains.anko.AnkoComponent
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.setContentView
import org.jetbrains.anko.verticalLayout
import java.io.File


class RandomizerActivity: AppCompatActivity() {
    companion object {
        const val FILE_PATH = "FILE_PATH"
        const val HANDLER_FACTORY = "HANDLER_FACTORY"
    }
    lateinit private var saveDir: File
    lateinit private var romHandler: RomHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val romPath = intent.getStringExtra(FILE_PATH) ?: throw IllegalStateException("rom path not provided")
        saveDir = File(romPath).parentFile
        val handlerFactory = Class.forName(intent.getStringExtra(HANDLER_FACTORY)).newInstance()
            as? RomHandler.Factory ?: throw IllegalStateException("handler factory not provided")
        romHandler = handlerFactory.create(random).apply {
            // TODO: Make async
            loadRom(romPath)
        }
        RandomizerActivityUI().setContentView(this)
    }
}

class RandomizerActivityUI: AnkoComponent<RandomizerActivity> {
    override fun createView(ui: AnkoContext<RandomizerActivity>) = ui.apply {
        verticalLayout {

        }
    }.view
}