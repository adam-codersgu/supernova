package com.codersguidebook.supernova

import android.animation.TimeAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_COLOUR
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_QUANTITY
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_SPEED
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_SPIN
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_TYPE
import java.util.*
import kotlin.math.roundToInt

class PlaybackAnimator(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        private const val SEED = 1337L
        private const val SCALE_MIN_PART = 0.45f
        private const val SCALE_RANDOM_PART = 0.55f
        private const val ALPHA_SCALE_PART = 0.9f
        private const val ALPHA_RANDOM_PART = 0.5f
    }

    class MovingObject {
        var selected = false
        var x = 0f
        var y = 0f
        var colour = 0
        var scale = 0f
        var alpha = 0f
        var speed = 0f
        var spin = 0
        lateinit var drawable: Drawable
    }

    private val redColours = listOf(R.color.red1, R.color.red2, R.color.red3,
        R.color.red4, R.color.red5, R.color.red6, R.color.red7)
    private val blueColours = listOf(
        R.color.blue1, R.color.blue2, R.color.blue3,
        R.color.blue4, R.color.blue5, R.color.blue6, R.color.blue7)
    private val nightColours = listOf(
        R.color.night1, R.color.night2, R.color.night3,
        R.color.night4, R.color.night5, R.color.night6, R.color.night7)
    private val pastelColours = listOf(R.color.nav_home, R.color.nav_playing, R.color.nav_playlists,
        R.color.nav_artists, R.color.nav_albums, R.color.nav_songs, R.color.nav_settings)
    private val leavesDrawables = listOf(R.drawable.leaf)
    private val instrumentsDrawables = listOf(R.drawable.drums, R.drawable.piano, R.drawable.saxophone, R.drawable.saxophone)
    private val spaceDrawables = listOf(R.drawable.star, R.drawable.star, R.drawable.earth, R.drawable.planet, R.drawable.saturn)
    private val mandalaDrawables = listOf(R.drawable.mandala1, R.drawable.mandala2, R.drawable.mandala3, R.drawable.mandala4)
    private val animalDrawables = listOf(R.drawable.cat, R.drawable.dolphin, R.drawable.elephant, R.drawable.peacock, R.drawable.wolf)
    private val flowerDrawables = listOf(R.drawable.flower, R.drawable.poppy, R.drawable.rose1, R.drawable.rose2)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val count = sharedPreferences.getInt(ANIMATION_QUANTITY, 6)
    var objectList = arrayOfNulls<MovingObject>(count)
    private val mRnd: Random = Random(SEED)
    private var mTimeAnimator: TimeAnimator? = null
    var usingCustomDrawable = false
    // default drawable
    var drawableList = drawableListGenerator(leavesDrawables)
    var spinSpeed = sharedPreferences.getInt(ANIMATION_SPIN, 20)
    var viewWidth = width
    private var mBaseSize = 0f
    private var mCurrentPlayTime: Long = 0
    // Speed in dp per s
    private var speedSetting = 70
    // default colour
    private var colourList = colourListGenerator(redColours)
    private var selectedObject: MovingObject? = null

    init {
        mBaseSize = drawableList[0]!!.intrinsicWidth.coerceAtLeast(drawableList[0]!!.intrinsicHeight) / 3f
    }

    override fun onDraw(canvas: Canvas) {
        val viewHeight = height
        for (movingObject in objectList) {
            val objectSize = movingObject!!.scale * mBaseSize

            // Save the current canvas state
            val save = canvas.save()

            // Move the canvas to the center of the leaf
            canvas.translate(movingObject.x, movingObject.y)

            // Rotate the canvas based on how far the leaf has moved
            val progress = (movingObject.y + objectSize) / viewHeight
            canvas.rotate(movingObject.spin * progress)

            // Prepare the size and alpha of the drawable
            val size = objectSize.roundToInt()
            movingObject.drawable.setBounds(-size, -size, size, size)
            movingObject.drawable.alpha = (255 * movingObject.alpha).roundToInt()

            if (!usingCustomDrawable) movingObject.drawable.setTint(movingObject.colour)

            // Draw the object to the canvas
            movingObject.drawable.draw(canvas)

            // Restore the canvas to it's previous position and rotation
            canvas.restoreToCount(save)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mTimeAnimator = TimeAnimator()
        mTimeAnimator!!.setTimeListener(TimeAnimator.TimeListener { _, _, deltaTime ->
            if (!isLaidOut) {
                // Ignore all calls before the view has been measured and laid out.
                return@TimeListener
            }
            updateState(deltaTime.toFloat())
            invalidate()
        })
        mTimeAnimator!!.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mTimeAnimator!!.cancel()
        mTimeAnimator!!.setTimeListener(null)
        mTimeAnimator!!.removeAllListeners()
        mTimeAnimator = null
    }

    fun createObjects() {
        // The starting position is dependent on the size of the view,
        // which is why the model is initialized here, when the view is measured.
        for (i in objectList.indices) {
            val movingObject = MovingObject()
            initialiseAnimationObject(movingObject)
            objectList[i] = movingObject
        }
    }

    fun start() {
        if (mTimeAnimator != null) {
            if (mTimeAnimator!!.isPaused) resume()
            else mTimeAnimator!!.start()
        }
    }

    fun pause() {
        if (mTimeAnimator != null && mTimeAnimator!!.isRunning) {
            // Store the current play time for later.
            mCurrentPlayTime = mTimeAnimator!!.currentPlayTime
            mTimeAnimator!!.pause()
        }
    }

    private fun resume() {
        if (mTimeAnimator != null && mTimeAnimator!!.isPaused) {
            mTimeAnimator!!.start()
            mTimeAnimator!!.currentPlayTime = mCurrentPlayTime
        }
    }

    private fun updateState(deltaMs: Float) {
        // Converting to seconds since PX/S constants are easier to understand
        val deltaSeconds = deltaMs / 1000f
        for (movingObject in objectList) {
            if (movingObject != null && !movingObject.selected) {
                val leafSize = movingObject.scale * mBaseSize
                // Move the movingObject based on the elapsed time and it's speed
                movingObject.y += movingObject.speed * deltaSeconds

                // If the movingObject is completely outside of the view bounds after
                // updating it's position, recycle it.
                if (movingObject.y > height + leafSize) initialiseAnimationObject(movingObject)
            }
        }
    }

    private fun initialiseAnimationObject(movingObject: MovingObject) {
        // Set the scale based on a min value and a random multiplier
        movingObject.scale = SCALE_MIN_PART + SCALE_RANDOM_PART * mRnd.nextFloat()

        // Set X to a random value within the width of the view
        movingObject.x = viewWidth * mRnd.nextFloat()

        // Set the Y position
        // Start near the top of the view with a random offset
        val leafSize = movingObject.scale * mBaseSize
        movingObject.y = -leafSize

        // set the drawable image
        if (!drawableList.isNullOrEmpty()) movingObject.drawable = drawableList.random()!!

        // set the drawable colour
        if (!colourList.isNullOrEmpty()) movingObject.colour = colourList.random()

        // The alpha is determined by the scale of the star and a random multiplier.
        movingObject.alpha = ALPHA_SCALE_PART * movingObject.scale + ALPHA_RANDOM_PART * mRnd.nextFloat()
        // The bigger and brighter a star is, the faster it moves
        val mBaseSpeed = speedSetting * resources.displayMetrics.density
        movingObject.speed = mBaseSpeed * movingObject.alpha * movingObject.scale

        // generate a random clockwise or anticlockwise spin between 20 and 180 degrees
        val spin = Random().nextInt(180-20) + spinSpeed
        if (spin % 2 == 0) movingObject.spin = spin
        else movingObject.spin = -spin
    }

    private fun colourListGenerator(colours: List<Int>): ArrayList<Int> {
        val list = arrayListOf<Int>()
        for (c in colours) {
            val colour = ContextCompat.getColor(context, c)
            list.add(colour)
        }
        return list
    }

    private fun drawableListGenerator(drawables: List<Int>): ArrayList<Drawable?> {
        val list = arrayListOf<Drawable?>()
        for (d in drawables) {
            val drawable = ContextCompat.getDrawable(context, d)
            list.add(drawable)
        }
        return list
    }

    fun changeDrawable(drawable: String, updatePreferences: Boolean) {
        drawableList = when (drawable) {
            context.getString(R.string.space) -> drawableListGenerator(spaceDrawables)
            context.getString(R.string.mandala) -> drawableListGenerator(mandalaDrawables)
            context.getString(R.string.animals) -> drawableListGenerator(animalDrawables)
            context.getString(R.string.flowers) -> drawableListGenerator(flowerDrawables)
            context.getString(R.string.instruments) -> drawableListGenerator(instrumentsDrawables)
            else -> drawableListGenerator(leavesDrawables)
        }
        usingCustomDrawable = false
        if (updatePreferences){
            sharedPreferences.edit().apply {
                putString(ANIMATION_TYPE, drawable)
                apply()
            }
            Toast.makeText(context, resources.getString(R.string.changes_applied), Toast.LENGTH_SHORT).show()
        }
    }

    fun changeColour(colour: String, updatePreferences: Boolean) {
        colourList = when (colour) {
            context.getString(R.string.blue) -> colourListGenerator(blueColours)
            context.getString(R.string.night) -> colourListGenerator(nightColours)
            context.getString(R.string.pastel) -> colourListGenerator(pastelColours)
            else -> colourListGenerator(redColours)
        }
        if (updatePreferences){
            sharedPreferences.edit().apply {
                putString(ANIMATION_COLOUR, colour)
                apply()
            }
            Toast.makeText(context, resources.getString(R.string.changes_applied), Toast.LENGTH_SHORT).show()
        }
    }

    fun changeSpeed(speed: String, updatePreferences: Boolean) {
        speedSetting = when (speed) {
            context.getString(R.string.fast) -> 180
            context.getString(R.string.slow) -> 20
            else -> 70
        }
        if (updatePreferences){
            sharedPreferences.edit().apply {
                putString(ANIMATION_SPEED, speed)
                apply()
            }
            Toast.makeText(context, resources.getString(R.string.changes_applied), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val x = event?.x ?: 0f
        val y = event?.y ?: 0f

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val index = getTouchedObject(x, y)
                if (index != null) {
                    selectedObject = objectList[index]
                    if (selectedObject != null) selectedObject!!.selected = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedObject != null) {
                    selectedObject!!.x = x
                    selectedObject!!.y = y
                }
            }
            MotionEvent.ACTION_UP -> {
                if (selectedObject != null) selectedObject!!.selected = false
                selectedObject = null
            }
        }


        return super.onTouchEvent(event)
    }

    private fun getTouchedObject(x: Float, y: Float): Int? {
        objectList.forEachIndexed { i, movingObject ->
            if (movingObject != null) {
                val objectX = movingObject.x.toInt()
                val objectY = movingObject.y.toInt()
                val bounds = movingObject.drawable.bounds
                val occupiedSpace = Rect(bounds.left + objectX, bounds.top + objectY, bounds.right + objectX, bounds.bottom + objectY)

                if (occupiedSpace.contains(x.toInt(), y.toInt())) return i
            }
        }
        return null
    }
}