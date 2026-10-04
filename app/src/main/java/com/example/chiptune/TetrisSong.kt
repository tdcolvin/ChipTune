package com.example.chiptune

object TetrisSong {
    const val LOOP_DURATION_SECONDS = 10.67

    val tetrisBass = listOf(
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),

        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),

        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),

        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),

        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
    )

    val tetrisMelody = listOf(
        // Measure 1
        Note(NOTE_E5, 1.0f),
        Note(NOTE_B4, 0.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_B4, 0.5f),

        // Measure 2
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 0.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_E5, 1.0f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_C5, 0.5f),

        // Measure 3
        Note(NOTE_B4, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_E5, 1.0f),

        // Measure 4
        Note(NOTE_C5, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(REST, 1.0f),

        // Measure 5
        Note(REST, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_F5, 0.5f),
        Note(NOTE_A5, 1.0f),
        Note(NOTE_G5, 0.5f),
        Note(NOTE_F5, 0.5f),

        // Measure 6
        Note(NOTE_E5, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_E5, 1.0f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_C5, 0.5f),

        // Measure 7
        Note(NOTE_B4, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_E5, 1.0f),

        // Measure 8
        Note(NOTE_C5, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(REST, 1.0f),
    )
}
