package com.codersguidebook.supernova.views

import android.animation.TimeAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_COLOUR
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_QUANTITY
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_SPEED
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_SPIN
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_TYPE
import java.util.*
import kotlin.math.roundToInt

class PlaybackAnimator(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        private const val SCALE_MIN_PART = 0.45f
        private const val SCALE_RANDOM_PART = 0.55f
        private const val ALPHA_SCALE_PART = 0.9f
        private const val ALPHA_RANDOM_PART = 0.5f
    }

    inner class MovingObject {
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

    private val redColours = listOf(
        R.color.red1, R.color.red2, R.color.red3,
        R.color.red4, R.color.red5, R.color.red6, R.color.red7
    )
    private val blueColours = listOf(
        R.color.blue1, R.color.blue2, R.color.blue3,
        R.color.blue4, R.color.blue5, R.color.blue6, R.color.blue7
    )
    private val nightColours = listOf(
        R.color.night1, R.color.night2, R.color.night3,
        R.color.night4, R.color.night5, R.color.night6, R.color.night7
    )
    private val pastelColours = listOf(
        R.color.nav_home, R.color.nav_playing, R.color.nav_playlists,
        R.color.nav_artists, R.color.nav_albums, R.color.nav_songs, R.color.nav_settings
    )
    private val leavesDrawables = listOf(R.drawable.leaf)
    private val instrumentsDrawables = listOf(
        R.drawable.drums,
        R.drawable.piano,
        R.drawable.saxophone,
        R.drawable.saxophone
    )
    private val spaceDrawables = listOf(
        R.drawable.star,
        R.drawable.star,
        R.drawable.earth,
        R.drawable.planet,
        R.drawable.saturn
    )
    private val mandalaDrawables = listOf(
        R.drawable.mandala1,
        R.drawable.mandala2,
        R.drawable.mandala3,
        R.drawable.mandala4
    )
    private val animalDrawables = listOf(
        R.drawable.cat,
        R.drawable.dolphin,
        R.drawable.elephant,
        R.drawable.peacock,
        R.drawable.wolf
    )
    private val flowerDrawables = listOf(
        R.drawable.flower,
        R.drawable.poppy,
        R.drawable.rose1,
        R.drawable.rose2
    )
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val count = sharedPreferences.getInt(ANIMATION_QUANTITY, 6)
    private val objectList = MutableList(count) { MovingObject() }
    private var mTimeAnimator: TimeAnimator? = null
    var usingCustomDrawable = false
    var drawableList = drawableListGenerator()
    private var spinSpeed = sharedPreferences.getInt(ANIMATION_SPIN, 20)
    private var mBaseSize = 0f
    private var speedSetting = 70
    private var colourList = colourListGenerator()
    private var selectedObject: MovingObject? = null

    init {
        sharedPreferences.getString(ANIMATION_COLOUR, null)?.let { changeColour(it) }

        sharedPreferences.getString(ANIMATION_SPEED, null)?.let { changeSpeed(it) }

        if (drawableList.isNotEmpty()) {
            mBaseSize = drawableList[0].intrinsicWidth.coerceAtLeast(drawableList[0].intrinsicHeight) / 3f
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (movingObject in objectList) {
            val objectSize = movingObject.scale * mBaseSize

            // Save the current canvas state
            val save = canvas.save()

            // Move the canvas to the center of the animation object
            canvas.translate(movingObject.x, movingObject.y)

            // Rotate the canvas based on how far the object has travelled
            val progress = (movingObject.y + objectSize) / measuredHeight
            canvas.rotate(movingObject.spin * progress)

            // Prepare the size and alpha of the drawable
            val size = objectSize.roundToInt()
            movingObject.drawable.setBounds(-size, -size, size, size)
            movingObject.drawable.alpha = (255 * movingObject.alpha).roundToInt()

            if (!usingCustomDrawable) movingObject.drawable.setTint(movingObject.colour)

            movingObject.drawable.draw(canvas)

            // Restore the canvas to it's previous position and rotation
            canvas.restoreToCount(save)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mTimeAnimator = TimeAnimator().apply {
            setTimeListener(TimeAnimator.TimeListener { _, _, deltaTime ->
                // Ignore all calls before the view has been measured and laid out.
                if (!isLaidOut) return@TimeListener
                updateState(deltaTime.toFloat())
                invalidate()
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mTimeAnimator?.apply {
            cancel()
            setTimeListener(null)
            removeAllListeners()
        }
        mTimeAnimator = null
    }

    /** Start or resume the animator. */
    fun start() {
        mTimeAnimator?.apply {
            if (this.isPaused) resumeAnimator()
            else this.start()
        }
    }

    /** Pause the animator. */
    fun pause() {
        if (mTimeAnimator?.isRunning == true) {
            mTimeAnimator?.pause()
        }
    }

    /** Resume the animator */
    private fun resumeAnimator() {
        if (mTimeAnimator?.isPaused == true) {
            mTimeAnimator?.start()
        }
    }

    /**
     * Update the position of each animation object based on how much time has elapsed.
     *
     * @param deltaMs The delta time provided by the TimeAnimator since the last update.
     */
    private fun updateState(deltaMs: Float) {
        // Converting to seconds since PX/S constants are easier to understand
        val deltaSeconds = deltaMs / 1000f
        for (movingObject in objectList) {
            if (!movingObject.selected) {
                val objectSize = movingObject.scale * mBaseSize
                // Move the object based on the elapsed time and it's speed
                movingObject.y += movingObject.speed * deltaSeconds

                // If the object is travels outside of the View bounds then recycle it.
                if (movingObject.y > measuredHeight + objectSize) initialiseAnimationObject(movingObject)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != 0 && h != 0) {
            for (obj in objectList) initialiseAnimationObject(obj, true)
        }
    }

    /**
     * Initialise an object for a new animation cycle.
     *
     * @param movingObject The animation object.
     * @param randomYStart A Boolean indicating whether the start position on the Y axis should be random.
     * Default = false, because after the first animation cycle the object should always reappear from
     * the top of the screen.
     */
    private fun initialiseAnimationObject(movingObject: MovingObject, randomYStart: Boolean = false) {
        // Set the scale based on a min value and a random multiplier
        movingObject.scale = SCALE_MIN_PART + SCALE_RANDOM_PART * Random().nextFloat()

        val objectSize = movingObject.scale * mBaseSize
        movingObject.x = measuredWidth * Random().nextFloat()
        movingObject.y = if (randomYStart && measuredHeight > 0) Random().nextInt(measuredHeight).toFloat()
        else -objectSize

        if (drawableList.isNotEmpty()) movingObject.drawable = drawableList.random()

        if (colourList.isNotEmpty()) movingObject.colour = colourList.random()

        // The alpha is determined by the scale of the star and a random multiplier.
        movingObject.alpha = ALPHA_SCALE_PART * movingObject.scale + ALPHA_RANDOM_PART * Random().nextFloat()
        // The bigger and brighter an object is, the faster it moves
        val mBaseSpeed = speedSetting * resources.displayMetrics.density
        movingObject.speed = mBaseSpeed * movingObject.alpha * movingObject.scale

        // Generate a random clockwise or anticlockwise spin between 20 and 180 degrees
        val spin = Random().nextInt(180-20) + spinSpeed
        if (spin % 2 == 0) movingObject.spin = spin
        else movingObject.spin = -spin
    }

    /**
     * Generate a list of colour values for the animation objects.
     *
     * @param colours A list of colour resource IDs to render.
     * Default = the list of red colour resource IDs.
     * @return A list of colour values.
     */
    private fun colourListGenerator(colours: List<Int> = redColours): List<Int> {
        return colours.mapNotNull { colour ->
            try {
                ContextCompat.getColor(context, colour)
            } catch (_: NotFoundException) {
                null
            }
        }
    }

    /**
     * Generate a list of Drawable resources for the animation objects.
     *
     * @param drawables A list of Drawable resource IDs to render.
     * Default = the list of Drawable resource IDs for the leaves.
     * @return A list of Drawable resources.
     */
    private fun drawableListGenerator(drawables: List<Int> = leavesDrawables): List<Drawable> {
        return drawables.mapNotNull { drawable ->
            ContextCompat.getDrawable(context, drawable)
        }
    }

    /**
     * Change the drawable theme of the animation objects.
     *
     * @param drawable The selected drawable theme.
     * @param updatePreferences A Boolean indicating whether the shared preferences should be
     * updated with the selected speed. Default = false.
     */
    fun changeDrawable(drawable: String, updatePreferences: Boolean = false) {
        drawableList = when (drawable) {
            context.getString(R.string.space) -> drawableListGenerator(spaceDrawables)
            context.getString(R.string.mandala) -> drawableListGenerator(mandalaDrawables)
            context.getString(R.string.animals) -> drawableListGenerator(animalDrawables)
            context.getString(R.string.flowers) -> drawableListGenerator(flowerDrawables)
            context.getString(R.string.instruments) -> drawableListGenerator(instrumentsDrawables)
            else -> drawableListGenerator()
        }
        usingCustomDrawable = false
        if (updatePreferences){
            sharedPreferences.edit().apply {
                putString(ANIMATION_TYPE, drawable)
                apply()
            }
            Toast.makeText(context, resources.getString(R.string.changes_applied),
                Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Change the colour of the animation objects.
     *
     * @param colour The selected colour.
     * @param updatePreferences A Boolean indicating whether the shared preferences should be
     * updated with the selected speed. Default = false.
     */
    fun changeColour(colour: String, updatePreferences: Boolean = false) {
        colourList = when (colour) {
            context.getString(R.string.blue) -> colourListGenerator(blueColours)
            context.getString(R.string.night) -> colourListGenerator(nightColours)
            context.getString(R.string.pastel) -> colourListGenerator(pastelColours)
            else -> colourListGenerator()
        }
        if (updatePreferences){
            sharedPreferences.edit().apply {
                putString(ANIMATION_COLOUR, colour)
                apply()
            }
            Toast.makeText(context, resources.getString(R.string.changes_applied),
                Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Change the speed of the animation objects' movement.
     *
     * @param speed The selected speed.
     * @param updatePreferences A Boolean indicating whether the shared preferences should be
     * updated with the selected speed. Default = false.
     */
    fun changeSpeed(speed: String, updatePreferences: Boolean = false) {
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
            Toast.makeText(context, resources.getString(R.string.changes_applied),
                Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event?.x ?: 0f
        val y = event?.y ?: 0f

        when (event?.action) {
            ACTION_DOWN -> {
                getTouchedObject(x, y)?.let {
                    selectedObject = objectList[it].apply {
                        this.selected = true
                    }
                    return true
                }
            }
            ACTION_MOVE -> {
                selectedObject?.apply {
                    this.x = x
                    this.y = y
                    return true
                }
            }
            else -> {
                selectedObject?.selected = false
                selectedObject = null
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Finds the selected animation object following a touch event at a given position.
     *
     * @param x The X coordinate of the touch event.
     * @param y The Y coordinate of the touch event.
     * @return The index of the selected animation object in the objectList list, or null
     * if no object was selected.
     */
    private fun getTouchedObject(x: Float, y: Float): Int? {
        objectList.forEachIndexed { index, obj ->
            val objectX = obj.x.toInt()
            val objectY = obj.y.toInt()
            val bounds = obj.drawable.bounds
            val occupiedSpace = Rect(bounds.left + objectX,
                bounds.top + objectY, bounds.right + objectX,
                bounds.bottom + objectY)

            if (occupiedSpace.contains(x.toInt(), y.toInt())) return index
        }
        return null
    }
}