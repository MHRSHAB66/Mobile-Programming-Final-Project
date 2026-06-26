package com.example.project.domain.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val followers: Int,
    val bio: String = "",
    val isFollowed: Boolean = false,
)
