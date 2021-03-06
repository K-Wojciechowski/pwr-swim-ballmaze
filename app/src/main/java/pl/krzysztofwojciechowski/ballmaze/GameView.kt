package pl.krzysztofwojciechowski.ballmaze

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import java.util.concurrent.ThreadLocalRandom



class GameView internal constructor(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var canvas: Canvas? = null
    private val paint: Paint
    private var lightValue = 0f
    private var mode = GameMode.START
    private var thread: GameThread? = null
    private val UNIT: Int

    private var screenWidth = 0
    private var screenHeight = 0
    private var maxVisibleFloors = 0

    private val gaps = IntArray(FLOORS)
    private val colors = IntArray(FLOORS)
    private val heights = IntArray(FLOORS)

    private var ballShift = 0f

    private var ballSize_px = 0
    private var ballX_px = 0
    private var ballY_px = 0
    private var topY_px = 0
    private var ballCentered_px = 0
    private var ballDistance_px = 0
    private var scrollDist_px = 0
    private var minLeftBallPosition_px = 0
    private var maxRightBallPosition_px = 0
    private var floorRectHeight_px = 0
    private var gapWidth_px = 0

    private var score = 0

    init {
        holder.addCallback(this)
        canvas = Canvas()
        paint = Paint()
        UNIT = getDim(R.dimen._unit_).toInt()
        setOnClickListener { if (mode !== GameMode.INGAME) startGame() }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        draw()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // No meaningful support.
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGame()
    }

    private fun startGame() {
        mode = GameMode.INGAME
        val range = (screenWidth.toFloat() - 2 * getDim(R.dimen.gap_margin_size) - gapWidth_px.toFloat() + 1).toInt()
        val ballRangePartial = getDim(R.dimen.ball_range_partial)
        val partialRange = (range / ballRangePartial).toInt()
        val rand = ThreadLocalRandom.current()

        val colorArray = resources.getIntArray(R.array.floor_colors)
        heights[0] = getDim(R.dimen.first_floor_height).toInt()
        for (i in 0 until FLOORS) {
            gaps[i] = getDim(R.dimen.gap_margin_size).toInt() + (rand.nextInt(partialRange) * ballRangePartial).toInt()
            colors[i] = colorArray[i % colorArray.size]
            if (i != 0) heights[i] = heights[i - 1] + getDim(R.dimen.distance_between_floors).toInt()
        }

        maxVisibleFloors = Math.ceil((screenHeight / getDim(R.dimen.distance_between_floors)).toDouble()).toInt()

        ballX_px = ballCentered_px
        ballY_px = 0
        topY_px = -ballSize_px
        score = 0
        thread = GameThread()
        thread!!.start()
    }

    fun stopGame() {
        mode = GameMode.START
        if (thread != null) thread!!.join()
    }

    private fun draw() {
        if (!holder.surface.isValid) return
        canvas = holder.lockCanvas()
        if (canvas == null) return
        if (lightValue < DARKMODE_THRESHOLD) {
            canvas!!.drawColor(getColor(R.color.background_dark))
            paint.color = getColor(R.color.foreground_dark)
        } else {
            canvas!!.drawColor(getColor(R.color.background_light))
            paint.color = getColor(R.color.foreground_light)
        }
        when (mode) {
            GameMode.START -> drawStart(canvas!!)
            GameMode.INGAME -> drawInGame(canvas!!)
            GameMode.LOSE -> drawLose(canvas!!)
            GameMode.WIN -> drawWin(canvas!!)
        }
        holder.unlockCanvasAndPost(canvas)
    }

    private fun computeDimensions() {
        screenWidth = width
        screenHeight = height
        ballCentered_px = screenWidth / 2
        ballSize_px = getDim(R.dimen.ball_size).toInt()
        ballDistance_px = ballCentered_px - (getDim(R.dimen.gap_margin_size) + ballSize_px).toInt()
        scrollDist_px = getDim(R.dimen.scroll_distance).toInt()
        minLeftBallPosition_px = (getDim(R.dimen.ball_margin_size) + ballSize_px).toInt()
        maxRightBallPosition_px = screenWidth - minLeftBallPosition_px
        floorRectHeight_px = getDim(R.dimen.floor_rect_height).toInt()
        gapWidth_px = getDim(R.dimen.gap_width).toInt()
    }

    private fun drawStart(canvas: Canvas) {
        // Compute dimensions before game starts
        computeDimensions()

        // Draw game title
        paint.textSize = getDim(R.dimen.title_size)
        drawCenterX(
            canvas, paint, getString(R.string.app_name), getDim(R.dimen.title_position)
        )

        // Draw objective title
        paint.textSize = getDim(R.dimen.objective_title_size)
        drawCenterX(
            canvas, paint, getString(R.string.objective_title), getDim(R.dimen.objective_title_position)
        )

        // Draw objective text content
        val objective = resources.getString(R.string.objective, FLOORS)
        val tp = TextPaint()
        tp.textSize = getDim(R.dimen.objective_text_size)
        tp.color = paint.color
        val sl = StaticLayout.Builder.obtain(
            objective, 0, objective.length, tp,
            (width - getDim(R.dimen.text_margin)).toInt()
        ).build()

        val objPos = getDim(R.dimen.objective_position)
        canvas.translate(0f, objPos)
        sl.draw(canvas)
        paint.textSize = getDim(R.dimen.big_text_size)
        canvas.translate(0f, -objPos)

        // Draw START prompt
        val oldtf = paint.typeface
        paint.typeface = Typeface.DEFAULT_BOLD
        drawCenterX(canvas, paint, getString(R.string.tap_to_start), -1f)
        paint.typeface = oldtf
    }

    private fun drawInGame(canvas: Canvas) {
        val displayY = resources.getDimension(R.dimen.ball_from_top)

        val foreColor = paint.color
        canvas.drawCircle(ballX_px.toFloat(), displayY, resources.getDimension(R.dimen.ball_size), paint)
        if (score > 0) {
            val scHeight = heights[score - 1]
            if (scHeight > ballY_px - ballSize_px - floorRectHeight_px) drawAt(score - 1, canvas)
        }
        val maxShown = Math.min(score + maxVisibleFloors, FLOORS - 1)
        for (i in score..maxShown) {
            drawAt(i, canvas)
        }

        // Draw score with shadow
        paint.color = getColor(R.color.text_shadow)
        canvas.drawText(
            Integer.toString(score),
            getDim(R.dimen.score_margin) + UNIT,
            ((screenHeight - getDim(R.dimen.score_margin)).toInt() + UNIT).toFloat(),
            paint
        )

        paint.color = foreColor
        canvas.drawText(
            Integer.toString(score),
            getDim(R.dimen.score_margin),
            (screenHeight - getDim(R.dimen.score_margin)).toInt().toFloat(),
            paint
        )
    }

    private fun drawAt(height: Int, canvas: Canvas) {
        val top = heights[height] - topY_px
        paint.color = colors[height]
        val (rectA, rectB) = getRectsFor(top, gaps[height])
        canvas.drawRect(rectA, paint)
        canvas.drawRect(rectB, paint)
    }

    private fun getRectsFor(top: Int, gapDistance: Int): Pair<Rect, Rect> {
        return Pair(
            Rect(0, top, gapDistance, top + floorRectHeight_px),
            Rect(gapDistance + gapWidth_px, top, screenWidth, top + floorRectHeight_px)
        )
    }

    private fun drawWin(canvas: Canvas) {
        if (lightValue < DARKMODE_THRESHOLD) {
            canvas.drawColor(getColor(R.color.win_dark))
        } else {
            canvas.drawColor(getColor(R.color.win_light))
        }

        drawGameOver(canvas, getString(R.string.you_won))
    }

    private fun drawLose(canvas: Canvas) {
        if (lightValue < DARKMODE_THRESHOLD) {
            canvas.drawColor(getColor(R.color.lose_dark))
        } else {
            canvas.drawColor(getColor(R.color.lose_light))
        }

        drawGameOver(canvas, getString(R.string.you_lost))
    }

    private fun drawGameOver(canvas: Canvas, wlText: String) {
        // Game title
        paint.textSize = getDim(R.dimen.title_size)
        drawCenterX(
            canvas, paint, getString(R.string.app_name), getDim(R.dimen.title_position)
        )

        // YOU WON/LOST
        paint.textSize = getDim(R.dimen.big_text_size)
        drawCenterX(
            canvas, paint, wlText, getDim(R.dimen.gameover_title_position)
        )

        // Score
        drawCenterX(
            canvas, paint, resources.getString(R.string.score, score), getDim(R.dimen.gameover_score_position)
        )

        // START prompt
        val oldtf = paint.typeface
        paint.typeface = Typeface.DEFAULT_BOLD
        drawCenterX(canvas, paint, getString(R.string.play_again), -1f)
        paint.typeface = oldtf
    }

    // https://stackoverflow.com/a/32081250
    private fun drawCenterX(canvas: Canvas, paint: Paint, text: String, _y: Float) {
        var y = _y
        val r = canvas.clipBounds
        val cHeight = r.height()
        val cWidth = r.width()
        paint.textAlign = Paint.Align.LEFT
        paint.getTextBounds(text, 0, text.length, r)

        val x = cWidth / 2f - r.width() / 2f - r.left.toFloat()
        if (y == -1f) y = cHeight / 2f + r.height() / 2f - r.bottom
        canvas.drawText(text, x, y, paint)
    }

    private fun getDim(resId: Int): Float {
        return resources.getDimension(resId)
    }

    private fun getString(resId: Int): String {
        return resources.getString(resId)
    }

    private fun getColor(colorId: Int): Int {
        return ContextCompat.getColor(context, colorId)
    }

    fun handleLightSensor(value: Float) {
        lightValue = value
        if (mode !== GameMode.INGAME) draw()
        // otherwise, the GameThread handles this
    }

    fun handleAccelerometer(x: Float) {
        ballShift = x
    }

    internal inner class GameThread : Thread() {
        override fun run() {
            while (mode === GameMode.INGAME) {
                runGameTick()
                draw()
                sleep(16)

            }
        }

        private fun runGameTick() {
            ballX_px += (ballShift * ballDistance_px).toInt()
            if (ballX_px < minLeftBallPosition_px) {
                ballX_px = minLeftBallPosition_px
            }
            if (ballX_px > maxRightBallPosition_px) {
                ballX_px = maxRightBallPosition_px
            }

            ballY_px += scrollDist_px
            topY_px += scrollDist_px

            // Collision detection
            // The collision will be with the next floor, so floor #score
            val (rectA, rectB) = getRectsFor(heights[score], gaps[score])
            val circleRect = getRectForCollisionDetection(ballX_px, ballY_px)
            if (rectA.intersect(circleRect) || rectB.intersect(circleRect)) {
                mode = GameMode.LOSE
            }

            if (ballY_px - (2 * ballSize_px) > heights[score]) {
                score++
            }
            if (score == FLOORS) {
                mode = GameMode.WIN
            }
        }

        /** Simplify the ball (circle) into a more forgiving rectangle. */
        private fun getRectForCollisionDetection(circleX: Int, circleY: Int): Rect {
            val radius = ballSize_px
            val rectLeft = circleX - radius + (CD_RECT_X_DIFF * UNIT).toInt()
            val rectTop = circleY - (radius / CD_RECT_Y_QUOT).toInt() + (CD_RECT_Y_DIFF * UNIT).toInt()
            val rectRight = circleX + radius - (CD_RECT_X_DIFF * UNIT).toInt()
            val rectBottom = circleY + (radius / CD_RECT_Y_QUOT).toInt() - (CD_RECT_Y_DIFF * UNIT).toInt()
            return Rect(rectLeft, rectTop, rectRight, rectBottom)
        }
    }
}
