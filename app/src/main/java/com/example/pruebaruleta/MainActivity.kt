package com.example.pruebaruleta

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.pruebaruleta.FraseDatabase.*
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var  db: FraseDatabase
    private lateinit var fraseDao: FraseDao
    private lateinit var ruleta: ImageView
    private lateinit var btnGirar: Button
    private  var panel:MutableList<ImageView> = mutableListOf()
    private var letrasDichas = mutableListOf<Char>()
    private lateinit var editTextText: EditText
    private lateinit var button: Button
    private lateinit var textViewJ1: TextView
    private lateinit var textViewJ2: TextView
    private lateinit var textViewJ3: TextView
    private lateinit var textViewTurno: TextView
    private lateinit var btnResolver: Button
    private var jugador1 = ""
    private var jugador2 = ""
    private var jugador3 = ""
    private  var turnoDeJugador=1
    private var anguloResultado=0
    private var resultado=""
    private var ruletaGirada=false
    private lateinit var textviewPrueba: TextView
    private lateinit var frases: List<Frase>
    private lateinit var textviewprueba2: TextView
    private lateinit var frase: String
    private lateinit var fraseSinEspacios: String
    private  var longitudFrase=0
    private  var letrasLevantadas=0
    private var sectores = listOf(40,50,60,70,0,10,20,30,40,50,60,70,0,10,20,30)
    private var sectoresAngulos= IntArray(sectores.size)
    private var grados = 0
    private var mediaPlayer: MediaPlayer? = null
    private var idioma = Locale.getDefault().language



    override fun onCreate(savedInstanceState: Bundle?) {
        db = FraseDatabase.getDatabase(this)
        fraseDao= db.fraseDao()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ruleta = findViewById(R.id.ruleta)
        btnGirar = findViewById(R.id.btnGirar)
        editTextText = findViewById(R.id.editTextText)
        button = findViewById(R.id.button)
        textViewJ1 = findViewById(R.id.textViewJ1)
        textViewJ2 = findViewById(R.id.textViewJ2)
        textViewJ3 = findViewById(R.id.textViewJ3)
        textviewprueba2 = findViewById(R.id.textView4)
        btnResolver = findViewById(R.id.btnResolver)
        mediaPlayer = MediaPlayer.create(this, R.raw.musicafondo)
        mediaPlayer?.start()
        button.isEnabled = false
        obtenerGradosPorSectores()

        for (i in 0..31) {
            val id = resources.getIdentifier("hueco$i", "id", packageName)
            val imageView = findViewById<ImageView>(id)
            panel.add(imageView)
        }
        button.setOnClickListener {
            if(ruletaGirada){
                val letra = editTextText.text.toString().uppercase().firstOrNull()
                if (letra == null || letra == ' ') {
                    Toast.makeText(this, getString(R.string.letraValida), Toast.LENGTH_SHORT).show()
                } else {
                    verificarLetra(letra)
                    ruletaGirada = false
                    letrasDichas.add(letra)
                }

            } else{
                Toast.makeText(this, "La ruleta no se ha girado", Toast.LENGTH_SHORT).show()
            }

            editTextText.text.clear()

        }
        btnGirar.setOnClickListener {
            girarRuleta()
        }
         jugador1 = intent.getStringExtra("jugador1")?: ""
         jugador2 = intent.getStringExtra("jugador2")?:""
         jugador3 = intent.getStringExtra("jugador3")?:""
        if (jugador1 != null && jugador2 != null && jugador3 != null) {
            inicializarJugadores(jugador1, jugador2, jugador3)
        }
        seleccionarJugador(turnoDeJugador)
        CoroutineScope(Dispatchers.IO).launch {
            if (idioma == "es") {
                frases = fraseDao.obtenerFrasesSinPanelFinalEsp()
            } else if (idioma == "en") {
                frases = fraseDao.obtenerFrasesSinPanelFinalEng()
            } else{
                frases = fraseDao.obtenerFrasesSinPanelFinalEsp()
            }
            withContext(Dispatchers.Main) {
                if (frases.isNotEmpty()) {
                    val aleatorio = Random.nextInt(0, frases.size)
                    frase = frases[aleatorio].frase
                    fraseSinEspacios = frase.replace(" ", "")
                    longitudFrase = fraseSinEspacios.length
                    letrasLevantadas = 0
                    textviewprueba2.text = frases[aleatorio].categoria
                    verificarEspacios()
                } else {
                    Toast.makeText(this@MainActivity, "Error al obtener frases", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnResolver.setOnClickListener {
            resolverFrase()
        }

    }
    private fun girarRuleta() {
        btnGirar.isEnabled = false
        val random = Random.Default
        grados = random.nextInt(sectores.size-1)
        resultado=""
        val anguloFinal = (360* sectores.size) + sectoresAngulos[grados]
        val animador = ObjectAnimator.ofFloat(ruleta, "rotation", 0f, anguloFinal.toFloat())
        animador.duration = 5000
        animador.interpolator = DecelerateInterpolator()
        animador.start()
        animador.doOnEnd {
            ruletaGirada=true
            button.isEnabled = true
             anguloResultado = sectores[sectores.size-(grados +1)]
             resultado = obtenerResultado(anguloResultado)
            mostrarResultado(resultado)
        }
    }
    var jugadores = HashMap<String, Int>()
    private fun inicializarJugadores(jugador1: String, jugador2: String, jugador3: String) {
        jugadores[jugador1] = 0
        jugadores[jugador2] = 0
        jugadores[jugador3] = 0
        textViewJ1.text = jugador1 + ":  " + jugadores[jugador1]
        textViewJ2.text = jugador2 + ":  " + jugadores[jugador2]
        textViewJ3.text = jugador3 + ":  " + jugadores[jugador3]
    }
    private fun obtenerResultado(angulo: Int): String {
        return when {
           angulo == 0 -> "Paso"
            else -> angulo.toString()
        }
    }


    private fun seleccionarJugador(numero:Int):String{
        var jugador = ""
        when(numero){
            1->{
                textViewJ1.setBackgroundColor(Color.parseColor("#D67F45"))
                textViewJ1.setTextColor(Color.WHITE)
                textViewJ3.setBackgroundColor(Color.parseColor("#FAE3C6"))
                textViewJ3.setTextColor(Color.parseColor("#D67F45"))
                textViewJ3.setTypeface(null, android.graphics.Typeface.BOLD)
                jugador=jugador1
            }
            2->{
                textViewJ2.setBackgroundColor(Color.parseColor("#D67F45"))
                textViewJ2.setTextColor(Color.WHITE)
                textViewJ1.setBackgroundColor(Color.parseColor("#FAE3C6"))
                textViewJ1.setTextColor(Color.parseColor("#D67F45"))
                textViewJ1.setTypeface(null, android.graphics.Typeface.BOLD)
                jugador=jugador2
            }
            3->{
                textViewJ3.setBackgroundColor(Color.parseColor("#D67F45"))
                textViewJ3.setTextColor(Color.WHITE)
                textViewJ2.setBackgroundColor(Color.parseColor("#FAE3C6"))
                textViewJ2.setTextColor(Color.parseColor("#D67F45"))
                textViewJ2.setTypeface(null, android.graphics.Typeface.BOLD)
                jugador=jugador3
            }
        }
        return jugador
    }

    private fun mostrarResultado(resultado: String) {
        if(resultado=="Paso"){
            verificarLetra(' ')
        }else{
            Toast.makeText(this, getString(R.string.toastResultaado) + resultado + "!", Toast.LENGTH_SHORT).show()
        }

    }
    private fun verificarLetra(letra: Char) {
        if (letrasDichas.contains(letra)) {
            Toast.makeText(this, getString(R.string.letraRepetida) + letra, Toast.LENGTH_SHORT).show()
        }
        if(letra!=' ' && !letrasDichas.contains(letra)){
            var letraEncontrada = false
            for (i in frase.indices) {
                if (frase[i] == letra) {
                    letraEncontrada = true
                    letrasLevantadas++
                    val imageView = panel[i]
                    imageView.setImageResource(asignarImagenLetra(letra))
                    val valorActual = jugadores[seleccionarJugador(turnoDeJugador)] ?: 0
                    if(resultado!="Jackpot"){
                        var valorInt= resultado.toInt();
                        jugadores[seleccionarJugador(turnoDeJugador)] = valorActual + valorInt
                        textViewJ1.text = jugador1 + ":  " + jugadores[jugador1]
                        textViewJ2.text = jugador2 + ":  " + jugadores[jugador2]
                        textViewJ3.text = jugador3 + ":  " + jugadores[jugador3]
                    } else{
                        letraEncontrada=false
                    }
                }
            }
            if(letrasLevantadas>=longitudFrase){
                irARuletaFinal()
            }
            if (!letraEncontrada) {
                Toast.makeText(this, getString(R.string.toastNoEsta) + letra, Toast.LENGTH_SHORT).show()
                turnoDeJugador++
                if(turnoDeJugador>3){
                    turnoDeJugador=1
                }
                seleccionarJugador(turnoDeJugador)
            }
        } else{
            Toast.makeText(this, getString(R.string.pasoTurno), Toast.LENGTH_SHORT).show()
            turnoDeJugador++
            if(turnoDeJugador>3){
                turnoDeJugador=1
            }
            seleccionarJugador(turnoDeJugador)

        }
        btnGirar.isEnabled = true
        button.isEnabled = false

    }

    private fun asignarImagenLetra(letra: Char): Int {
        return when (letra) {
            'A' -> R.drawable.a
            'B' -> R.drawable.b
            'C' -> R.drawable.c
            'D' -> R.drawable.d
            'E' -> R.drawable.e
            'F' -> R.drawable.f
            'G' -> R.drawable.g
            'H' -> R.drawable.h
            'I' -> R.drawable.i
            'J' -> R.drawable.j
            'K' -> R.drawable.k
            'L' -> R.drawable.l
            'M' -> R.drawable.m
            'N' -> R.drawable.n
            'Ñ' -> R.drawable.nn
            'O' -> R.drawable.o
            'P' -> R.drawable.p
            'Q' -> R.drawable.q
            'R' -> R.drawable.r
            'S' -> R.drawable.s
            'T' -> R.drawable.t
            'U' -> R.drawable.u
            'V' -> R.drawable.v
            'W' -> R.drawable.w
            'X' -> R.drawable.x
            'Y' -> R.drawable.y
            'Z' -> R.drawable.z
            else -> R.drawable.cuadroblanco
        }
    }

    private fun verificarEspacios() {
        for (i in frase.indices) {
            if (frase[i] == ' ') {
                val imageView =panel[i]
                imageView.setImageResource(R.drawable.cuadroazul)
            }
        }
        if (frase.length < 32) {
            for (i in frase.length until 32) {
                val imageView = panel[i]
                imageView.setImageResource(R.drawable.cuadroazul)
            }
        }
    }
    private fun resolverFrase() {
        val editTextFrase = EditText(this)
        editTextFrase.gravity = android.view.Gravity.CENTER
        editTextFrase.setTextColor(Color.BLACK)
        editTextFrase.hint = getString(R.string.resolverHint)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Comprobar frase")
            .setMessage(getString(R.string.resolverMensaje))
            .setView(editTextFrase)
            .setPositiveButton(getString(R.string.resolverComprobar)) { _, _ ->
                val fraseIntroducida = editTextFrase.text.toString().uppercase()
                if (fraseIntroducida == frase) {
                    Toast.makeText(this, getString(R.string.resolverCorrecto), Toast.LENGTH_LONG).show()
                    irARuletaFinal()
                } else {
                    Toast.makeText(this, getString(R.string.resolverIncorrecto), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.resolverCancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }
    private fun irARuletaFinal(){
        mediaPlayer?.release()
        mediaPlayer = null
        val jugadorFinalEntry = jugadores.maxByOrNull { it.value }
        val jugadorFinal = jugadorFinalEntry?.key
        val puntosJugadorFinal = jugadorFinalEntry?.value
        val jugadoresRestantes = jugadores.toMutableMap().apply {
            if (jugadorFinal != null) {
                this.remove(jugadorFinal)
            }
        }
        val intent = Intent(this, RuletaFinal::class.java)
        if (jugadorFinal != null && puntosJugadorFinal != null) {
            intent.putExtra("jugadorFinal", jugadorFinal)
            intent.putExtra("puntosJugadorFinal", puntosJugadorFinal)
        }
        val nombresRestantes = jugadoresRestantes.keys.toList()
        val puntosRestantes = jugadoresRestantes.values.toList()
        intent.putStringArrayListExtra("nombresRestantes", ArrayList(nombresRestantes))
        intent.putIntegerArrayListExtra("puntosRestantes", ArrayList(puntosRestantes))
        intent.putExtra("jugadores", jugadores)
        startActivity(intent)
    }
    private fun obtenerGradosPorSectores(){
        val gradoSector = 360/sectores.size
        for(i in sectores.indices){
            sectoresAngulos[i]=(i+1) * gradoSector
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }


}
