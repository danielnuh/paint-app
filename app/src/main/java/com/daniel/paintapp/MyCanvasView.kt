package com.daniel.paintapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat


private const val STROKE_WIDTH = 6f

class MyCanvasView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var path = Path()
    var isPaint:Boolean = false

    private val drawColor = ResourcesCompat.getColor(resources, R.color.black, null)
    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.white, null)
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap
    private lateinit var frame: Rect

    // Set up the paint with which to draw.
    private val paint = Paint().apply {
        color = drawColor
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = STROKE_WIDTH // default: Hairline-width (really thin)
    }

    /**
     * Don't draw every single pixel.
     * If the finger has has moved less than this distance, don't draw. scaledTouchSlop, returns
     * the distance in pixels a touch can wander before we think the user is scrolling.
     */
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    private var currentX = 0f
    private var currentY = 0f

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    /**
     * Called whenever the view changes size.
     * Since the view starts out with no size, this is also called after
     * the view has been inflated and has a valid size.
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)

        // Calculate a rectangular frame around the picture.
        val inset = 40
        frame = Rect(inset, inset, width - inset, height - inset)
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the bitmap that has the saved path.
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }

    /**
     * No need to call and implement MyCanvasView#performClick, because MyCanvasView custom view
     * does not handle click actions.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
            }
        }
        return true
    }

    /**
     * The following methods factor out what happens for different touch events,
     * as determined by the onTouchEvent() when statement.
     * This keeps the when conditional block
     * concise and makes it easier to change what happens for each event.
     * No need to call invalidate because we are not drawing anything.
     */
    private fun touchStart() {
        isPaint = true
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        isPaint = true
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2).
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to save it.
            extraCanvas.drawPath(path, paint)
        }
        // Invalidate() is inside the touchMove() under ACTION_MOVE because there are many other
        // types of motion events passed into this listener, and we don't want to invalidate the
        // view for those.
        invalidate()
    }

    private fun touchUp() {
        // Reset the path so it doesn't get drawn again.
        isPaint = true
        path.reset()
    }

    fun clearCanvas(){
        isPaint = false
        extraCanvas.drawColor(backgroundColor)
        extraCanvas.drawPath(path, paint)
        invalidate()
    }

    fun cropImage():Bitmap{
        val threshold = 100
        val srcBitmap = extraBitmap

        val startTop = getStartTopPixelDraw(srcBitmap)
        val startBottom = getStartBottomPixelDraw(srcBitmap)
        val startLeft = getStartLeftPixelDraw(srcBitmap, startTop, startBottom)
        val startRight = getStartRightPixelDraw(srcBitmap, startTop, startBottom)

        val squareBitmapWidth = (startRight-startLeft)+threshold
        val squareBitmapHeight = (startBottom-startTop)+threshold

        val dstBitmap = Bitmap.createBitmap(
            squareBitmapWidth,  // Width
            squareBitmapHeight,  // Height
            Bitmap.Config.ARGB_8888 // Config
        )

        val canvas = Canvas(dstBitmap)

        val paint = Paint()
        paint.isAntiAlias = true

        val rect = Rect(0, 0, squareBitmapWidth, squareBitmapHeight)
        val rectF = RectF(rect)
        canvas.drawRect(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val left:Float = -(startLeft)+(threshold/2).toFloat()
        val top:Float = -(startTop)+(threshold/2).toFloat()

        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(srcBitmap, left, top, paint)
        return dstBitmap
    }

    private fun getStartLeftPixelDraw(srcBitmap: Bitmap, startTop:Int, startBottom:Int):Int {
        var left = srcBitmap.width
        for(y in startTop until startBottom){
            for(x in 0 until srcBitmap.width){
                val pixel = srcBitmap.getPixel(x, y)
                if (pixel == drawColor && x < left){
                    left = x
                    break
                }
            }
        }
        return left
    }

    private fun getStartRightPixelDraw(srcBitmap: Bitmap, startTop:Int, startBottom:Int):Int {
        var right = 0
        for(y in startTop until startBottom){
            for(x in srcBitmap.width-1 downTo  0){
                val pixel = srcBitmap.getPixel(x, y)
                if (pixel == drawColor && x > right){
                    right = x
                    break
                }
            }
        }
        return right
    }

    private fun getStartTopPixelDraw(srcBitmap: Bitmap):Int {
        var top = 0
        var isBreak = false
        for(y in 0 until srcBitmap.height){
            for(x in 0 until srcBitmap.width){
                val pixel = srcBitmap.getPixel(x, y)
                if (pixel == drawColor && y > top){
                    top = y
                    isBreak = true
                }
            }
            if (isBreak) break
        }
        return top
    }

    private fun getStartBottomPixelDraw(srcBitmap: Bitmap):Int {
        var bottom = 0
        var isBreak = false
        for(y in srcBitmap.height-1 downTo 0){
            for(x in 0 until srcBitmap.width){
                val pixel = srcBitmap.getPixel(x, y)
                if (pixel == drawColor && y > bottom){
                    bottom = y
                    isBreak = true
                }
            }
            if (isBreak) break
        }
        return bottom
    }
}