package com.dev4life.multipermission

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener
import com.dev4life.multipermission.databinding.ActivityDrawerNewBinding
import kotlin.math.roundToInt

class HomeDrawerActivity : AppCompatActivity() {
    val binding by lazy { ActivityDrawerNewBinding.inflate(layoutInflater) }
    var isDrawerOpen = false

    val transitionListener = object : TransitionListener {
        override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {

        }

        override fun onTransitionChange(
            motionLayout: MotionLayout?,
            startId: Int,
            endId: Int,
            progress: Float
        ) {
            val progressVal = (progress * 100 / 1).roundToInt()
            Log.e("TAG", "progressVal: $progressVal")
        }

        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            if (currentId == R.id.start) {
                isDrawerOpen = false
            } else {
                isDrawerOpen
            }
        }

        override fun onTransitionTrigger(
            motionLayout: MotionLayout?,
            triggerId: Int,
            positive: Boolean,
            progress: Float
        ) {
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.run {
            motionLayout.addTransitionListener(transitionListener)

            imgBack.setOnTouchListener { v, event ->
                if (!isDrawerOpen) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            imgBack.isSelected = true
                            imgBack.pressEffect()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            imgBack.isSelected = false
                            imgBack.unPressEffect()
                            motionLayout.transitionToEnd()
                            isDrawerOpen = true
                        }
                    }
                } else {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            imgBack.isSelected = true
                            imgBack.pressEffect()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            imgBack.isSelected = false
                            imgBack.unPressEffect()
                            motionLayout.transitionToStart()
                            isDrawerOpen = false
                        }
                    }
                }
                return@setOnTouchListener true
            }

            imgRecents.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        imgRecents.isSelected = true
                        imgRecents.pressEffect()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        imgRecents.isSelected = false
                        imgRecents.unPressEffect()
                    }
                }
                return@setOnTouchListener true
            }

            imgAllPdf.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        imgAllPdf.isSelected = true
                        imgAllPdf.pressEffect()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        imgAllPdf.isSelected = false
                        imgAllPdf.unPressEffect()
                    }
                }
                return@setOnTouchListener true
            }

            imgTools.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        imgTools.isSelected = true
                        imgTools.pressEffect()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        imgTools.isSelected = false
                        imgTools.unPressEffect()
                    }
                }
                return@setOnTouchListener true
            }

            imgFavourite.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        imgFavourite.isSelected = true
                        imgFavourite.pressEffect()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        imgFavourite.isSelected = false
                        imgFavourite.unPressEffect()
                    }
                }
                return@setOnTouchListener true
            }
        }
    }

    override fun onBackPressed() {
        if (binding.edtSearch.hasFocus()) {
            binding.edtSearch.clearFocus()
            return
        }
        finish()
    }
}