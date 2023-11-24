package com.example.vassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.example.vassistant.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val RECORD_AUDIO_PERMISSION_CODE = 123
    private lateinit var speechRecognizer: SpeechRecognizer
    lateinit var textToSpeech: TextToSpeech
    private var rescount = 0
    private var bot = arrayListOf(
        "order your products",
        "order quantity ",
        "cases count",
        "pieces count",
        "do you like to add another product"
    )
    private var prodList = arrayListOf("pepsi", "coca-cola", "sprite", "slice", "miranda")
    private var quanList = arrayListOf("150 ml", "250 ml", "500 ml", "750 ml", "1 l", "2 l")
    private var result = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermission()
        binding.vbtn.setOnClickListener {
            if (rescount == 0) speak(bot[rescount])
            startListening()
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        } else {
            initializeSpeechRecognizer()
            initializeTextToSpeech()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize TextToSpeech and SpeechRecognizer
                initializeSpeechRecognizer()
                initializeTextToSpeech()
            } else {
                // Permission denied, handle accordingly
                // You might want to show a message to the user or disable the voice functionality
                showMessage("Permission denied for recording audio")
            }
        }
    }


    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(
            applicationContext
        ) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }
    }

    private fun startListening() {
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "say something"
            )
            speechRecognizer.startListening(intent)
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle) {
                    Log.d("MainActivity", "onReadyForSpeech")
                    binding.resultTextView.text = "listenning...   $result"
                }

                override fun onBeginningOfSpeech() {
                    Log.d("MainActivity", " onBeginningOfSpeech")
                    // Speech input has begun
                }

                override fun onRmsChanged(rmsdB: Float) {
//                    Commons.print("onRmsChanged rmsdB="+rmsdB);
                    // The RMS value changed
                }

                override fun onBufferReceived(buffer: ByteArray) {
                    Log.d("MainActivity", "onBufferReceived")

                    // Audio data received
                }

                override fun onEndOfSpeech() {
                    Log.d("MainActivity", "onEndOfSpeech")

                    // Speech input has ended
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
                        else -> "Speech recognition error"
                    }
                    binding.resultTextView.text = "$errorMessage   $result"
                    showMessage(errorMessage)
                }

                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.size > 0) {
                        val text = matches[0]
                        if (text.equals("yes", ignoreCase = true)) rescount = 0
                        showMessage("You said: $text")
                        Log.d("MainActivity", "you said: $text")
                        Log.d("MainActivity", "your result: $result")
                        if (text.equals("stop", ignoreCase = true) || text.equals(
                                "no",
                                ignoreCase = true
                            )
                        ) {
                            binding.resultTextView.text = "final result: $result"
                            Log.d("MainActivity", "final result: $result")
                        } else {
                            try {
                                updateList(text)
                            } catch (e: InterruptedException) {
                                throw RuntimeException(e)
                            }
                        }
                    } else {
                        showMessage("No speech recognized")
                    }
                }

                override fun onPartialResults(partialResults: Bundle) {
                    Log.d("MainActivity","onPartialResults")
                    val partialResultsList =
                        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (partialResultsList != null && partialResultsList.size > 0) {
                        val partialResult = partialResultsList[0]
                        binding.partialResultTextView.text=partialResult
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle) {
                    Log.d("MainActivity", "onEvent")

                    // Reserved for future use
                }
            })
        }
    }

    @Throws(InterruptedException::class)
    private fun updateList(text: String) {
        var flag = false
        if (rescount < 2) {
            for (product in prodList) {
                if (text.equals(product, ignoreCase = true)) {
                    result.add(text)
                    rescount += 1
                    flag = true
                    speak(bot.get(rescount))
                    Log.d("MainActivity", "$result count=$rescount")
                }
            }
            for (quantity in quanList) {
                if (text.equals(quantity, ignoreCase = true)) {
                    result.add(text)
                    rescount += 1
                    speak(bot.get(rescount))
                    flag = true
                    Log.d("MainActivity", "$result count=$rescount")
                }
            }
            if (!flag) speak("not available, try different")
        } else {
            result.add(text)
            rescount += 1
            speak(bot[rescount])
            Log.d("MainActivity", "$result count=$rescount")
        }
        binding.resultTextView.text = result.toString()
        binding.vbtn.performClick()
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
                }

                override fun onError(utteranceId: String) {
                    // Utterance error
                }
            })
        }
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