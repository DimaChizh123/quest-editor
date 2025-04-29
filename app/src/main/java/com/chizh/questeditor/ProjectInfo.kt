package com.chizh.questeditor

import kotlinx.serialization.Serializable

@Serializable
data class ProjectInfo(val title: String, val projectFile: String, val date: Long)