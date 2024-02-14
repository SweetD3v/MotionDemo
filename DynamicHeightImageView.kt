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







    class CustomEventView : View {
    private val paint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
    private val cornerRadius = CommonUtils.dpToPx(8).toFloat()
    private var rect: RectF? = null
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun updateWidth(mWidth: Int) {
        this.mWidth = mWidth
        rect = RectF(0f, 0f, this.mWidth.toFloat(), mHeight.toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.e("TAG", "onDraw: ${rect?.width()}")
        rect?.let { canvas?.drawRoundRect(it, cornerRadius, cornerRadius, paint) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mWidth = width
        mHeight = height

        val left = (mWidth - 2 * cornerRadius) / 2
        val top = (mHeight - 2 * cornerRadius) / 2
        val right = mWidth - left
        val bottom = mHeight - top

        Log.e("TAG", "onSizeChanged1: $right")
        Log.e("TAG", "onSizeChanged2: $bottom")

        rect = RectF(0f, 0f, mWidth.toFloat(), mHeight.toFloat())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        Log.e("TAG", "onSizeChangedM: $width")
    }
}



    class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val timelineViews = ArrayList<TimelineViewBinding>()
    private var timelineItemViews = ArrayList<View>()
    var currentTimeView = ArrayList<View>()
    private var timeLineItems = ArrayList<EventI>()
    var currentTime = ArrayList<EventI>()
    private val eventViewMargin = dpToPx(0)
    private val timelineItemRect = ArrayList<Rect>()
    private val timelineCustomViews = ArrayList<CustomEventView>()
    private fun dpToPx(dp: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    )

    var timelineEvents: List<Event> = ArrayList()
        set(value) {
            timeLineItems.clear()
            timelineItemRect.clear()
            value.forEach {
                timelineItemRect.add(Rect())
                timeLineItems.add(EventI(it, startTime))
            }
            field = value
            addTimelineItemViews()
            requestLayout()
        }

    var startTime = DEFAULT_START_LABEL
        set(value) {
            if (value !in 0 until TOTAL) {
                throw IllegalArgumentException("Start time has to be fom 0 to 23")
            }
            field = value
            initLabels()
            invalidate()
        }

    private var mDetector: GestureDetectorCompat

    @ColorInt
    var eventBg: Int = Color.BLUE

    @ColorInt
    var eventNameColor: Int = DEFAULT_TEXT_COLOR

    @ColorInt
    var eventTimeColor: Int = DEFAULT_TEXT_COLOR

    init {
        orientation = VERTICAL
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.TimelineView,
            0, 0
        )

        startTime = 0
        setBackgroundColor(
            typedArray.getColor(
                R.styleable.TimelineView_backgroundColor,
                DEFAULT_BG_COLOR
            )
        )
        eventNameColor =
            typedArray.getColor(R.styleable.TimelineView_eventNameColor, DEFAULT_TEXT_COLOR)
        eventTimeColor =
            typedArray.getColor(R.styleable.TimelineView_eventTimeColor, DEFAULT_TEXT_COLOR)
        eventBg = typedArray.getColor(
            R.styleable.TimelineView_eventBackground,
            getColor(R.color.app_color)
        )

        typedArray.recycle()

        initLabels()

        mDetector =
            GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean {
                    return true
                }

                override fun onContextClick(e: MotionEvent?): Boolean {
                    return super.onContextClick(e)
                }

                override fun onLongPress(e: MotionEvent?) {
                    if (e != null) {
                        val array = ArrayList<Int>()
                        for (i in 0 until timelineItemRect.size) {
                            if (timelineItemRect[i].contains(e.x.toInt(), e.y.toInt())) {
                                array.add(i)
                            }
                        }
                        //val i = timelineItemRect.indexOfFirst { it.contains(e.x.toInt(), e.y.toInt()) }
                        if (array.size > 0) {
                            val i = array[array.size - 1]
                            if (timelineItemViews.size != i)
                                timelineItemViews[i].callOnClick()
                        }
                    }
                }

                override fun onShowPress(e: MotionEvent?) {

                }
            })
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        timelineItemViews.forEachIndexed { index, view ->
            val divWidth = timelineViews[0].divider.width
            if (timeLineItems.size == index) {
                return
            }
            //val left = left
            val left = (right - eventViewMargin - divWidth + eventViewMargin).toInt()
            val top = getPosFromIndex(timeLineItems[index].startIndex)
            val bottom = getPosFromIndex(timeLineItems[index].endIndex)
            val right = (right - eventViewMargin).toInt()
            //val right = right

            val widthSpec = MeasureSpec.makeMeasureSpec(right.minus(left), MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            view.layout(0, dpToPx(1).toInt(), right - left, bottom - top)
        }

        currentTimeView.forEachIndexed { index, view ->
            if (currentTime.size == index) {
                return
            }
            //val left = (right - divWidth).toInt()
            val top = getPosFromIndex(currentTime[index].startIndex)

            val widthSpec = MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, right - left, bottom - top)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        var left: Int
        var divWidth = timelineViews[0].divider.width

        timelineItemViews.forEachIndexed { index, view ->
            if (timeLineItems.size == index) {
                return
            }

            left = (right - eventViewMargin - divWidth + eventViewMargin).toInt()

            if (index > 0 && timelineEvents[index].startTime == timelineEvents[index - 1].startTime) {
                left += 50
            } else {
//                divWidth = timelineViews[0].divider.width
            }
//            left = getLeft()

            val top = getPosFromIndex(timeLineItems[index].startIndex)
            val bottom = getPosFromIndex(timeLineItems[index].endIndex)
            var right = (right - eventViewMargin).toInt()
            //val right = right
            timelineItemRect[index].set(left, top, right, bottom)
            canvas?.save()
            canvas?.translate(left.toFloat(), top.toFloat())
            view.draw(canvas)
            canvas?.restore()
        }

        timelineCustomViews.forEachIndexed { index, view ->
            if (index > 0 && timelineEvents[index].startTime == timelineEvents[index - 1].startTime) {
                view.updateWidth(view.width - 50)
                invalidate()
            }
        }

        currentTimeView.forEachIndexed { index, view ->
            if (currentTime.size == index) {
                return
            }
            left = getLeft()
            val top = getPosFromIndex(currentTime[index].startIndex)

            if (timelineItemRect.size == 0) {
                timelineItemRect.add(Rect())
            }
            timelineItemRect[index].set(left, top, right, bottom)
            canvas?.save()
            canvas?.translate(left.toFloat(), top.toFloat())
            view.draw(canvas)
            canvas?.restore()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initLabels() {
        /*timelineViews.clear()
        removeAllViewsInLayout()

        for (i in 0 until TOTAL) {
            val binding = TimelineViewBinding.inflate(LayoutInflater.from(context), this, false)
            binding.timelineLabel.text = getTime(i, startTime)
            if(binding.timelineLabel.text.equals("12 PM"))
                binding.timelineLabel.text = "Noon"
            timelineViews.add(binding)
            addView(binding.root)
        }*/
    }

    fun updateTimeView() {
        var currentTimes = ""
        if (currentTime.size != 0) {
            currentTimes = currentTime[0].name
            currentTimes =
                CommonUtils.instance.convertDateInSpecificFormat("hh:mm a", "HH:mm", currentTimes)
        }


        timelineViews.clear()
        removeAllViewsInLayout()

        for (i in 0 until TOTAL) {
            val binding = TimelineViewBinding.inflate(LayoutInflater.from(context), this, false)
            binding.timelineLabel.text = getTime(i, startTime)
            if (binding.timelineLabel.text.equals("12 PM"))
                binding.timelineLabel.text = "Noon"

            var time = if (binding.timelineLabel.text.equals("Noon"))
                CommonUtils.instance.convertDateInSpecificFormat("hh a", "HH", "12 PM")
            else
                CommonUtils.instance.convertDateInSpecificFormat(
                    "hh a",
                    "HH",
                    binding.timelineLabel.text.toString()
                )
            if (!currentTimes.equals("") && !time.equals("")) {
                var splitCurrentTime = currentTimes.split(":")
                if (splitCurrentTime.size > 0) {
                    var minutes = splitCurrentTime[1].toInt()
                    var hour = splitCurrentTime[0].toInt()
                    if (hour == time.toInt() && minutes < 10) {
                        binding.timelineLabel.visibility = View.INVISIBLE
                    } else if (minutes > 50 && hour + 1 == time.toInt()) {
                        binding.timelineLabel.visibility = View.INVISIBLE
                    } else {
                        binding.timelineLabel.visibility = View.VISIBLE
                    }
                }
            }
            timelineViews.add(binding)
            addView(binding.root)
        }
    }

    private fun getHourIndex(time: String, start: Int): Float {
        //val hm = formatter.format(Date(time * 1000)).split(" ")
        val hm = time.split(" ")
        var h = hm[0].toFloat()
        val m = hm[1].toInt()
        h = h + m / 60f - start

        if (h < 0) h += TimelineView.TOTAL
        return h
    }

    private fun addTimelineItemViews() {
        timelineItemViews.clear()

        for (i in 0 until timeLineItems.size) {
            val item = timeLineItems[i]
            val binding = ScheduleEventItemBinding.inflate(LayoutInflater.from(context))
            /*binding.event = item
            binding.cardView.setCardBackgroundColor(eventBg)
            binding.cardView1.setCardBackgroundColor(eventBg)

            binding.cardView1.visibility = View.GONE

            binding.constrained = true
            binding.eventName.setTextColor(eventNameColor)
            binding.eventTime.setTextColor(eventTimeColor)

            binding.eventName1.setTextColor(eventNameColor)
            binding.eventTime1.setTextColor(eventTimeColor)*/

            /*  val top = getPosFromIndex(timeLineItems[i].startIndex)
              val bottom = getPosFromIndex(timeLineItems[i].endIndex)
              binding.cardView.getLayoutParams().height = bottom - top*/

            binding.tvTime.text = item.description
            binding.tvTime2.text = item.description
            binding.tvProcedureName.text = item.procedureName
            binding.tvDrName.text = item.drName
            binding.tvNames.text = item.name
            binding.tvNames2.text = item.name
            val diff = item.endIndex - item.startIndex

            binding.root.alpha = 0.5f

            when (item.confirmedStatus) {
                "1" -> {
                    binding.cardView.setBackgroundResource(R.drawable.appointment_bg_confirm)
                }

                "0" -> {
                    binding.cardView.setBackgroundResource(R.drawable.appointment_bg_not_confirm)
                }

                else -> {
                    binding.cardView.setBackgroundResource(R.drawable.appointment_bg_blocked)
                }
            }

            if (diff < 1) {
                binding.tvDrName.visibility = View.GONE
                binding.tvProcedureName.visibility = View.GONE

                binding.tvTime.visibility = View.GONE
                binding.tvNames.visibility = View.GONE

                binding.tvTime2.visibility = View.VISIBLE
                binding.tvNames2.visibility = View.VISIBLE
            } else if (diff == 1f) {
                binding.tvDrName.visibility = View.VISIBLE
                binding.tvProcedureName.visibility = View.VISIBLE

                binding.tvTime.visibility = View.VISIBLE
                binding.tvNames.visibility = View.VISIBLE

                binding.tvTime2.visibility = View.GONE
                binding.tvNames2.visibility = View.GONE

                val constraintLayout = binding.cardView
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    R.id.tv_procedure_name,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    dpToPx(6).toInt()
                )
                constraintSet.connect(
                    R.id.tv_dr_name,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    dpToPx(4).toInt()
                )
                constraintSet.applyTo(constraintLayout)
            } else {
                binding.tvDrName.visibility = View.VISIBLE
                binding.tvProcedureName.visibility = View.VISIBLE

                binding.tvTime.visibility = View.VISIBLE
                binding.tvNames.visibility = View.VISIBLE

                binding.tvTime2.visibility = View.GONE
                binding.tvNames2.visibility = View.GONE

                val constraintLayout = binding.cardView
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    R.id.tv_dr_name,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    dpToPx(6).toInt()
                )
                constraintSet.connect(
                    R.id.tv_procedure_name,
                    ConstraintSet.BOTTOM,

                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    dpToPx(6).toInt()
                )
                constraintSet.applyTo(constraintLayout)
            }

            timelineItemViews.add(binding.root)
            timelineCustomViews.add(binding.customView)
            binding.executePendingBindings()
        }
    }

    private fun getTime(raw: Int, start: Int): String {
        var state = "AM"
        var time = (raw + start) % TOTAL

        if (time >= 12) state = "PM"
        if (time == 0) time = 12
        if (time > 12) time -= 12

        return "${"%2d".format(time)} $state"
    }

    fun getPosFromIndex(index: Float): Int {
        val l = floor(index).toInt()
        val h = ceil(index).toInt()

        if (l == timelineViews.size)
            return 0
        val lVal = timelineViews[l].let { it.root.top + it.divider.top }

        if (h == timelineViews.size)
            return 0
        val hVal = timelineViews[h].let { it.root.top + it.divider.top }

        return (lVal + (index - l) * (hVal - lVal)).toInt()
    }

    private fun getColor(colorAttr: Int): Int {
        val typedValue = TypedValue()
        val a: TypedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(colorAttr))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    companion object {
        const val TOTAL = 24
        const val DEFAULT_START_LABEL = 7
        const val DEFAULT_BG_COLOR = Color.BLACK
        const val DEFAULT_TEXT_COLOR = Color.WHITE
    }
}




    <?xml version="1.0" encoding="utf-8"?>
<layout>

    <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/card_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.app.calendartimelineview.timelinemodule.CustomEventView
            android:id="@+id/customView"
            android:layout_width="match_parent"
            android:layout_height="@dimen/_16dp"
            android:elevation="5dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <View
            android:id="@+id/view_bg"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:background="@drawable/appointment_bg_not_confirm"
            android:backgroundTint="@color/appointment_not_confirm_view_bg"
            android:visibility="gone"
            app:layout_constraintBottom_toBottomOf="@+id/view_padding"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintRight_toRightOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <ImageView
            android:id="@+id/iv_view"
            android:layout_width="@dimen/_5dp"
            android:layout_height="match_parent"
            android:src="@drawable/ic_left_view_appointment"
            android:visibility="invisible"
            app:layout_constraintBottom_toBottomOf="@+id/view_bg"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:tint="@color/appointment_not_confirm_view" />

        <TextView
            android:id="@+id/tv_names"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginLeft="@dimen/_13dp"
            android:layout_marginTop="@dimen/_5dp"
            android:fontFamily="@font/eina_02_semi_bold"
            android:maxWidth="@dimen/_120dp"
            android:singleLine="true"
            android:textColor="@color/text_color"
            android:textSize="@dimen/_12sp"
            app:layout_constraintLeft_toRightOf="@+id/iv_view"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/tv_names2"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginLeft="@dimen/_13dp"
            android:fontFamily="@font/eina_02_semi_bold"
            android:maxWidth="@dimen/_120dp"
            android:singleLine="true"
            android:textColor="@color/text_color"
            android:textSize="@dimen/_12sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintLeft_toRightOf="@+id/iv_view"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/tv_dr_name"
            android:layout_width="@dimen/_150dp"
            android:layout_height="@dimen/_16dp"
            android:layout_marginStart="@dimen/_13dp"
            android:fontFamily="@font/eina_02_regular"
            android:singleLine="true"
            android:textColor="@color/text_color"
            android:textSize="@dimen/_10sp"
            app:layout_constraintLeft_toRightOf="@+id/iv_view" />

        <TextView
            android:id="@+id/tv_time"
            android:layout_width="wrap_content"
            android:layout_height="@dimen/_16dp"
            android:layout_marginTop="@dimen/_5dp"
            android:layout_marginEnd="@dimen/_18dp"
            android:fontFamily="@font/eina_02_regular"
            android:textColor="@color/text_color"
            android:textSize="@dimen/_10sp"
            app:layout_constraintRight_toRightOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/tv_time2"
            android:layout_width="wrap_content"
            android:layout_height="@dimen/_16dp"
            android:layout_marginEnd="@dimen/_18dp"
            android:fontFamily="@font/eina_02_regular"
            android:textColor="@color/text_color"
            android:textSize="@dimen/_10sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintRight_toRightOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/tv_procedure_name"
            android:layout_width="@dimen/_100dp"
            android:layout_height="wrap_content"
            android:layout_marginEnd="@dimen/_18dp"
            android:ellipsize="start"
            android:fontFamily="@font/eina_02_regular"
            android:maxLines="1"
            android:text="PerEx, 3DImagehhjhjhjjgjhgjg r4rrewr grgrgregre rtretr"
            android:textAlignment="textEnd"
            android:textColor="@color/text_color"
            android:textSize="@dimen/_10sp"
            app:layout_constraintRight_toRightOf="parent" />


        <View
            android:id="@+id/view_padding"
            android:layout_width="0dp"
            android:layout_height="@dimen/_6dp"
            android:visibility="invisible"
            app:layout_constraintLeft_toLeftOf="@+id/view_bg"
            app:layout_constraintRight_toRightOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/tv_dr_name" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
