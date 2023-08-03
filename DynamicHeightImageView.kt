package com.demo.staggeredexample

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.imageview.ShapeableImageView


class DynamicHeightImageView : ShapeableImageView {
    private var mAspectRatio = 1f

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )


    fun setAspectRatio(aspectRatio: Float) {
        mAspectRatio = aspectRatio
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = measuredWidth
        setMeasuredDimension(measuredWidth, (measuredWidth / mAspectRatio).toInt())
    }
}






//            if (adapterPosition % 5 == 0) flowerItemBinding.image.setAspectRatio(9 / 16f)
//            if (adapterPosition % 3 == 0) flowerItemBinding.image.setAspectRatio(12 / 18f)
//            if (adapterPosition % 7 == 0) flowerItemBinding.image.setAspectRatio(1f)





<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="2dp">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="?android:attr/selectableItemBackground">

        <com.demo.staggeredexample.DynamicHeightImageView
            android:id="@+id/image"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:adjustViewBounds="true"
            android:scaleType="centerCrop"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:shapeAppearanceOverlay="@style/ShapeAppearance8" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</androidx.constraintlayout.widget.ConstraintLayout>







    fun toggleSelection(pos: Int) {
        val currentList = listDiffer.currentList
        if (selected.contains(pos) && currentList[pos].gridType == MEDIAGRIDTYPE.TYPE_IMAGE) {
            selected.remove(pos)
        } else if (currentList[pos].gridType == MEDIAGRIDTYPE.TYPE_IMAGE) {
            selected.add(pos)
        }
        notifyItemChanged(pos)
        isSelecting = selected.size > 0
    }

    fun selectRange(start: Int, end: Int, selected: Boolean) {
        val currentList = listDiffer.currentList
        for (i in start..end) {
            if (selected && currentList[i].gridType == MEDIAGRIDTYPE.TYPE_IMAGE) {
                this.selected.add(i)
            } else {
                this.selected.remove(i)
            }
        }

        notifyItemRangeChanged(start, end - start + 1)
    }

    fun deselectAll() {
        selected.clear()
        isSelecting = false
        notifyDataSetChanged()
    }

    fun selectAll() {
        isSelecting = true
        val currentList = listDiffer.currentList
        for (i in 0 until currentList.size) {
            if (currentList[i].gridType == MEDIAGRIDTYPE.TYPE_IMAGE) {
                selected.add(i)
            }
        }
        notifyDataSetChanged()
    }

    fun getCountSelected(): Int {
        return selected.size
    }

    fun getSelectionMedia(): MutableList<MediaTimeLine> {
        return listDiffer.currentList.slice(selected).toMutableList()
    }

    override fun onChange(position: Int): CharSequence {
        var realIndex = position
        if (isTitleDate(position)) {
            realIndex++
        }
        return listDiffer.currentList[realIndex].date.convertToDDMMMMYYYY()
    }
