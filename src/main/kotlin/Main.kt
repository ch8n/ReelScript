import com.sksamuel.scrimage.ImmutableImage
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
    println("${imageCropped.width} X ${imageCropped.height} | ${imageCropped.ratio()}")
    imageCropped.write("result1")

    println("Appending Quote")
    val quote = "LoreIpsome sample is added to image!"
    val awtImage: BufferedImage = imageCropped.awt()
    val (imageWidth, imageHeight) = 1080 to 1920
    val imageWithQuote = with(awtImage) {
        val graphics = graphics
        val font = Font(Font.MONOSPACED, Font.PLAIN, 46)
        val fontMetrics: FontMetrics = graphics.getFontMetrics(font)

        // text attribute
        val attributedText = AttributedString(quote)
        attributedText.addAttribute(TextAttribute.FONT, font)
        attributedText.addAttribute(TextAttribute.FOREGROUND, Color.GREEN)

        // text position - Centalized
        val positionX: Int = (imageWidth - fontMetrics.stringWidth(quote)) / 2
        val positionY: Int = ((imageHeight - fontMetrics.height) / 2) + fontMetrics.ascent

        // draw text
        graphics.drawString(attributedText.iterator, positionX, positionY)
        ImmutableImage.fromAwt(this)
    }
    imageWithQuote.write("result2")
    Unit
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
