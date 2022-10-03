import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.composite.AlphaComposite
import com.sksamuel.scrimage.composite.OverlayComposite
import com.sksamuel.scrimage.filter.AlphaMaskFilter
import com.sksamuel.scrimage.filter.BlurFilter
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
    val imageUrl = "https://i.picsum.photos/id/893/4342/2895.jpg?hmac=fQQo3ufVfIXHYgZrJgCHACadJMc9Uw0We_nFVRIbzcM"
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

    val awtImage: BufferedImage = imageCropped.awt()
    val imageWithQuote = with(awtImage) {
        val graphics = graphics
        val font = Font(Font.MONOSPACED, Font.BOLD, 52)
        val fontMetrics: FontMetrics = graphics.getFontMetrics(font)

        val lines = quote.getBoundFormattedLines(reelImageSize.center, fontMetrics)
        val maxWidth = lines.maxOf { fontMetrics.stringWidth(it) }
        val height = fontMetrics.height * lines.size
        val quoteSize = Size(width = maxWidth, height = height)

        val blurFontBg = imageCropped
            .cropSize(
                quoteSize.copy(
                    width = quoteSize.width + 50,
                    height = quoteSize.height + 50,
                )
            )
            .filter(BlurFilter())

        blurFontBg.write("result3")

        var yLoc = (reelImageSize.center.height - quoteSize.center.height) + fontMetrics.ascent
        val xLoc = reelImageSize.center.width - quoteSize.center.width

        val overlay = imageCropped.overlay(
            blurFontBg, xLoc, yLoc - (fontMetrics.ascent * 1.5).roundToInt()
        )

        lines.forEach { line ->
            val attributedText = AttributedString(line)
            attributedText.addAttribute(TextAttribute.FONT, font)
            attributedText.addAttribute(TextAttribute.FOREGROUND, Color.GREEN)
            graphics.drawString(attributedText.iterator, xLoc, yLoc)
            yLoc += fontMetrics.height
        }

        ImmutableImage.fromAwt(this).composite(
            OverlayComposite(1.0),
            overlay
        )
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

