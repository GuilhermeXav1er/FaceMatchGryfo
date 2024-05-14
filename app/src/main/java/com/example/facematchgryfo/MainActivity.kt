package com.example.facematchgryfo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.facematchgryfo.databinding.ActivityMainBinding
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import android.util.Base64
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.ImageCaptureConfig
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import com.github.kittinunf.fuel.Fuel
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.kittinunf.fuel.coroutines.awaitString


class MainActivity : AppCompatActivity() {

    private val mainBinding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val CAMERA_PERMISSION_CODE = 100
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            // Permission has already been granted
            openCamera()
        }

        mainBinding.captureImageButton.setOnClickListener{
            takePhotos()
        }
    }

    private fun sendImagesToServer(image1Base64: String, image2Base64: String) {
        val url = "https://api.gryfo.com.br/face_match"
        val body = """
        {
            "document_img": "$image1Base64",
            "face_img": "$image2Base64"
        }
    """.trimIndent()

        val partnerId = "DesafioEstag"
        val apiKey = "9sndf96soADfhnJSgnsJDFiufgnn9suvn498gBN9nfsDesafioEstag"

        // Executa a operação de rede em uma coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = Fuel.post(url)
                    .body(body)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "$partnerId:$apiKey")
                    .awaitString()

                // Processa a resposta na thread principal
                withContext(Dispatchers.Main) {
                    val responseObject = JSONObject(response)
                    val responseCode = responseObject.optInt("response_code")

                    when (responseCode) {
                        100 -> {
                            // Sucesso: Mostro a mensagem do servidor
                            val message = responseObject.optString("message")
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Outros códigos de resposta: Mostro uma msg genérica de erro
                            val errorMessage = "Ocorreu um erro na requisição. Por favor, tente novamente mais tarde."
                            Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                // Erro na requisição
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erro ao enviar imagens: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with opening camera
                openCamera()
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "Permissao para camera negada.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openCamera() {
        // Code to open the camera
        Toast.makeText(this, "permissao garantida", Toast.LENGTH_SHORT).show()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUserCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        }
        else {
            AspectRatio.RATIO_16_9
        }
    }
    private fun bindCameraUserCases() {
        val screenAspectRadio = aspectRatio(
            mainBinding.previewView.width,
            mainBinding.previewView.height
        )
        val rotation = mainBinding.previewView.display.rotation


        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    screenAspectRadio,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            ).build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(mainBinding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private val capturedImages = mutableListOf<String>()
    private var firstImageCaptured = false

    private fun takePhotos() {
        if (!firstImageCaptured) {
            // Capturar a primeira foto
            capturePhoto { image1Base64 ->
                capturedImages.add(image1Base64)
                firstImageCaptured = true
            }
        } else {
            // Capturar a segunda foto
            capturePhoto { image2Base64 ->
                capturedImages.add(image2Base64)
                // Verificar se ambas as fotos foram capturadas
                if (capturedImages.size == 2) {
                    // Se sim, enviar as imagens para o servidor
                    sendImagesToServer(capturedImages[0], capturedImages[1])
                    // Limpar a lista de fotos capturadas
                    capturedImages.clear()
                    // Permitir a captura de mais fotos
                    firstImageCaptured = false
                } else {
                    // Trate o caso em que o número de imagens capturadas não é o esperado
                    Toast.makeText(this@MainActivity, "Erro ao capturar imagens", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun capturePhoto(onCaptured: (String) -> Unit) {
        // Captura a imagem
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // Converte a imagem para Base64 e chama a função de retorno
                val base64Image = image.toBase64Resized()
                onCaptured(base64Image)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(
                    this@MainActivity,
                    exception.message.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun ImageProxy.toBase64Resized(): String {
        // Converte os dados da imagem em um array de bytes
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)

        // Converte os bytes em um Bitmap
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Redimensiona o Bitmap para uma resolução menor
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)

        // Converte o Bitmap para Base64
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // Reduz a qualidade para 50%
        val bytesResized = outputStream.toByteArray()
        return Base64.encodeToString(bytesResized, Base64.DEFAULT)
    }

}