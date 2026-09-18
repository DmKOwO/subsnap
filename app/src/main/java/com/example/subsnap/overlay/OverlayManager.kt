package com.example.subsnap.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class OverlayManager(
    private val context: Context,
    private val onCaptureClick: () -> Unit,
    private val onToggleAutoClick: () -> Unit,
    private val onOpenAppClick: () -> Unit,
    private val onCloseClick: () -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootLayout: LinearLayout? = null
    private var mainBubble: View? = null
    private var menuContainer: LinearLayout? = null
    private var autoStatusDot: View? = null
    private var autoStatusText: TextView? = null
    private var autoBadge: TextView? = null

    private var isMenuExpanded = false
    private var isAutoCapturing = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        gravity = Gravity.TOP or Gravity.START
        x = 50
        y = 300
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        ).toInt()
    }

    fun onConfigurationChanged(newScreenWidth: Int, newScreenHeight: Int) {
        mainHandler.post {
            val margin = dp(16f)
            val bubbleSize = dp(64f)
            layoutParams.x = layoutParams.x.coerceIn(margin, (newScreenWidth - bubbleSize).coerceAtLeast(margin))
            layoutParams.y = layoutParams.y.coerceIn(margin, (newScreenHeight - bubbleSize).coerceAtLeast(margin))
            rootLayout?.let {
                try {
                    windowManager.updateViewLayout(it, layoutParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        if (rootLayout != null) return

        rootLayout = createOverlayView()
        try {
            windowManager.addView(rootLayout, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        rootLayout?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        rootLayout = null
    }

    fun updateAutoCaptureState(enabled: Boolean) {
        isAutoCapturing = enabled
        mainHandler.post {
            autoStatusDot?.let { dot ->
                val bg = dot.background as? GradientDrawable
                bg?.setColor(if (enabled) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E"))
            }
            autoStatusText?.text = if (enabled) "Авто: ВКЛ" else "Авто: ВЫКЛ"

            mainBubble?.let { bubble ->
                val bg = bubble.background as? GradientDrawable
                if (enabled) {
                    bg?.colors = intArrayOf(Color.parseColor("#059669"), Color.parseColor("#047857"))
                    bg?.setStroke(dp(3f), Color.parseColor("#34D399"))
                } else {
                    bg?.colors = intArrayOf(Color.parseColor("#6366F1"), Color.parseColor("#4338CA"))
                    bg?.setStroke(dp(2f), Color.parseColor("#FFFFFF"))
                }
            }
            autoBadge?.visibility = if (enabled) View.VISIBLE else View.GONE
        }
    }

    fun onCaptureSuccess() {
        mainHandler.post {
            mainBubble?.let { bubble ->
                val originalScale = bubble.scaleX
                bubble.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(120)
                    .withEndAction {
                        bubble.animate()
                            .scaleX(originalScale)
                            .scaleY(originalScale)
                            .setDuration(120)
                            .start()
                    }
                    .start()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView(): LinearLayout {
        val dp = { value: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Expanded menu items container
        menuContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(180f), LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE1E1E2C"))
                cornerRadius = dp(16f).toFloat()
                setStroke(dp(1f), Color.parseColor("#44FFFFFF"))
            }

            // Button: Instant Capture
            addView(createMenuItem("📸 Сделать снимок") {
                onCaptureClick()
                toggleMenu()
            })

            // Button: Toggle Auto
            val autoItem = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
                background = createRippleDrawable()

                autoStatusDot = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(10f), dp(10f)).apply {
                        marginEnd = dp(8f)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isAutoCapturing) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E"))
                    }
                }
                addView(autoStatusDot)

                autoStatusText = TextView(context).apply {
                    text = if (isAutoCapturing) "Авто: ВКЛ" else "Авто: ВЫКЛ"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    isSingleLine = true
                }
                addView(autoStatusText)

                setOnClickListener {
                    onToggleAutoClick()
                    toggleMenu()
                }
            }
            addView(autoItem)

            // Button: Open App Gallery
            addView(createMenuItem("🖼️ Открыть карточки") {
                onOpenAppClick()
                toggleMenu()
            })

            // Button: Close Overlay & Service
            addView(createMenuItem("❌ Остановить") {
                onCloseClick()
            })
        }
        container.addView(menuContainer)

        // Main draggable bubble
        val bubbleFrame = FrameLayout(context).apply {
            val size = dp(56f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                topMargin = dp(6f)
            }
            elevation = dp(8f).toFloat()

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isAutoCapturing) {
                    colors = intArrayOf(Color.parseColor("#059669"), Color.parseColor("#047857"))
                    setStroke(dp(3f), Color.parseColor("#34D399"))
                } else {
                    colors = intArrayOf(Color.parseColor("#6366F1"), Color.parseColor("#4338CA"))
                    setStroke(dp(2f), Color.parseColor("#FFFFFF"))
                }
                orientation = GradientDrawable.Orientation.TL_BR
            }

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(26f), dp(26f), Gravity.CENTER)
                setImageResource(android.R.drawable.ic_menu_camera)
                setColorFilter(Color.WHITE)
            }
            addView(icon)

            autoBadge = TextView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                ).apply {
                    bottomMargin = dp(4f)
                }
                text = "AUTO"
                textSize = 8f
                setTextColor(Color.parseColor("#A7F3D0"))
                setPadding(dp(4f), dp(1f), dp(4f), dp(1f))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#DD064E3B"))
                    cornerRadius = dp(4f).toFloat()
                }
                visibility = if (isAutoCapturing) View.VISIBLE else View.GONE
            }
            addView(autoBadge)
        }
        mainBubble = bubbleFrame

        setupDraggable(mainBubble!!)
        container.addView(mainBubble)

        return container
    }

    private fun createMenuItem(text: String, onClick: () -> Unit): TextView {
        val dp = { value: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()
        }
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 13f
            isSingleLine = true
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            background = createRippleDrawable()
            setOnClickListener { onClick() }
        }
    }

    private fun createRippleDrawable(): GradientDrawable {
        val dp = { value: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()
        }
        return GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = dp(8f).toFloat()
        }
    }

    private fun toggleMenu() {
        isMenuExpanded = !isMenuExpanded
        menuContainer?.visibility = if (isMenuExpanded) View.VISIBLE else View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggable(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var isLongPressTriggered = false
        var downTime = 0L
        val longPressHandler = Handler(Looper.getMainLooper())
        val longPressRunnable = Runnable {
            if (!isDragging && !isMenuExpanded) {
                isLongPressTriggered = true
                toggleMenu()
                triggerHapticFeedback()
            }
        }

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    isLongPressTriggered = false
                    downTime = System.currentTimeMillis()
                    longPressHandler.postDelayed(longPressRunnable, 400)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (Math.abs(deltaX) > 12 || Math.abs(deltaY) > 12) {
                        isDragging = true
                        isLongPressTriggered = false
                        longPressHandler.removeCallbacks(longPressRunnable)
                        layoutParams.x = initialX + deltaX
                        layoutParams.y = initialY + deltaY
                        try {
                            windowManager.updateViewLayout(rootLayout, layoutParams)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressHandler.removeCallbacks(longPressRunnable)

                    if (!isDragging) {
                        if (isLongPressTriggered) {
                            isLongPressTriggered = false
                        } else if (isMenuExpanded) {
                            toggleMenu()
                        } else {
                            // FAST TAP: Instant screenshot in 1 action!
                            onCaptureClick()
                        }
                    } else {
                        snapToEdge()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    true
                }
                else -> false
            }
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // ignore if vibration unavailable
        }
    }

    private fun snapToEdge() {
        val dp = { value: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()
        }
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val bubbleWidth = mainBubble?.width?.takeIf { it > 0 } ?: dp(56f)
        val margin = dp(12f)

        val currentX = layoutParams.x
        val targetX = if (currentX + (bubbleWidth / 2) < screenWidth / 2) {
            margin
        } else {
            screenWidth - bubbleWidth - margin
        }

        val animator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                layoutParams.x = animation.animatedValue as Int
                try {
                    windowManager.updateViewLayout(rootLayout, layoutParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        animator.start()
    }
}
