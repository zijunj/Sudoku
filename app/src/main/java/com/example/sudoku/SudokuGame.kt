package com.example.sudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SudokuGame() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var grid by remember { mutableStateOf(generateSudokuGrid()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var inputText by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title and Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Sudoku", fontSize = 24.sp, modifier = Modifier.weight(1f))

            Button(onClick = { grid = generateSudokuGrid() }) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sudoku Grid with Borders
        LazyVerticalGrid(
            columns = GridCells.Fixed(9),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(grid.flatten().withIndex().toList()) { (index, number) ->
                val row = index / 9
                val col = index % 9

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, Color.Black) // Default thin border
                        .background(if (row == 0) Color.LightGray else Color.White)
                        .clickable {
                            if (row != 0) {
                                selectedCell = row to col
                                showDialog = true
                            }
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (number != 0) number.toString() else "",
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                }
            }
        }

        // Input Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedCell?.let { (row, col) ->
                            val enteredNumber = inputText.text.toIntOrNull() ?: 0

                            if (enteredNumber !in 1..9) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Invalid input! Enter numbers 1-9.")
                                }
                            } else if (!isMoveValid(grid, row, col, enteredNumber)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Invalid move! Rule violation.")
                                }
                            } else {
                                val newGrid = grid.toMutableList()
                                val updatedRow = newGrid[row].toMutableList()
                                updatedRow[col] = enteredNumber
                                newGrid[row] = updatedRow
                                grid = newGrid
                                showDialog = false

                                // Check if Sudoku is complete
                                if (isSudokuComplete(grid)) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("You won!")
                                    }
                                }
                            }
                        }
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Enter Number (1-9)") },
                text = {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        singleLine = true
                    )
                }
            )
        }

        // Snackbar for Errors and Winning Message
        SnackbarHost(hostState = snackbarHostState)
    }
}

/**
 * Checks if a move is valid before inserting the number.
 */
fun isMoveValid(grid: List<List<Int>>, row: Int, col: Int, num: Int): Boolean {
    return isValidRow(grid, row, num) &&
            isValidColumn(grid, col, num) &&
            isValidBox(grid, row, col, num)
}

/**
 * Generates a 9x9 Sudoku grid with the first row randomized.
 */
fun generateSudokuGrid(): List<List<Int>> {
    val firstRow = (1..9).shuffled()
    return List(9) { row ->
        if (row == 0) firstRow else List(9) { 0 }
    }
}

/**
 * Checks if Sudoku is complete.
 */
fun isSudokuComplete(grid: List<List<Int>>): Boolean {
    return (0..8).all { i ->
        isRowComplete(grid, i) && isColumnComplete(grid, i) && isBoxComplete(grid, i)
    }
}


/**
 * Checks if a number can be placed in a row without duplicates.
 */
fun isValidRow(grid: List<List<Int>>, row: Int, num: Int): Boolean {
    return !grid[row].contains(num)
}

/**
 * Checks if a number can be placed in a column without duplicates.
 */
fun isValidColumn(grid: List<List<Int>>, col: Int, num: Int): Boolean {
    return grid.none { it[col] == num }
}

/**
 * Checks if a number can be placed in a 3x3 box without duplicates.
 */
fun isValidBox(grid: List<List<Int>>, row: Int, col: Int, num: Int): Boolean {
    val startRow = (row / 3) * 3
    val startCol = (col / 3) * 3

    for (r in startRow until startRow + 3) {
        for (c in startCol until startCol + 3) {
            if (grid[r][c] == num) return false
        }
    }
    return true
}

/**
 * Checks if a row contains all numbers from 1-9 exactly once.
 */
fun isRowComplete(grid: List<List<Int>>, row: Int): Boolean {
    val nums = grid[row].filter { it in 1..9 }
    return nums.toSet().size == 9 // Ensures all numbers appear exactly once
}

/**
 * Checks if a column contains all numbers from 1-9 exactly once.
 */
fun isColumnComplete(grid: List<List<Int>>, col: Int): Boolean {
    val nums = grid.map { it[col] }.filter { it in 1..9 }
    return nums.toSet().size == 9 // Ensures all numbers appear exactly once
}

/**
 * Checks if a 3x3 box contains all numbers from 1-9 exactly once.
 */
fun isBoxComplete(grid: List<List<Int>>, box: Int): Boolean {
    val startRow = (box / 3) * 3
    val startCol = (box % 3) * 3
    val nums = mutableListOf<Int>()

    for (r in startRow until startRow + 3) {
        for (c in startCol until startCol + 3) {
            if (grid[r][c] in 1..9) nums.add(grid[r][c])
        }
    }
    return nums.toSet().size == 9 // Ensures all numbers appear exactly once
}

