package com.agc.bwitch.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.agc.bwitch.R

class AndroidTarotSoundPlayer(
    context: Context,
) : TarotSoundPlayer {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    @Volatile
    private var isFlipSoundLoaded: Boolean = false

    private val flipSoundId = soundPool.load(context.applicationContext, R.raw.tarot_card_flip, 1)

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            isFlipSoundLoaded = status == 0 && sampleId == flipSoundId
        }
    }

    override fun playCardFlip() {
        if (!isFlipSoundLoaded) return

        soundPool.play(
            flipSoundId,
            0.65f,
            0.65f,
            1,
            0,
            1.0f,
        )
    }
}
