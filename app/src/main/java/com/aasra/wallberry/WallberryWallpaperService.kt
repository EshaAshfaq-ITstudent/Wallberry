package com.aasra.wallberry

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import kotlin.random.Random

class WallberryWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = BerryEngine()

    inner class BerryEngine : Engine() {
        private val paint = Paint().apply {
            color = 0xFFE89090.toInt() // CoralPink
            isAntiAlias = true
        }
        private var visible = false
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { draw() }

        // Berry objects
        private val berries = List(15) { Berry() }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) draw() else handler.removeCallbacks(drawRunnable)
        }

        override fun onSurfaceDestroyed(holder: android.view.SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                // React to touch: push berries away
                berries.forEach { it.reactToTouch(event.x, event.y) }
            }
            super.onTouchEvent(event)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(0xFFFFFAF8.toInt()) // WarmCream Background
                    berries.forEach { 
                        it.update(canvas.width, canvas.height)
                        it.draw(canvas, paint) 
                    }
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            handler.removeCallbacks(drawRunnable)
            if (visible) handler.postDelayed(drawRunnable, 30)
        }

        inner class Berry {
            var x = Random.nextFloat() * 1000
            var y = Random.nextFloat() * 2000
            var radius = Random.nextFloat() * 30 + 20
            var speedX = Random.nextFloat() * 4 - 2
            var speedY = Random.nextFloat() * 4 - 2

            fun update(width: Int, height: Int) {
                x += speedX
                y += speedY
                if (x < 0 || x > width) speedX *= -1
                if (y < 0 || y > height) speedY *= -1
            }

            fun draw(canvas: Canvas, paint: Paint) {
                canvas.drawCircle(x, y, radius, paint)
            }

            fun reactToTouch(tx: Float, ty: Float) {
                val dx = x - tx
                val dy = y - ty
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble())
                if (dist < 300) {
                    speedX += dx / 10
                    speedY += dy / 10
                }
            }
        }
    }
}
