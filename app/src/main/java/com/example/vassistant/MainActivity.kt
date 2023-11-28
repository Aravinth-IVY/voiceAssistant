package com.example.vassistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import com.example.vassistant.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private val RECORD_AUDIO_PERMISSION_CODE = 123

    private lateinit var speechRecognizer: SpeechRecognizer
    lateinit var textToSpeech: TextToSpeech

    private var productList:ArrayList<ProductBo> = arrayListOf(
    ProductBo(1, "miranda 250ml", 0, 10),
    ProductBo(2, "miranda 500ml", 0, 20),
    ProductBo(3, "miranda 1 liter", 0, 30),
    ProductBo(4, "miranda 2 liter", 0, 40),
    ProductBo(5, "slice 250ml", 0, 50),
    ProductBo(6, "slice 500ml", 0, 60),
    ProductBo(7, "slice 1 liter", 0, 70),
    ProductBo(8, "slice 2 liter", 0, 80),
    ProductBo(9, "fanta 250ml", 0, 90),
    ProductBo(10, "fanta 500ml", 0, 100),
    ProductBo(11, "fanta 1 liter", 0, 110),
    ProductBo(12, "fanta 2 liter", 0, 120)
    )

    var prodcount = 0
    var productName = "slice"
    var isSepaking = false
    //var bot:ArrayList<String> = ArrayList()
    private var rescount = 0
    private var result = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermission()


        binding.vbtn.setOnClickListener {

            speakAndListen()
            //speakAndListen("Product"+productList[prodcount].id+ " " + productList[prodcount].name + "and the quantity is "+productList[prodcount].piece +"peaces")

            //Thread.sleep(4000)

        }
    }

    private fun speakAndListen(){
        isSepaking=true
        speak("Product"+productList[prodcount].id+ " " + productList[prodcount].name + "and the quantity is "+productList[prodcount].piece +"peaces")

        Thread.sleep(4000)
        isSepaking=false
       // startListening()
    }

    private fun checkPermission() {
        if (isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
            initializeSpeechComponents()
        } else {
            requestPermission(Manifest.permission.RECORD_AUDIO, RECORD_AUDIO_PERMISSION_CODE)
        }
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSpeechComponents()
            } else {
                showMessage("Permission denied for recording audio")
            }
        }
    }

    private fun initializeSpeechComponents() {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
        // Any other initialization related to speech components
    }


    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(
            applicationContext
        ) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.getDefault()
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle) {
                    Log.d("MainActivity", "onReadyForSpeech")
                    binding.resultTextView.text = "listenning..."
                }

                override fun onBeginningOfSpeech() {
                    Log.d("MainActivity", " onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {
                }

                override fun onBufferReceived(buffer: ByteArray) {
                    Log.d("MainActivity", "onBufferReceived")
                }

                override fun onEndOfSpeech() {
                    Log.d("MainActivity", "onEndOfSpeech")
                }

                override fun onError(error: Int) {
                    Log.d("MainActivity", "onError")
                    val errorMessage: String = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Other client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network related error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server sends error status"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language Not supported"
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language Unavailabe."
                        else -> "Speech recognition error"
                    }
                    binding.resultTextView.text = "$errorMessage   $result"
                    showMessage(errorMessage)
                }

                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.size > 0) {
                        val text = matches[0]
                        Log.d("MainActivity", text)
                    }
                    showMessage("no Speech Recognized in onResult")

                }

                @SuppressLint("SuspiciousIndentation")
                override fun onPartialResults(partialResults: Bundle) {
                    Log.d("MainActivity","onPartialResults")
                    val partialResultsList =
                        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (partialResultsList != null && partialResultsList.size > 0 && !isSepaking) {
                        val partialResult = partialResultsList[0]
                        binding.partialResultTextView.text=partialResult
                            if( partialResult.isNotEmpty() && partialResult.contains("next product")){
                                prodcount++
                                speechRecognizer.stopListening()
                                Thread.sleep(4000)
                                speakAndListen()
                            }

                    }
                }

                override fun onEvent(eventType: Int, params: Bundle) {
                    Log.d("MainActivity", "onEvent")

                    // Reserved for future use
                }
            })
        }
    }



    private fun startListening() {
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            //intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,155000)
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,155000)
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,155000)
            speechRecognizer.startListening(intent)
        }
    }

    private fun speak(text: String) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    // Utterance started
                }

                override fun onDone(utteranceId: String) {
                    // Utterance completed
                    isSepaking=false
                    runOnUiThreads { startListening() }
                }

                override fun onError(utteranceId: String) {
                    // Utterance error
                }
            })
        }
    }

    fun runOnUiThreads(runnable: Runnable) {
        Handler(mainLooper).post(runnable)
    }

    private fun showMessage(message: String){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (speechRecognizer != null) {
            speechRecognizer.destroy()
        }
        if (textToSpeech != null) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}