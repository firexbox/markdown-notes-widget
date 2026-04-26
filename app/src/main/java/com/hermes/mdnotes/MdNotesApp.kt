package com.hermes.mdnotes

import android.app.Application
import com.hermes.mdnotes.data.NotesRepository

class MdNotesApp : Application() {

    lateinit var notesRepository: NotesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        notesRepository = NotesRepository.getInstance(this)
    }

    companion object {
        lateinit var instance: MdNotesApp
            private set
    }
}
