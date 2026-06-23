package com.example.zentap

// ──────────────────────────────────────────────────────────────────────────────
// InterventionData — the only file you need to touch to extend any category.
//
//  • Add a String to reflectionQuestions to add a new reflection prompt.
//  • Add a YouTubeVideo(...) to youtubeVideos to add a new video.
//  • Add a PhysicalNudge(...) to physicalNudges to add a new physical activity.
//
// The app cycles through all three categories in order (Reflect → Watch → Move)
// and advances to the next item within a category each full rotation.
// ──────────────────────────────────────────────────────────────────────────────

object InterventionData {

    // ── Reflection questions ─────────────────────────────────────────────────
    val reflectionQuestions: List<String> = listOf(
        "How are you actually feeling right now — not \"fine\", actually?",
        "What's one thing you're avoiding today?",
        "When did you last feel genuinely proud of yourself?",
        "What would you do with the next 30 minutes if Instagram didn't exist?",
        "What's been weighing on you most this week?",
        "Who haven't you reached out to in a while that you miss?",
        "What are you grateful for right now, in this exact moment?",
        "Are you scrolling to feel better, or because you're bored? Which one is it really?",
        "What does your body need right now — water, rest, or movement?",
        "What's one small thing you could do today that would make tomorrow easier?",
    )

    // ── YouTube / long-form videos ───────────────────────────────────────────
    data class YouTubeVideo(val title: String, val url: String)

    val youtubeVideos: List<YouTubeVideo> = listOf(
        YouTubeVideo(
            "Inside the Mind of a Master Procrastinator – Tim Urban",
            "https://www.youtube.com/watch?v=arj7oStGLkU"
        ),
        YouTubeVideo(
            "The Power of Vulnerability – Brené Brown",
            "https://www.youtube.com/watch?v=iCvmsMzlF7o"
        ),
        YouTubeVideo(
            "How to Make Stress Your Friend – Kelly McGonigal",
            "https://www.youtube.com/watch?v=RcGyVTAoXEU"
        ),
        YouTubeVideo(
            "The Surprising Science of Happiness – Dan Gilbert",
            "https://www.youtube.com/watch?v=4q1dgn_C0AU"
        ),
        YouTubeVideo(
            "How to Speak So That People Want to Listen – Julian Treasure",
            "https://www.youtube.com/watch?v=eIho2S0ZahI"
        ),
        YouTubeVideo(
            "Do Schools Kill Creativity? – Ken Robinson",
            "https://www.youtube.com/watch?v=iG9CE55wbtY"
        ),
        YouTubeVideo(
            "The Puzzle of Motivation – Dan Pink",
            "https://www.youtube.com/watch?v=rrkrvAUbU9Y"
        ),
        YouTubeVideo(
            "Your Body Language May Shape Who You Are – Amy Cuddy",
            "https://www.youtube.com/watch?v=Ks-_Mh1QhMc"
        ),
    )

    // ── Physical nudges ──────────────────────────────────────────────────────
    data class PhysicalNudge(val title: String, val instruction: String)

    val physicalNudges: List<PhysicalNudge> = listOf(
        PhysicalNudge(
            "Box Breathing",
            "Breathe IN for 4... hold for 4... OUT for 4... hold for 4.\nRepeat 4 times. Slow."
        ),
        PhysicalNudge(
            "Posture Reset",
            "Sit up straight. Roll your shoulders back. Unclench your jaw.\nTake one slow breath from there."
        ),
        PhysicalNudge(
            "Drink Water",
            "Go get a full glass of water and drink it all.\nYour scrolling thumb will survive the 60 seconds."
        ),
        PhysicalNudge(
            "Window Gaze",
            "Look out a window at something far away for 60 seconds.\nLet your eyes relax — no screens."
        ),
        PhysicalNudge(
            "Quick Walk",
            "Stand up. Walk to the furthest room and back — three times.\nThat's it. Go."
        ),
        PhysicalNudge(
            "Stretch It Out",
            "Stand up, reach both arms overhead, then slowly touch your toes.\nHold each for 5 seconds."
        ),
        PhysicalNudge(
            "Lock & Stare",
            "Lock the phone. Stare at a wall for 30 seconds.\nIf the urge passes, you didn't need it."
        ),
    )
}