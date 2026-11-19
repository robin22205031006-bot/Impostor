package com.example.data

enum class PlayerRole {
    NORMAL,
    IMPOSTOR
}

data class LocalPlayer(
    val id: String,
    val name: String,
    val score: Int = 0
)
