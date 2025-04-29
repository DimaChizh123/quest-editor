package com.chizh.questeditor

import androidx.lifecycle.ViewModel

class ProjectViewModel : ViewModel() {
    val questScreens = mutableListOf<QuestScreen>()
    var currentIndex = 0
}