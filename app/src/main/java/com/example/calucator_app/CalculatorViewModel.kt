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
        if (_state.value.currentNumber.isEmpty() && _state.value.numbers.isEmpty()) return
        if (_state.value.operators.size == _state.value.numbers.size) {
            _state.update {
                it.copy(
                    currentEquation = it.currentEquation + it.currentNumber + operator.toEquation(),
                    numbers = it.numbers + it.currentNumber,
                    currentNumber = "",
                    operators = it.operators + operator
                )
            }
        }
    }

    fun clearEquation() {
        _state.update {
            it.copy(
                currentEquation = "",
                currentNumber = "",
                operators = listOf(),
                numbers = listOf(),
                lastOperation = ""
            )
        }
    }

    fun deleteLast() {
        _state.update { currentState ->
            when {
                // Case 1: If currentNumber is not empty, remove its last character
                currentState.currentNumber.isNotEmpty() -> {
                    currentState.copy(currentNumber = currentState.currentNumber.dropLast(1))
                }
                // Case 2: If there are operators and numbers, remove the last operator but restore the last number as currentNumber
                currentState.operators.isNotEmpty() && currentState.numbers.isNotEmpty() -> {
                    val lastNumber = currentState.numbers.last()
                    val lastOperatorLength = currentState.operators.last().toEquation().length
                    val lastNumberLength = lastNumber.length
                    val charsToDrop = lastOperatorLength + lastNumberLength
                    currentState.copy(
                        currentEquation = currentState.currentEquation.dropLast(charsToDrop),
                        operators = currentState.operators.dropLast(1),
                        numbers = currentState.numbers.dropLast(1),
                        currentNumber = lastNumber  // Restore the last number for editing
                    )
                }
                // Case 3: If only numbers exist (e.g., after an equal operation), clear the last number
                currentState.numbers.isNotEmpty() -> {
                    currentState.copy(
                        currentEquation = currentState.currentEquation.dropLast(currentState.numbers.last().length),
                        numbers = currentState.numbers.dropLast(1)
                    )
                }
                // Case 4: If everything is empty, return the current state
                else -> currentState
            }
        }
    }
    fun onEqualButtonClicked() {
        val currentState = _state.value
        if (currentState.currentNumber.isEmpty() && currentState.numbers.isEmpty()) return

        // اجمع الأرقام
        var numbers = currentState.numbers + (if (currentState.currentNumber.isNotEmpty()) listOf(currentState.currentNumber) else listOf())
        var operators = currentState.operators

        // لو الأوبريتور أكتر من الأرقام → احذف آخر Operator
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
                        Operator.MOD -> numList[i] % numList[i + 1]
                        else -> 0f // Unreachable
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
                else -> {} // Already handled
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
    val currentNumber: String = "",
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