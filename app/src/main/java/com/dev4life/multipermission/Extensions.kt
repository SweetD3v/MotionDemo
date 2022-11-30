package com.dev4life.multipermission

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.Resources
import android.util.TypedValue
import android.view.View

fun dpToPx(dp: Int): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(), Resources.getSystem().displayMetrics
    )
}

fun View.pressEffect() {
    val scaleDownX: ObjectAnimator = ObjectAnimator.ofFloat(
        this,
        "scaleX", 0.95f
    )
    val scaleDownY: ObjectAnimator = ObjectAnimator.ofFloat(
        this,
        "scaleY", 0.95f
    )

    scaleDownX.duration = 200
    scaleDownY.duration = 200
    val scaleDown = AnimatorSet()
    scaleDown.play(scaleDownX).with(scaleDownY)
    scaleDown.start()
}

fun View.unPressEffect() {
    val scaleUpX = ObjectAnimator.ofFloat(
        this, "scaleX", 1f
    )
    val scaleUpY = ObjectAnimator.ofFloat(
        this, "scaleY", 1f
    )
    scaleUpX.duration = 200
    scaleUpY.duration = 200
    val scaleUp = AnimatorSet()
    scaleUp.play(scaleUpX).with(scaleUpY)
    scaleUp.start()
}