package com.example.data.repository

import com.example.data.local.MathDao
import com.example.data.model.SavedQuestion
import com.example.data.model.QuestionPaper
import com.example.data.model.PracticeProfile
import kotlinx.coroutines.flow.Flow

class MathRepository(private val mathDao: MathDao) {

    val allSavedQuestions: Flow<List<SavedQuestion>> = mathDao.getAllSavedQuestions()
    val allQuestionPapers: Flow<List<QuestionPaper>> = mathDao.getAllQuestionPapers()
    val profile: Flow<PracticeProfile?> = mathDao.getProfileFlow()

    suspend fun saveQuestion(question: SavedQuestion) {
        mathDao.insertSavedQuestion(question)
    }

    suspend fun deleteSavedQuestionById(id: Int) {
        mathDao.deleteSavedQuestionById(id)
    }

    suspend fun getQuestionPaperById(id: Int): QuestionPaper? {
        return mathDao.getQuestionPaperById(id)
    }

    suspend fun insertQuestionPaper(paper: QuestionPaper): Long {
        return mathDao.insertQuestionPaper(paper)
    }

    suspend fun deleteQuestionPaperById(id: Int) {
        mathDao.deleteQuestionPaperById(id)
    }

    suspend fun updateProfile(currentGrade: String, isAnswerCorrect: Boolean) {
        val existingProfile = mathDao.getProfileDirect() ?: PracticeProfile(currentGrade = currentGrade)
        
        val newTotalSolved = existingProfile.totalSolved + 1
        val newCorrectSolved = existingProfile.correctSolved + if (isAnswerCorrect) 1 else 0
        
        // Streak calculation
        val now = System.currentTimeMillis()
        val lastSolved = existingProfile.lastSolvedTimestamp
        val oneDayMs = 24 * 60 * 60 * 1000L
        
        val newStreak = when {
            lastSolved == 0L -> 1
            now - lastSolved <= oneDayMs -> existingProfile.streakDays // solved on the same day
            now - lastSolved <= 2 * oneDayMs -> existingProfile.streakDays + 1 // solved on the next day
            else -> 1 // streak broken
        }

        val updated = existingProfile.copy(
            currentGrade = currentGrade,
            totalSolved = newTotalSolved,
            correctSolved = newCorrectSolved,
            streakDays = newStreak,
            lastSolvedTimestamp = now
        )
        mathDao.insertProfile(updated)
    }

    suspend fun setGrade(currentGrade: String) {
        val existingProfile = mathDao.getProfileDirect() ?: PracticeProfile(currentGrade = currentGrade)
        mathDao.insertProfile(existingProfile.copy(currentGrade = currentGrade))
    }
}
