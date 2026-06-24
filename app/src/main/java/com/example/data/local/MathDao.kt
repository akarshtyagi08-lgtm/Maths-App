package com.example.data.local

import androidx.room.*
import com.example.data.model.SavedQuestion
import com.example.data.model.QuestionPaper
import com.example.data.model.PracticeProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface MathDao {
    // SavedQuestions
    @Query("SELECT * FROM saved_questions ORDER BY timestamp DESC")
    fun getAllSavedQuestions(): Flow<List<SavedQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedQuestion(question: SavedQuestion)

    @Delete
    suspend fun deleteSavedQuestion(question: SavedQuestion)

    @Query("DELETE FROM saved_questions WHERE id = :id")
    suspend fun deleteSavedQuestionById(id: Int)

    // QuestionPapers
    @Query("SELECT * FROM question_papers ORDER BY timestamp DESC")
    fun getAllQuestionPapers(): Flow<List<QuestionPaper>>

    @Query("SELECT * FROM question_papers WHERE id = :id")
    suspend fun getQuestionPaperById(id: Int): QuestionPaper?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionPaper(paper: QuestionPaper): Long

    @Query("DELETE FROM question_papers WHERE id = :id")
    suspend fun deleteQuestionPaperById(id: Int)

    // PracticeProfile
    @Query("SELECT * FROM practice_profile WHERE id = 1")
    fun getProfileFlow(): Flow<PracticeProfile?>

    @Query("SELECT * FROM practice_profile WHERE id = 1")
    suspend fun getProfileDirect(): PracticeProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PracticeProfile)
}
