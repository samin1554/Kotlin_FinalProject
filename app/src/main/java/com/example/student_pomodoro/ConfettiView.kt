package com.example.student_pomodoro

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    private var isAnimating = false

    private val colors = listOf(
        0xFFE85D4E.toInt(),  // coral
        0xFFFF6B6B.toInt(),  // red
        0xFF6B9080.toInt(),  // sage
        0xFFD4A373.toInt(),  // gold
        0xFFFFD93D.toInt(),  // yellow
        0xFF4ECDC4.toInt(),  // teal
        0xFFA8E6CF.toInt()   // mint
    )

    private inner class Particle(
        var x: Float,
        var y: Float,
        val color: Int,
        val size: Float,
        var velocityX: Float,
        var velocityY: Float,
        val rotation: Float,
        var rotationSpeed: Float,
        val shape: Int // 0 = circle, 1 = rect
    ) {
        var alpha = 255
        var currentRotation = 0f
    }

    fun burst(count: Int = 80) {
        particles.clear()
        val width = this.width.toFloat()
        val height = this.height.toFloat()
        if (width == 0f || height == 0f) return

        val centerX = width / 2f
        val centerY = height / 3f

        repeat(count) {
            val angle = Random.nextDouble(0.0, Math.PI * 2).toFloat()
            val speed = Random.nextFloat() * 15f + 5f
            particles.add(
                Particle(
                    x = centerX,
                    y = centerY,
                    color = colors.random(),
                    size = Random.nextFloat() * 10f + 6f,
                    velocityX = kotlin.math.cos(angle) * speed,
                    velocityY = kotlin.math.sin(angle) * speed - 10f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 10f - 5f,
                    shape = Random.nextInt(2)
                )
            )
        }

        visibility = VISIBLE
        isAnimating = true
        alpha = 1f

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimating = false
                    visibility = GONE
                    particles.clear()
                }
            })
            start()
        }
    }

    private fun updateParticles() {
        val gravity = 0.4f
        val drag = 0.98f

        particles.forEach { p ->
            p.velocityY += gravity
            p.velocityX *= drag
            p.x += p.velocityX
            p.y += p.velocityY
            p.currentRotation += p.rotationSpeed
            if (p.alpha > 3) p.alpha -= 3
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating) return

        particles.forEach { p ->
            paint.color = p.color
            paint.alpha = p.alpha.coerceIn(0, 255)

            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.currentRotation)

            if (p.shape == 0) {
                canvas.drawCircle(0f, 0f, p.size / 2f, paint)
            } else {
                canvas.drawRect(-p.size / 2f, -p.size / 3f, p.size / 2f, p.size / 3f, paint)
            }

            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
