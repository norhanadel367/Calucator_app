package com.example.calucator_app

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CalculatorViewModel : ViewModel() {
    private val _state = MutableStateFlow(CalculatorScreenState())
    val state = _state.asStateFlow()

    fun onNumberClicked(number: String) {
        _state.update {
            val current = it.currentNumber
            val newNumber = when {
                current == "Undefined" || current == "Infinity" -> number
                number == "." && current.contains(".") -> current
                current == "0" && number != "." -> number
                else -> current + number
            }
            it.copy(currentNumber = newNumber)
        }
    }

    fun onChangeSignClicked() {
        _state.update {
            val current = it.currentNumber
            if (current.isNotEmpty() && current != "0") {
                it.copy(currentNumber = if (current.startsWith("-")) current.drop(1) else "-$current")
            } else {
                it
            }
        }
    }

    fun onOperatorClicked(operator: Operator) {
        _state.update { currentState ->
            if (currentState.currentNumber.isEmpty() && currentState.numbers.isEmpty()) return@update currentState
            val lastChar = if (currentState.currentEquation.isNotEmpty()) currentState.currentEquation.last() else ' '
            val isLastCharOperator = lastChar in listOf('+', '-', 'x', '/', '%')

            if ((currentState.currentNumber.isNotEmpty() && currentState.currentNumber != "Undefined" && currentState.currentNumber != "Infinity") &&
                (!isLastCharOperator || currentState.currentEquation.isEmpty())) {
                currentState.copy(
                    currentEquation = currentState.currentEquation + currentState.currentNumber + operator.toEquation(),
                    numbers = currentState.numbers + currentState.currentNumber,
                    currentNumber = "",
                    operators = currentState.operators + operator
                )
            } else {
                currentState
            }
        }
    }

    fun clearEquation() {
        _state.update {
            it.copy(
                currentEquation = "",
                currentNumber = "0",
                operators = listOf(),
                numbers = listOf(),
                lastOperation = ""
            )
        }
    }

    fun deleteLast() {
        _state.update { currentState ->
            when {
                currentState.currentNumber.isNotEmpty() -> {
                    val newNumber = currentState.currentNumber.dropLast(1)
                    currentState.copy(currentNumber = if (newNumber.isEmpty()) "0" else newNumber)
                }
                currentState.operators.isNotEmpty() && currentState.numbers.isNotEmpty() -> {
                    val lastNumber = currentState.numbers.last()
                    val lastOperatorLength = currentState.operators.last().toEquation().length
                    val lastNumberLength = lastNumber.length
                    val charsToDrop = lastOperatorLength + lastNumberLength
                    currentState.copy(
                        currentEquation = currentState.currentEquation.dropLast(charsToDrop),
                        operators = currentState.operators.dropLast(1),
                        numbers = currentState.numbers.dropLast(1),
                        currentNumber = lastNumber
                    )
                }
                currentState.numbers.isNotEmpty() -> {
                    currentState.copy(
                        currentEquation = currentState.currentEquation.dropLast(currentState.numbers.last().length),
                        numbers = currentState.numbers.dropLast(1)
                    )
                }
                else -> currentState
            }
        }
    }

    fun onEqualButtonClicked() {
        val currentState = _state.value
        if (currentState.currentNumber.isEmpty() && currentState.numbers.isEmpty()) return

        var numbers = currentState.numbers + (if (currentState.currentNumber.isNotEmpty()) listOf(currentState.currentNumber) else listOf())
        var operators = currentState.operators

        if (operators.size >= numbers.size) {
            operators = operators.dropLast(1)
        }

        if (numbers.isEmpty()) {
            _state.update { it.copy(lastOperation = "0") }
            return
        }

        val result = calculateResult(numbers.map { it.toFloatOrNull() ?: 0f }, operators)
        _state.update {
            it.copy(
                lastOperation = "${currentState.currentEquation}${currentState.currentNumber}",
                currentEquation = "",
                currentNumber = result,
                numbers = listOf(),
                operators = listOf()
            )
        }
    }

    private fun calculateResult(numbers: List<Float>, operators: List<Operator>): String {
        if (numbers.isEmpty()) return "0"
        if (numbers.size == 1) {
            val resStr = numbers[0].toString()
            return if (resStr.endsWith(".0")) resStr.dropLast(2) else resStr
        }

        val numList = numbers.toMutableList()
        val opList = operators.toMutableList()

        var i = 0
        while (i < opList.size) {
            when (opList[i]) {
                Operator.MULTIPLY, Operator.DIVIDER, Operator.MOD -> {
                    val result = when (opList[i]) {
                        Operator.MULTIPLY -> numList[i] * numList[i + 1]
                        Operator.DIVIDER -> if (numList[i + 1] != 0f) numList[i] / numList[i + 1] else return "Undefined"
                        Operator.MOD -> (numList[i] * numList[i + 1]) / 100
                        else -> 0f
                    }
                    numList[i] = result
                    numList.removeAt(i + 1)
                    opList.removeAt(i)
                }
                else -> i++
            }
        }

        var res = numList[0]
        for (j in 0 until opList.size) {
            when (opList[j]) {
                Operator.ADDITION -> res += numList[j + 1]
                Operator.MINUS -> res -= numList[j + 1]
                else -> {}
            }
        }

        val resStr = res.toString()
        return if (resStr.endsWith(".0")) resStr.dropLast(2) else resStr
    }

    private fun Operator.toEquation(): String {
        return when (this) {
            Operator.DIVIDER -> " / "
            Operator.MULTIPLY -> " x "
            Operator.ADDITION -> " + "
            Operator.MOD -> " % "
            Operator.MINUS -> " - "
        }
    }
}

data class CalculatorScreenState(
    val operators: List<Operator> = listOf(),
    val currentEquation: String = "",
    val currentNumber: String = "0",
    val numbers: List<String> = listOf(),
    val lastOperation: String = ""
)

enum class Operator {
    DIVIDER,
    MULTIPLY,
    ADDITION,
    MOD,
    MINUS
}