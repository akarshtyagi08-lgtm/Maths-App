package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_questions")
data class SavedQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionText: String,
    val imageBase64: String? = null,
    val solutionText: String,
    val isCorrect: Boolean? = null,
    val gradeLevel: String,
    val topic: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "question_papers")
data class QuestionPaper(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val grade: String,
    val topic: String,
    val difficulty: String,
    val questionsJson: String, // JSON Array of MathQuestion objects
    val userAnswersJson: String? = null, // Map of index to answer
    val feedbackJson: String? = null, // AI grading details
    val score: Int = 0,
    val maxScore: Int = 0,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "practice_profile")
data class PracticeProfile(
    @PrimaryKey val id: Int = 1,
    val currentGrade: String = "High School",
    val totalSolved: Int = 0,
    val correctSolved: Int = 0,
    val streakDays: Int = 0,
    val lastSolvedTimestamp: Long = 0L
)

// Standard non-entity data classes
data class MathQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String, // String matching the correct option text exactly
    val stepByStepSolution: String
)
