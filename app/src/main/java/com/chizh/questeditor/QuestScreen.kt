package com.chizh.questeditor

import kotlinx.serialization.Serializable

@Serializable
data class Project(val screens: List<QuestScreen>) //сам проект, хранящий список своих экранов

@Serializable
data class QuestScreen(
    var screenText: String = "",
    var correctAnswer: String = "",
    var primaryMediaType: MediaType? = null,
    var primaryMediaContent: String? = null,
    var secondaryMediaType: MediaType? = null,
    var secondaryMediaContent: String? = null)

@Serializable
enum class MediaType { TEXT, IMAGE }

enum class Slot { PRIMARY, SECONDARY }