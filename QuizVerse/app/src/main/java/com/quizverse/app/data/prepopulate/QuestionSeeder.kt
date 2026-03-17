package com.quizverse.app.data.prepopulate

import com.quizverse.app.data.database.QuizDatabase
import com.quizverse.app.data.database.entities.Achievement
import com.quizverse.app.data.database.entities.Category
import com.quizverse.app.data.database.entities.UserProgress
import com.quizverse.app.data.prepopulate.questions.animalQuestions
import com.quizverse.app.data.prepopulate.questions.filmQuestions
import com.quizverse.app.data.prepopulate.questions.foodQuestions
import com.quizverse.app.data.prepopulate.questions.geoQuestions
import com.quizverse.app.data.prepopulate.questions.geoQuestionsEasy
import com.quizverse.app.data.prepopulate.questions.geoQuestionsMedium
import com.quizverse.app.data.prepopulate.questions.geoQuestionsHard
import com.quizverse.app.data.prepopulate.questions.geoQuestionsExpert
import com.quizverse.app.data.prepopulate.questions.geoQuestionsMaster
import com.quizverse.app.data.prepopulate.questions.historyQuestions
import com.quizverse.app.data.prepopulate.questions.literatureQuestions
import com.quizverse.app.data.prepopulate.questions.logicQuestions
import com.quizverse.app.data.prepopulate.questions.mixedQuestions
import com.quizverse.app.data.prepopulate.questions.musicQuestions
import com.quizverse.app.data.prepopulate.questions.scienceQuestions
import com.quizverse.app.data.prepopulate.questions.sportQuestions
import com.quizverse.app.data.prepopulate.questions.techQuestions
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestions
import com.quizverse.app.data.prepopulate.questions.dortmundQuestions

/**
 * Seeds the database with initial data (categories, questions, achievements, user progress).
 * Called exactly once from [QuizDatabase.DatabaseCallback.onCreate].
 */
object QuestionSeeder {

    suspend fun seedDatabase(database: QuizDatabase) {
        insertCategories(database)
        insertQuestions(database)
        insertAchievements(database)
        insertInitialProgress(database)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Categories
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun insertCategories(database: QuizDatabase) {
        // Build list of all 12 categories; questionCount reflects seeded data (50 each).
        val categories = listOf(
            Category(id = 1,  name = "Weltgeographie",      iconName = "\uD83C\uDF0D", gradientStartColor = 0xFF4FACFEL, gradientEndColor = 0xFF00F2FEL, questionCount = 1424, isLocked = false),
            Category(id = 2,  name = "Wissenschaft & Natur",iconName = "\uD83D\uDD2C", gradientStartColor = 0xFF6C63FFL, gradientEndColor = 0xFFA855F7L, questionCount = 50, isLocked = false),
            Category(id = 3,  name = "Geschichte",          iconName = "\uD83D\uDCDC", gradientStartColor = 0xFFF97316L, gradientEndColor = 0xFFFBBF24L, questionCount = 50, isLocked = false),
            Category(id = 4,  name = "Film & Fernsehen",    iconName = "\uD83C\uDFAC", gradientStartColor = 0xFFEC4899L, gradientEndColor = 0xFFF43F5EL, questionCount = 50, isLocked = false),
            Category(id = 5,  name = "Musik",               iconName = "\uD83C\uDFB5", gradientStartColor = 0xFF8B5CF6L, gradientEndColor = 0xFFC084FCL, questionCount = 50, isLocked = false),
            Category(id = 6,  name = "Sport",               iconName = "\u26BD",       gradientStartColor = 0xFF22C55EL, gradientEndColor = 0xFF10B981L, questionCount = 50, isLocked = false),
            Category(id = 7,  name = "Technologie",         iconName = "\uD83D\uDCBB", gradientStartColor = 0xFF3B82F6L, gradientEndColor = 0xFF6366F1L, questionCount = 50, isLocked = false),
            Category(id = 8,  name = "Essen & Trinken",     iconName = "\uD83C\uDF73", gradientStartColor = 0xFFF59E0BL, gradientEndColor = 0xFFEF4444L, questionCount = 50, isLocked = false),
            Category(id = 9,  name = "Tierwelt",            iconName = "\uD83D\uDC3E", gradientStartColor = 0xFF14B8A6L, gradientEndColor = 0xFF06B6D4L, questionCount = 50, isLocked = false),
            Category(id = 10, name = "Sprache & Literatur", iconName = "\uD83D\uDCDA", gradientStartColor = 0xFF8B5CF6L, gradientEndColor = 0xFFEC4899L, questionCount = 50, isLocked = false),
            Category(id = 11, name = "Alle Kategorien",      iconName = "\uD83C\uDF1F", gradientStartColor = 0xFFF43F5EL, gradientEndColor = 0xFFF97316L, questionCount = 50, isLocked = false),
            Category(id = 12, name = "Logik & Denksport",   iconName = "\uD83E\uDDE0", gradientStartColor = 0xFF0EA5E9L, gradientEndColor = 0xFF6366F1L, questionCount = 50, isLocked = false),
            // ── Bundesliga-Vereine (Unterkategorien von Sport) ──────────
            Category(id = 13, name = "Hertha BSC",          iconName = "\u26BD",       gradientStartColor = 0xFF1A5276L, gradientEndColor = 0xFF2E86C1L, questionCount = 50, isLocked = false),
            Category(id = 14, name = "Borussia Dortmund",   iconName = "\u26BD",       gradientStartColor = 0xFFFDD835L, gradientEndColor = 0xFF212121L, questionCount = 50, isLocked = false),
        )

        // Insert categories using raw SQLite through the support DB to avoid a missing CategoryDao.
        categories.forEach { cat ->
            database.openHelper.writableDatabase.execSQL(
                """INSERT OR REPLACE INTO categories
                   (id, name, iconName, gradientStartColor, gradientEndColor, questionCount, isLocked)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                arrayOf(cat.id, cat.name, cat.iconName, cat.gradientStartColor, cat.gradientEndColor, cat.questionCount, if (cat.isLocked) 1 else 0)
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Questions  (50 per category, 600 total)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun insertQuestions(database: QuizDatabase) {
        val dao = database.questionDao()
        val all = geoQuestions() +
            geoQuestionsEasy() +
            geoQuestionsMedium() +
            geoQuestionsHard() +
            geoQuestionsExpert() +
            geoQuestionsMaster() +
            scienceQuestions() +
            historyQuestions() +
            filmQuestions() +
            musicQuestions() +
            sportQuestions() +
            techQuestions() +
            foodQuestions() +
            animalQuestions() +
            literatureQuestions() +
            mixedQuestions() +
            logicQuestions() +
            herthaBscQuestions() +
            dortmundQuestions()
        dao.insertAll(all)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Achievements (40+ unique milestones across multiple categories)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun insertAchievements(database: QuizDatabase) {
        val dao = database.achievementDao()
        val achievements = listOf(
            // ── Erste Schritte ─────────────────────────────────────────────
            Achievement(id = "first_quiz",       nameKey = "Erster Start",          descriptionKey = "Schließe dein erstes Quiz ab",                  iconName = "\uD83C\uDFAF", requiredValue = 1,   tier = 1),
            Achievement(id = "first_correct",    nameKey = "Erste Antwort",         descriptionKey = "Beantworte deine erste Frage richtig",           iconName = "\u2705",       requiredValue = 1,   tier = 1),
            Achievement(id = "first_perfect",    nameKey = "Fehlerlos",             descriptionKey = "Beende ein Quiz ohne Fehler",                    iconName = "\u2B50",       requiredValue = 1,   tier = 2),
            Achievement(id = "quiz_5",           nameKey = "Aufgewärmt",            descriptionKey = "Schließe 5 Quizzes ab",                          iconName = "\uD83D\uDD25", requiredValue = 5,   tier = 1),
            Achievement(id = "quiz_25",          nameKey = "Quiz-Enthusiast",       descriptionKey = "Schließe 25 Quizzes ab",                         iconName = "\uD83D\uDCCB", requiredValue = 25,  tier = 2),
            Achievement(id = "quiz_100",         nameKey = "Quiz-Profi",            descriptionKey = "Schließe 100 Quizzes ab",                        iconName = "\uD83C\uDFC6", requiredValue = 100, tier = 3),
            Achievement(id = "quiz_500",         nameKey = "Quiz-Legende",          descriptionKey = "Schließe 500 Quizzes ab",                        iconName = "\uD83D\uDC51", requiredValue = 500, tier = 3),

            // ── Streak-Achievements ────────────────────────────────────────
            Achievement(id = "streak_3",         nameKey = "3er-Strähne",           descriptionKey = "Erreiche einen Streak von 3",                    iconName = "\uD83D\uDD25", requiredValue = 3,   tier = 1),
            Achievement(id = "streak_7",         nameKey = "Eine Woche Feuer",      descriptionKey = "Erreiche einen Streak von 7",                    iconName = "\uD83D\uDD25", requiredValue = 7,   tier = 1),
            Achievement(id = "streak_10",        nameKey = "Flamme",                descriptionKey = "Erreiche einen Streak von 10",                   iconName = "\uD83D\uDD25", requiredValue = 10,  tier = 2),
            Achievement(id = "streak_25",        nameKey = "Inferno",               descriptionKey = "Erreiche einen Streak von 25",                   iconName = "\uD83C\uDF0B", requiredValue = 25,  tier = 2),
            Achievement(id = "streak_50",        nameKey = "Feuersturm",            descriptionKey = "Erreiche einen Streak von 50",                   iconName = "\uD83C\uDF2A", requiredValue = 50,  tier = 3),
            Achievement(id = "streak_100",       nameKey = "Ewige Flamme",          descriptionKey = "Erreiche einen Streak von 100",                  iconName = "\u2600",       requiredValue = 100, tier = 3),

            // ── Geschwindigkeit ────────────────────────────────────────────
            Achievement(id = "speed_demon",      nameKey = "Blitzschnell",          descriptionKey = "Beantworte 5 Fragen in unter 3 Sekunden",        iconName = "\u26A1",       requiredValue = 5,   tier = 2),
            Achievement(id = "speed_master",     nameKey = "Gedanken-Blitz",        descriptionKey = "Beantworte 20 Fragen in unter 3 Sekunden",       iconName = "\uD83C\uDF29", requiredValue = 20,  tier = 3),
            Achievement(id = "quick_10",         nameKey = "Schnelldenker",         descriptionKey = "Beende ein 10-Fragen-Quiz in unter 60 Sekunden", iconName = "\u23F1",       requiredValue = 1,   tier = 2),

            // ── Richtige Antworten ─────────────────────────────────────────
            Achievement(id = "correct_10",       nameKey = "Wissenshungrig",        descriptionKey = "Beantworte 10 Fragen richtig",                   iconName = "\uD83D\uDCDA", requiredValue = 10,  tier = 1),
            Achievement(id = "correct_50",       nameKey = "Wissbegierig",          descriptionKey = "Beantworte 50 Fragen richtig",                   iconName = "\uD83D\uDCD6", requiredValue = 50,  tier = 1),
            Achievement(id = "correct_100",      nameKey = "Experte",               descriptionKey = "Beantworte 100 Fragen richtig",                  iconName = "\uD83C\uDF93", requiredValue = 100, tier = 2),
            Achievement(id = "correct_500",      nameKey = "Meister",               descriptionKey = "Beantworte 500 Fragen richtig",                  iconName = "\uD83E\uDDD9", requiredValue = 500, tier = 2),
            Achievement(id = "marathon_1000",    nameKey = "Marathon",              descriptionKey = "Beantworte 1000 Fragen gesamt",                  iconName = "\uD83C\uDFC3", requiredValue = 1000, tier = 3),
            Achievement(id = "marathon_100",     nameKey = "Ausdauerläufer",        descriptionKey = "Beantworte 100 Fragen in einer Session",         iconName = "\uD83D\uDCAA", requiredValue = 100, tier = 2),

            // ── XP & Level ────────────────────────────────────────────────
            Achievement(id = "level_5",          nameKey = "Aufsteiger",            descriptionKey = "Erreiche Level 5",                               iconName = "\uD83D\uDCC8", requiredValue = 5,   tier = 1),
            Achievement(id = "level_10",         nameKey = "Fortgeschrittener",     descriptionKey = "Erreiche Level 10",                              iconName = "\uD83D\uDCCA", requiredValue = 10,  tier = 2),
            Achievement(id = "level_25",         nameKey = "Veteran",               descriptionKey = "Erreiche Level 25",                              iconName = "\uD83E\uDD48", requiredValue = 25,  tier = 2),
            Achievement(id = "level_50",         nameKey = "Elite",                 descriptionKey = "Erreiche Level 50",                              iconName = "\uD83E\uDD47", requiredValue = 50,  tier = 3),
            Achievement(id = "xp_1000",          nameKey = "XP-Sammler",            descriptionKey = "Sammle 1.000 XP",                                iconName = "\uD83D\uDC8E", requiredValue = 1000, tier = 1),
            Achievement(id = "xp_10000",         nameKey = "XP-Millionär (Kilo)",   descriptionKey = "Sammle 10.000 XP",                               iconName = "\uD83D\uDCB0", requiredValue = 10000, tier = 2),
            Achievement(id = "xp_100000",        nameKey = "XP-Legende",            descriptionKey = "Sammle 100.000 XP",                              iconName = "\uD83C\uDF1F", requiredValue = 100000, tier = 3),

            // ── Kategorien ────────────────────────────────────────────────
            Achievement(id = "cat_geo",          nameKey = "Geograf",               descriptionKey = "Schließe 10 Geographie-Quizzes ab",              iconName = "\uD83C\uDF0D", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_science",      nameKey = "Wissenschaftler",       descriptionKey = "Schließe 10 Wissenschafts-Quizzes ab",           iconName = "\uD83D\uDD2C", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_history",      nameKey = "Historiker",            descriptionKey = "Schließe 10 Geschichts-Quizzes ab",              iconName = "\uD83D\uDCDC", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_film",         nameKey = "Filmkenner",            descriptionKey = "Schließe 10 Film & TV-Quizzes ab",               iconName = "\uD83C\uDFAC", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_music",        nameKey = "Musikliebhaber",        descriptionKey = "Schließe 10 Musik-Quizzes ab",                   iconName = "\uD83C\uDFB5", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_sport",        nameKey = "Sportfan",              descriptionKey = "Schließe 10 Sport-Quizzes ab",                   iconName = "\u26BD",       requiredValue = 10,  tier = 1),
            Achievement(id = "cat_tech",         nameKey = "Technikfreak",          descriptionKey = "Schließe 10 Technologie-Quizzes ab",             iconName = "\uD83D\uDCBB", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_food",         nameKey = "Feinschmecker",         descriptionKey = "Schließe 10 Essen & Trinken-Quizzes ab",         iconName = "\uD83C\uDF73", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_animals",      nameKey = "Tierfreund",            descriptionKey = "Schließe 10 Tierwelt-Quizzes ab",                iconName = "\uD83D\uDC3E", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_literature",   nameKey = "Bücherwurm",            descriptionKey = "Schließe 10 Literatur-Quizzes ab",               iconName = "\uD83D\uDCDA", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_mixed",        nameKey = "Allrounder",            descriptionKey = "Schließe 10 Alle-Kategorien-Quizzes ab",         iconName = "\uD83C\uDF1F", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_logic",        nameKey = "Logiker",               descriptionKey = "Schließe 10 Logik-Quizzes ab",                   iconName = "\uD83E\uDDE0", requiredValue = 10,  tier = 1),
            Achievement(id = "all_cats",         nameKey = "Universalgelehrter",    descriptionKey = "Schließe jede Kategorie mindestens einmal ab",   iconName = "\uD83C\uDF10", requiredValue = 12,  tier = 3),

            // ── Tages-Challenge ───────────────────────────────────────────
            Achievement(id = "daily_3",          nameKey = "Dreifach-Tagesquiz",    descriptionKey = "Löse 3 Tages-Challenges",                        iconName = "\uD83D\uDCC5", requiredValue = 3,   tier = 1),
            Achievement(id = "daily_7",          nameKey = "Wöchentliches Ritual",  descriptionKey = "Löse 7 Tages-Challenges in Folge",               iconName = "\uD83D\uDCC6", requiredValue = 7,   tier = 2),
            Achievement(id = "daily_30",         nameKey = "Monatlicher Meister",   descriptionKey = "Löse 30 Tages-Challenges in Folge",              iconName = "\uD83D\uDDD3", requiredValue = 30,  tier = 3),

            // ── Schwierigkeitslevel ────────────────────────────────────────
            Achievement(id = "hard_10",          nameKey = "Herausforderungsannahme",descriptionKey = "Beantworte 10 schwere Fragen richtig",          iconName = "\uD83D\uDCAA", requiredValue = 10,  tier = 2),
            Achievement(id = "master_5",         nameKey = "Meisterfragen",         descriptionKey = "Beantworte 5 Master-Fragen richtig",             iconName = "\uD83E\uDDE0", requiredValue = 5,   tier = 3),
            Achievement(id = "perfect_hard",     nameKey = "Perfektionist",         descriptionKey = "Beende ein Schwer-Quiz mit 100%",                iconName = "\uD83D\uDCAF", requiredValue = 1,   tier = 3),

            // ── Spielzeit ─────────────────────────────────────────────────
            Achievement(id = "playtime_1h",      nameKey = "Eine Stunde",           descriptionKey = "Spiele insgesamt 1 Stunde",                      iconName = "\u231B",       requiredValue = 60,  tier = 1),
            Achievement(id = "playtime_10h",     nameKey = "Zehn Stunden",          descriptionKey = "Spiele insgesamt 10 Stunden",                    iconName = "\uD83D\uDD50", requiredValue = 600, tier = 2),
            Achievement(id = "playtime_100h",    nameKey = "Hundert Stunden",       descriptionKey = "Spiele insgesamt 100 Stunden",                   iconName = "\uD83C\uDFC5", requiredValue = 6000, tier = 3),

            // ── Sonderpunkte ──────────────────────────────────────────────
            Achievement(id = "night_owl",        nameKey = "Nachteule",             descriptionKey = "Spiele nach Mitternacht",                        iconName = "\uD83E\uDD89", requiredValue = 1,   tier = 1),
            Achievement(id = "early_bird",       nameKey = "Frühaufsteher",         descriptionKey = "Spiele vor 6 Uhr morgens",                       iconName = "\uD83C\uDF05", requiredValue = 1,   tier = 1),
            Achievement(id = "no_hint",          nameKey = "Eigene Kraft",          descriptionKey = "Beende 5 Quizzes ohne Joker",                    iconName = "\uD83E\uDDE9", requiredValue = 5,   tier = 2),
            Achievement(id = "comeback",         nameKey = "Comeback",              descriptionKey = "Gewinne nach 3 falschen Antworten in Folge",     iconName = "\uD83D\uDD04", requiredValue = 1,   tier = 2),
        )
        dao.insertAll(achievements)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initial UserProgress (empty row for first launch)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun insertInitialProgress(database: QuizDatabase) {
        val dao = database.progressDao()
        dao.insertProgress(UserProgress())
    }
}
