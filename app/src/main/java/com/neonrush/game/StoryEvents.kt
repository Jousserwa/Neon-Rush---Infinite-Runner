package com.neonrush.game

enum class StoryBeatType { OPENING, MID_RUN, BOSS_INTRO, ENDING }

data class StoryEvent(
    val world: World,
    val type: StoryBeatType,
    val text: String
)
