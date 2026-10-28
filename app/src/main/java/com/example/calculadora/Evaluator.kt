package com.example.calculadora

import java.math.BigDecimal
import java.math.MathContext

/**
 * Avaliador de expressões aritméticas.
 * Suporta: + − × ÷ (também - * /), parênteses, % (pós-fixo = /100),
 * sinal unário e vírgula ou ponto como separador decimal.
 */
object Evaluator {

    class ErroCalculo(msg: String) : Exception(msg)

    fun avaliar(expressao: String): Double {
        val p = Parser(expressao.filter { !it.isWhitespace() })
        val v = p.parseTudo()
        if (v.isNaN() || v.isInfinite()) throw ErroCalculo("Resultado inválido")
        return v
    }

    /** Formata para exibição (vírgula decimal, até 12 dígitos significativos). */
    fun formatar(d: Double): String {
        if (d == 0.0) return "0"
        return BigDecimal(d)
            .round(MathContext(12))
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
    }

    private class Parser(private val s: String) {
        private var pos = 0

        fun parseTudo(): Double {
            if (s.isEmpty()) throw ErroCalculo("Vazio")
            val v = expressao()
            if (pos < s.length) throw ErroCalculo("Caractere inesperado")
            return v
        }

        private fun peek(): Char? = if (pos < s.length) s[pos] else null

        private fun expressao(): Double {
            var v = termo()
            while (true) {
                when (peek()) {
                    '+' -> { pos++; v += termo() }
                    '−', '-' -> { pos++; v -= termo() }
                    else -> return v
                }
            }
        }

        private fun termo(): Double {
            var v = fator()
            while (true) {
                when (peek()) {
                    '×', '*' -> { pos++; v *= fator() }
                    '÷', '/' -> {
                        pos++
                        val d = fator()
                        if (d == 0.0) throw ErroCalculo("Divisão por zero")
                        v /= d
                    }
                    else -> return v
                }
            }
        }

        private fun fator(): Double {
            var v = unario()
            while (peek() == '%') { pos++; v /= 100.0 }
            return v
        }

        private fun unario(): Double = when (peek()) {
            '−', '-' -> { pos++; -unario() }
            '+' -> { pos++; unario() }
            else -> primario()
        }

        private fun primario(): Double {
            if (peek() == '(') {
                pos++
                val v = expressao()
                if (peek() != ')') throw ErroCalculo("Parêntese não fechado")
                pos++
                return v
            }
            val ini = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == ',' || s[pos] == '.')) pos++
            if (ini == pos) throw ErroCalculo("Número esperado")
            return s.substring(ini, pos).replace(',', '.').toDoubleOrNull()
                ?: throw ErroCalculo("Número inválido")
        }
    }
}
