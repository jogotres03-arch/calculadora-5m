# Calculadora 5M (Android)

Calculadora com **5 memórias independentes** (M1 a M5), em Kotlin, sem dependências além de AndroidX.

## Como abrir e rodar
1. Abra a pasta `Calculadora5M` no **Android Studio** (Koala ou mais recente).
2. Aguarde o *Gradle Sync* (ele baixa o Gradle 8.7 sozinho).
3. Conecte um celular com depuração USB ou use um emulador e clique em **Run ▶**.
   Para gerar o APK: *Build > Build Bundle(s) / APK(s) > Build APK(s)*.

## Como usar as memórias
- Toque em **M1…M5** para escolher qual memória está ativa (a ativa fica azul e mostra o valor guardado).
- **MS** guarda o valor atual na memória ativa (substitui).
- **M+** soma o valor atual à memória ativa; **M−** subtrai.
- **MR** insere o valor da memória ativa na expressão.
- **MC** limpa a memória ativa. **Segure MC** para limpar todas.
- Segurar um botão **M1…M5** seleciona e limpa aquela memória.
- As memórias ficam salvas mesmo fechando o app.

O "valor atual" é o resultado da expressão na tela (ex.: com `12+8` digitado, MS guarda 20).

## Arquivos principais
- `Evaluator.kt` – avaliador de expressões (+ − × ÷ % e parênteses).
- `MainActivity.kt` – interface (montada em código) e lógica das 5 memórias.
