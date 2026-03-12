package com.agc.bwitch.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AndroidTarotHaptics(
    context: Context,
) : TarotHaptics {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun performRevealHaptic() {
        val currentVibrator = vibrator ?: return
        if (!currentVibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            currentVibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentVibrator.vibrate(
                VibrationEffect.createOneShot(16L, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            currentVibrator.vibrate(16L)
        }
    }
}
