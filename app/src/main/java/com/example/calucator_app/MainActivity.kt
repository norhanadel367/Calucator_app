package com.example.calucator_app

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.calucator_app.databinding.ActivityMainBinding

import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.TRANSPARENT

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.isAppearanceLightStatusBars = true

        setUpData()
        setUpClickListeners()
    }

    private fun setUpData() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.result.text = state.lastOperation
                binding.viewText.text = if (state.currentNumber.isEmpty() &&
                    state.lastOperation.isNotEmpty()) state.currentEquation else state.currentEquation + state.currentNumber
            }
        }
    }

    private fun setUpClickListeners() {
        binding.btnAC.setOnClickListener { viewModel.clearEquation() }
        binding.btnBack.setOnClickListener { viewModel.deleteLast() }
        binding.btnEqual.setOnClickListener { viewModel.onEqualButtonClicked() }
        binding.btnPlusMinus.setOnClickListener { viewModel.onChangeSignClicked() }

        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9,
            binding.btnPoint
        )
        numberButtons.forEach { button ->
            button.setOnClickListener { onClickNum(button) }
        }

        val operatorButtons = listOf(
            binding.btnPlus, binding.btnMinus, binding.btnMultiply, binding.btnDivide, binding.btnPercent
        )
        operatorButtons.forEach { button ->
            button.setOnClickListener { onClickOperator(button) }
        }
    }

    private fun onClickNum(button: TextView) {
        val number = button.text.toString()
        viewModel.onNumberClicked(number)
    }

    private fun onClickOperator(button: TextView) {
        val op = when (button.text.toString()) {
            "x" -> Operator.MULTIPLY
            "-" -> Operator.MINUS
            "/" -> Operator.DIVIDER
            "%" -> Operator.MOD
            "+" -> Operator.ADDITION
            else -> Operator.MULTIPLY
        }
        viewModel.onOperatorClicked(op)
    }
}

