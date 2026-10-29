package com.example.calculadora

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TOTAL_MEMORIAS = 5
        private const val PREFS = "calculadora_memorias"
        private const val OPERADORES = "+−×÷"

        private val COR_FUNDO = Color.parseColor("#12141A")
        private val COR_NUMERO = Color.parseColor("#262A35")
        private val COR_FUNCAO = Color.parseColor("#3A3F4F")
        private val COR_OPERADOR = Color.parseColor("#F29F05")
        private val COR_IGUAL = Color.parseColor("#2EAD6B")
        private val COR_MEM = Color.parseColor("#1D2230")
        private val COR_MEM_SEL = Color.parseColor("#3D5AFE")
        private val COR_MEM_OP = Color.parseColor("#2A3042")
        private val COR_MEM_PERIGO = Color.parseColor("#4A2430")
    }

    // Cinco memórias independentes. null = vazia.
    private val memorias = arrayOfNulls<Double>(TOTAL_MEMORIAS)
    private var selecionada = 0

    private var expr = ""
    private var acabouDeCalcular = false

    private lateinit var tvExpr: TextView
    private lateinit var tvResultado: TextView
    private val botoesMemoria = mutableListOf<TextView>()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        carregarMemorias()
        setContentView(construirTela())
        atualizarMemorias()
        atualizarDisplay()
    }

    // ---------------------------------------------------------------- UI

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun construirTela(): View {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COR_FUNDO)
            setPadding(dp(12), dp(8), dp(12), dp(12))
            fitsSystemWindows = true
        }

        // Display
        val display = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.END
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        tvExpr = TextView(this).apply {
            setTextColor(Color.parseColor("#9AA0B4"))
            textSize = 26f
            gravity = Gravity.END
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.START
        }
        tvResultado = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
        }
        display.addView(tvExpr, matchWrap())
        display.addView(tvResultado, matchWrap())
        raiz.addView(display, LinearLayout.LayoutParams(-1, 0, 1.3f))

        // Linha de seleção das 5 memórias
        val linhaSlots = linha(altura = dp(56))
        for (i in 0 until TOTAL_MEMORIAS) {
            val b = botao("", COR_MEM, Color.WHITE, 12f) { selecionarMemoria(i) }
            b.maxLines = 2
            b.ellipsize = TextUtils.TruncateAt.END
            b.setOnLongClickListener { selecionarMemoria(i); limparMemoria(); true }
            botoesMemoria.add(b)
            linhaSlots.addView(b, peso())
        }
        raiz.addView(linhaSlots, linhaParams(dp(56)))

        // Operações de memória (atuam na memória selecionada)
        val linhaOps = linha(altura = dp(44))
        listOf(
            "MC" to { limparMemoria() },
            "MR" to { recuperarMemoria() },
            "M+" to { somarMemoria(1) },
            "M−" to { somarMemoria(-1) },
            "MS" to { guardarMemoria() },
            "MCA" to { confirmarLimparTodas() }
        ).forEach { (rotulo, acao) ->
            val perigo = rotulo == "MCA"
            val b = botao(
                rotulo,
                if (perigo) COR_MEM_PERIGO else COR_MEM_OP,
                if (perigo) Color.parseColor("#FFB4B4") else Color.parseColor("#B7C0FF"),
                14f
            ) { acao() }
            if (rotulo == "MC") b.setOnLongClickListener { confirmarLimparTodas(); true }
            linhaOps.addView(b, peso())
        }
        raiz.addView(linhaOps, linhaParams(dp(44)))

        // Teclado
        val teclado = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val linhas = listOf(
            listOf("C", "⌫", "(", ")"),
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "−"),
            listOf("0", ",", "%", "+"),
        )
        for (l in linhas) {
            val row = linha(altura = 0)
            for (t in l) row.addView(botaoTeclado(t), peso())
            teclado.addView(row, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        // Última linha: x², √ e "=" (que ocupa metade da largura)
        val rowFinal = linha(altura = 0)
        rowFinal.addView(botaoTeclado("x²"), peso())
        rowFinal.addView(botaoTeclado("√"), peso())
        rowFinal.addView(botaoTeclado("="), peso().apply { weight = 2f })
        teclado.addView(rowFinal, LinearLayout.LayoutParams(-1, 0, 1f))

        raiz.addView(teclado, LinearLayout.LayoutParams(-1, 0, 4.2f).apply { topMargin = dp(6) })
        return raiz
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun peso() = LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
    private fun linhaParams(h: Int) = LinearLayout.LayoutParams(-1, h)

    private fun linha(altura: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    private fun botao(texto: String, fundo: Int, cor: Int, tamanho: Float, aoClicar: () -> Unit): TextView =
        TextView(this).apply {
            text = texto
            setTextColor(cor)
            textSize = tamanho
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isClickable = true
            isFocusable = true
            background = fundoArredondado(fundo)
            setOnClickListener { aoClicar() }
        }

    private fun fundoArredondado(cor: Int): RippleDrawable {
        val shape = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(cor)
        }
        return RippleDrawable(ColorStateList.valueOf(0x44FFFFFF), shape, null)
    }

    private fun botaoTeclado(t: String): TextView {
        val fundo = when {
            t == "=" -> COR_IGUAL
            t in listOf("÷", "×", "−", "+") -> COR_OPERADOR
            t.first().isDigit() || t == "," -> COR_NUMERO
            else -> COR_FUNCAO
        }
        return botao(t, fundo, Color.WHITE, 24f) { aoTocarTecla(t) }
    }

    // ---------------------------------------------------------- Teclado

    private fun aoTocarTecla(t: String) {
        when {
            t == "C" -> { expr = ""; acabouDeCalcular = false }
            t == "⌫" -> {
                if (acabouDeCalcular) { expr = ""; acabouDeCalcular = false }
                else expr = expr.dropLast(1)
            }
            t == "=" -> calcular()
            t.first().isDigit() -> digito(t)
            t == "," -> virgula()
            t in listOf("÷", "×", "−", "+") -> operador(t.first())
            t == "(" -> abreParenteses()
            t == ")" -> fechaParenteses()
            t == "%" -> porcento()
            t == "x²" -> quadrado()
            t == "√" -> raiz()
        }
        atualizarDisplay()
    }

    private fun comecarNovo() {
        if (acabouDeCalcular) { expr = ""; acabouDeCalcular = false }
    }

    private fun digito(d: String) {
        comecarNovo()
        if (expr.lastOrNull() == ')' || expr.lastOrNull() == '%') expr += "×"
        expr += d
    }

    private fun virgula() {
        comecarNovo()
        val numeroAtual = expr.takeLastWhile { it.isDigit() || it == ',' }
        if (numeroAtual.contains(',')) return
        expr += if (numeroAtual.isEmpty()) "0," else ","
    }

    private fun operador(op: Char) {
        acabouDeCalcular = false
        if (expr.isEmpty()) { if (op == '−') expr = "−"; return }
        val ultimo = expr.last()
        if (ultimo == '(') { if (op == '−') expr += op; return }
        if (ultimo in OPERADORES) {
            // troca o operador anterior (mantém o sinal unário se for só ele)
            if (expr.length == 1) { if (op != '−') expr = "" ; return }
            expr = expr.dropLast(1) + op
        } else expr += op
    }

    private fun abreParenteses() {
        comecarNovo()
        val u = expr.lastOrNull()
        if (u != null && (u.isDigit() || u == ')' || u == '%' || u == ',')) expr += "×"
        expr += "("
    }

    private fun fechaParenteses() {
        acabouDeCalcular = false
        val abertos = expr.count { it == '(' } - expr.count { it == ')' }
        val u = expr.lastOrNull() ?: return
        if (abertos > 0 && (u.isDigit() || u == ')' || u == '%')) expr += ")"
    }

    private fun porcento() {
        acabouDeCalcular = false
        val u = expr.lastOrNull() ?: return
        if (u.isDigit() || u == ')' || u == '%') expr += "%"
    }

    private fun calcular() {
        if (expr.isEmpty()) return
        val v = valorAtual() ?: return
        expr = Evaluator.formatar(v).replace('-', '−')
        acabouDeCalcular = true
    }

    /** Eleva ao quadrado o valor mostrado no display. */
    private fun quadrado() {
        val v = valorAtual() ?: return
        aplicarResultado(v * v)
    }

    /** Raiz quadrada do valor mostrado no display. */
    private fun raiz() {
        val v = valorAtual() ?: return
        if (v < 0) { aviso("Raiz de número negativo"); return }
        aplicarResultado(Math.sqrt(v))
    }

    private fun aplicarResultado(r: Double) {
        if (r.isNaN() || r.isInfinite()) { aviso("Resultado muito grande"); return }
        expr = Evaluator.formatar(r).replace('-', '−')
        acabouDeCalcular = true
    }

    // ------------------------------------------------------ Valor / display

    /** Fecha parênteses pendentes e remove operadores soltos no fim. */
    private fun preparar(e: String): String {
        var s = e
        while (s.isNotEmpty() && (s.last() in OPERADORES || s.last() == '(' || s.last() == ',')) s = s.dropLast(1)
        val abertos = s.count { it == '(' } - s.count { it == ')' }
        return s + ")".repeat(maxOf(abertos, 0))
    }

    private fun avaliarOuNull(e: String): Double? = try {
        Evaluator.avaliar(preparar(e))
    } catch (_: Evaluator.ErroCalculo) {
        null
    }

    /** Valor numérico da expressão atual; mostra aviso se não for possível. */
    private fun valorAtual(): Double? {
        if (expr.isEmpty()) { aviso("Digite um valor primeiro"); return null }
        val v = avaliarOuNull(expr)
        if (v == null) aviso("Expressão inválida")
        return v
    }

    private fun atualizarDisplay() {
        tvExpr.text = if (acabouDeCalcular) "" else expr
        tvResultado.text = when {
            expr.isEmpty() -> "0"
            acabouDeCalcular -> expr
            else -> avaliarOuNull(expr)?.let { Evaluator.formatar(it) } ?: ""
        }
        tvResultado.textSize = if (tvResultado.text.length > 12) 32f else 44f
    }

    private fun aviso(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ------------------------------------------------------------ Memórias

    private fun selecionarMemoria(i: Int) {
        selecionada = i
        atualizarMemorias()
    }

    private fun limparMemoria() {
        memorias[selecionada] = null
        salvarMemorias(); atualizarMemorias()
    }

    private fun confirmarLimparTodas() {
        if (memorias.all { it == null }) { aviso("As memórias já estão vazias"); return }
        AlertDialog.Builder(this)
            .setTitle("Apagar todas as memórias?")
            .setMessage("M1 a M5 serão esvaziadas. Isso não pode ser desfeito.")
            .setPositiveButton("Apagar tudo") { _, _ -> limparTodas() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun limparTodas() {
        memorias.fill(null)
        salvarMemorias(); atualizarMemorias()
        aviso("Todas as memórias foram apagadas")
    }

    private fun guardarMemoria() {
        val v = valorAtual() ?: return
        memorias[selecionada] = v
        salvarMemorias(); atualizarMemorias()
    }

    private fun somarMemoria(sinal: Int) {
        val v = valorAtual() ?: return
        memorias[selecionada] = (memorias[selecionada] ?: 0.0) + sinal * v
        salvarMemorias(); atualizarMemorias()
    }

    private fun recuperarMemoria() {
        val v = memorias[selecionada]
        if (v == null) { aviso("M${selecionada + 1} está vazia"); return }
        comecarNovo()
        // substitui o número que está sendo digitado, se houver
        val numero = expr.takeLastWhile { it.isDigit() || it == ',' }
        expr = expr.dropLast(numero.length)
        val u = expr.lastOrNull()
        if (u != null && (u.isDigit() || u == ')' || u == '%')) expr += "×"
        val txt = Evaluator.formatar(v).replace('-', '−')
        expr += if (v < 0) "($txt)" else txt
        atualizarDisplay()
    }

    private fun atualizarMemorias() {
        botoesMemoria.forEachIndexed { i, b ->
            val v = memorias[i]
            b.text = "M${i + 1}\n" + (v?.let { Evaluator.formatar(it) } ?: "—")
            b.background = fundoArredondado(if (i == selecionada) COR_MEM_SEL else COR_MEM)
            b.alpha = if (v == null && i != selecionada) 0.7f else 1f
        }
    }

    private fun salvarMemorias() {
        prefs.edit().apply {
            for (i in 0 until TOTAL_MEMORIAS) {
                val v = memorias[i]
                if (v == null) remove("m$i")
                else putLong("m$i", java.lang.Double.doubleToRawLongBits(v))
            }
            putInt("sel", selecionada)
        }.apply()
    }

    private fun carregarMemorias() {
        for (i in 0 until TOTAL_MEMORIAS) {
            if (prefs.contains("m$i"))
                memorias[i] = java.lang.Double.longBitsToDouble(prefs.getLong("m$i", 0L))
        }
        selecionada = prefs.getInt("sel", 0).coerceIn(0, TOTAL_MEMORIAS - 1)
    }

    override fun onPause() {
        super.onPause()
        salvarMemorias()
    }
}
