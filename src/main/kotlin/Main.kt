import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.composite.AlphaComposite
import com.sksamuel.scrimage.composite.ColorBurnComposite
import com.sksamuel.scrimage.composite.ColorComposite
import com.sksamuel.scrimage.composite.GlowComposite
import com.sksamuel.scrimage.composite.LuminosityComposite
import com.sksamuel.scrimage.composite.OverlayComposite
import com.sksamuel.scrimage.composite.RedComposite
import com.sksamuel.scrimage.composite.SaturationComposite
import com.sksamuel.scrimage.composite.ScreenComposite
import com.sksamuel.scrimage.composite.SubtractComposite
import com.sksamuel.scrimage.filter.AlphaMaskFilter
import com.sksamuel.scrimage.filter.BlurFilter
import com.sksamuel.scrimage.filter.BorderFilter
import com.sksamuel.scrimage.nio.JpegWriter
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.*
import java.awt.font.TextAttribute
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.text.AttributedString
import kotlin.math.roundToInt


const val DIRECTORY_OUTPUT = "output"

suspend fun main() = coroutineScope {
    resetDirectory()

    println("Getting images!")
    val imageUrl =
        "https://images.unsplash.com/photo-1518837321959-58dfc718abcf?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=1170&q=80"
    val imageStream = getImageStreamOrNull(imageUrl) ?: return@coroutineScope
    val image = ImmutableImage.loader().fromStream(imageStream)
    println("${image.width} X ${image.height} | ${image.ratio()}")

    println("Cropping to reel size!")
    val reelImageSize = Size(1080, 1920)
    val imageCropped = image.cropSize(reelImageSize)
    println("${imageCropped.width} X ${imageCropped.height} | ${imageCropped.ratio()}")
    imageCropped.write("result1")

    println("Appending Quote")
    val quote =
        "Here strange creatures are watching the kotlin logo. You can drag'n'drop them as well as the logo. Doubleclick to add more creatures but be careful. They may be watching you!"

    val awtImage = imageCropped.awt()
    val imageWithQuote = with(awtImage) {
        val graphics = graphics
        val font = Font(Font.MONOSPACED, Font.BOLD, 52)
        val fontMetrics: FontMetrics = graphics.getFontMetrics(font)

        val lines = quote.getBoundFormattedLines(reelImageSize.center, fontMetrics)
        val maxWidth = lines.maxOf { fontMetrics.stringWidth(it) }
        val height = fontMetrics.height * lines.size
        val quoteSize = Size(width = maxWidth, height = height)

        var yLoc = (reelImageSize.center.height - quoteSize.center.height) + fontMetrics.ascent
        val xLoc = reelImageSize.center.width - quoteSize.center.width

        lines.forEach { line ->
            val lineWidth = fontMetrics.stringWidth(line)
            val attributedText = AttributedString(line)
            attributedText.addAttribute(TextAttribute.FONT, font)
            attributedText.addAttribute(TextAttribute.FOREGROUND, Color.WHITE)
            graphics.drawRoundRect(
                xLoc + (fontMetrics.ascent * 0.25).roundToInt(),
                yLoc - fontMetrics.ascent,
                lineWidth,
                fontMetrics.height,
                8,
                8
            )
            graphics.drawString(attributedText.iterator, xLoc, yLoc)
            yLoc += fontMetrics.height
        }

        ImmutableImage.fromAwt(this)
    }
    imageWithQuote.write("result2")
    Unit
}

data class Size(val width: Int, val height: Int) {
    val center get() = this.copy(width = width / 2, height = height / 2)
}


fun ImmutableImage.cropSize(size: Size): ImmutableImage {
    return cover(size.width, size.height)
}

fun resetDirectory() {
    println("Clean up!")
    val file = File(DIRECTORY_OUTPUT)
    file.deleteRecursively()
    file.mkdir()
}

fun getImageStreamOrNull(imageUrl: String): InputStream? {
    val okHttpClient = OkHttpClient()
    val imageRequest = Request.Builder().url(imageUrl).build()
    val response = okHttpClient.newCall(imageRequest).execute()
    return response.body?.byteStream()
}

fun ImmutableImage.write(nameWithExtension: String) {
    val writer = JpegWriter().withCompression(50)
    val file = output(writer, File("$DIRECTORY_OUTPUT/$nameWithExtension.jpeg"))
    println("Created : ${file.absoluteFile}")
}

fun String.getBoundFormattedLines(bounds: Size, fontMetrics: FontMetrics): List<String> {
    val input = this
    val words = input.split(" ")
    val windowWidth = bounds.width
    var lineStart = ""
    val lines = mutableListOf<String>()
    words.forEachIndexed { index, word ->
        lineStart = "$lineStart $word"
        val widthOnWindow = fontMetrics.stringWidth(lineStart)
        val isFittingOnWindow = widthOnWindow < windowWidth
        if (!isFittingOnWindow) {
            lines.add(lineStart)
            lineStart = ""
        }
    }
    if (lineStart.isNotEmpty()) {
        lines.add(lineStart)
    }
    return lines
}

