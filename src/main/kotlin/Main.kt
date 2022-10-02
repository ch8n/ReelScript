import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.*
import java.awt.font.GlyphVector
import java.awt.font.TextAttribute
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.text.AttributedString


const val DIRECTORY_OUTPUT = "output"

suspend fun main() = coroutineScope {
    resetDirectory()

    println("Getting images!")
    val imageUrl = "https://i.picsum.photos/id/893/4342/2895.jpg?hmac=fQQo3ufVfIXHYgZrJgCHACadJMc9Uw0We_nFVRIbzcM"
    val imageStream = getImageStreamOrNull(imageUrl) ?: return@coroutineScope
    val image = ImmutableImage.loader().fromStream(imageStream)
    println("${image.width} X ${image.height} | ${image.ratio()}")

    println("Cropping to reel size!")
    val imageCropped = image.cropReelSize()
    imageCropped.write("result1")

    println("Appending Quote")
    val quote = "LoreIpsome sample is added to image!"
    val awtImage: BufferedImage = imageCropped.awt()
    val (imageWidth, imageHeight) = 1080 to 1920
    val imageWithQuote = with(awtImage) {
        val graphics = graphics
        val baseFont = Font(Font.MONOSPACED, Font.PLAIN, 24)

        // dynamic fontsize
        val newFontSize = graphics.getAdaptiveFontSizeRecursive(baseFont, quote)
        val adaptiveFont = baseFont.deriveFont(baseFont.style, newFontSize.toFloat())
        val adaptiveFontMetrics: FontMetrics = graphics.getFontMetrics(adaptiveFont)

        // text attribute
        val attributedText = AttributedString(quote)
        attributedText.addAttribute(TextAttribute.FONT, adaptiveFont)
        attributedText.addAttribute(TextAttribute.FOREGROUND, Color.GREEN)

        // text position - Centalized
        val positionX: Int = (imageWidth - adaptiveFontMetrics.stringWidth(quote)) / 2
        val positionY: Int = (imageHeight - adaptiveFontMetrics.height) / 2 + adaptiveFontMetrics.ascent

        // draw text
        graphics.drawString(attributedText.iterator, positionX, positionY)
        ImmutableImage.fromAwt(this)
    }
    imageWithQuote.write("result2")
    Unit
}

fun Graphics.getAdaptiveFontSizeRecursive(baseFont: Font, quote: String): Double {
    val (imageWidth, imageHeight) = 1080 to 1920
    val graphics = this
    val metrics: FontMetrics = graphics.getFontMetrics(baseFont)
    val vector: GlyphVector = baseFont.createGlyphVector(metrics.fontRenderContext, quote)
    val outline: Shape = vector.getOutline(0f, 0f)
    val expectedWidth: Double = outline.bounds.getWidth()
    val expectedHeight: Double = outline.bounds.getHeight()
    val widthBasedFontSize: Double = baseFont.size2D * imageWidth / expectedWidth
    val heightBasedFontSize: Double = baseFont.size2D * imageHeight / expectedHeight
    val textFits = imageWidth >= expectedWidth && imageHeight >= expectedHeight
    val newFontSize = if (widthBasedFontSize < heightBasedFontSize) widthBasedFontSize else heightBasedFontSize
    return if (textFits) {
        newFontSize
    } else {
        val newBaseFont = baseFont.deriveFont(baseFont.style, newFontSize.toFloat())
        getAdaptiveFontSizeRecursive(newBaseFont, quote)
    }
}

fun ImmutableImage.cropReelSize(): ImmutableImage {
    val window = 1080 to 1920
    return cover(window.first, window.second)
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
