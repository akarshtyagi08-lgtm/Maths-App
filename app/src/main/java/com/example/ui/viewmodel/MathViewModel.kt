package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiMathService
import com.example.data.local.MathDatabase
import com.example.data.model.MathQuestion
import com.example.data.model.PracticeProfile
import com.example.data.model.QuestionPaper
import com.example.data.model.SavedQuestion
import com.example.data.repository.MathRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

enum class MathScreen {
    DASHBOARD,
    SOLVER,
    GENERATOR,
    ACTIVE_EXAM,
    EXAM_RESULT,
    STATS
}

data class Stroke(
    val points: List<Offset>,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    val width: Float = 6f
)

class MathViewModel(application: Application) : AndroidViewModel(application) {

    private val mathDao = MathDatabase.getDatabase(application).mathDao()
    private val repository = MathRepository(mathDao)

    // Room flow state holders
    val savedQuestions: StateFlow<List<SavedQuestion>> = repository.allSavedQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questionPapers: StateFlow<List<QuestionPaper>> = repository.allQuestionPapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<PracticeProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Navigation State
    private val _currentScreen = MutableStateFlow(MathScreen.DASHBOARD)
    val currentScreen: StateFlow<MathScreen> = _currentScreen.asStateFlow()

    // Solver Screen States
    private val _solverQuestionText = MutableStateFlow("")
    val solverQuestionText: StateFlow<String> = _solverQuestionText.asStateFlow()

    private val _solverImageBase64 = MutableStateFlow<String?>(null)
    val solverImageBase64: StateFlow<String?> = _solverImageBase64.asStateFlow()

    private val _solverIsLoading = MutableStateFlow(false)
    val solverIsLoading: StateFlow<Boolean> = _solverIsLoading.asStateFlow()

    private val _solverExplanation = MutableStateFlow<String?>(null)
    val solverExplanation: StateFlow<String?> = _solverExplanation.asStateFlow()

    // Canvas Draw States
    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _currentStroke = MutableStateFlow<Stroke?>(null)
    val currentStroke: StateFlow<Stroke?> = _currentStroke.asStateFlow()

    // Question Paper Generator States
    private val _generatorGrade = MutableStateFlow("High School")
    val generatorGrade: StateFlow<String> = _generatorGrade.asStateFlow()

    private val _generatorTopic = MutableStateFlow("Algebra")
    val generatorTopic: StateFlow<String> = _generatorTopic.asStateFlow()

    private val _generatorDifficulty = MutableStateFlow("Medium")
    val generatorDifficulty: StateFlow<String> = _generatorDifficulty.asStateFlow()

    private val _generatorCount = MutableStateFlow(5)
    val generatorCount: StateFlow<Int> = _generatorCount.asStateFlow()

    private val _generatorIsLoading = MutableStateFlow(false)
    val generatorIsLoading: StateFlow<Boolean> = _generatorIsLoading.asStateFlow()

    private val _generatorError = MutableStateFlow<String?>(null)
    val generatorError: StateFlow<String?> = _generatorError.asStateFlow()

    // Active practice exam state
    private val _selectedPaper = MutableStateFlow<QuestionPaper?>(null)
    val selectedPaper: StateFlow<QuestionPaper?> = _selectedPaper.asStateFlow()

    private val _activeExamAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val activeExamAnswers: StateFlow<Map<Int, String>> = _activeExamAnswers.asStateFlow()

    private val _activeExamGradingLoading = MutableStateFlow(false)
    val activeExamGradingLoading: StateFlow<Boolean> = _activeExamGradingLoading.asStateFlow()

    // Selected Detail view state
    private val _selectedSavedQuestion = MutableStateFlow<SavedQuestion?>(null)
    val selectedSavedQuestion: StateFlow<SavedQuestion?> = _selectedSavedQuestion.asStateFlow()

    fun navigateTo(screen: MathScreen) {
        _currentScreen.value = screen
    }

    // --- SOLVER METHODS ---

    fun setSolverQuestionText(text: String) {
        _solverQuestionText.value = text
    }

    fun setSolverImageBase64(base64: String?) {
        _solverImageBase64.value = base64
    }

    fun clearSolver() {
        _solverQuestionText.value = ""
        _solverImageBase64.value = null
        _solverExplanation.value = null
        _strokes.value = emptyList()
        _currentStroke.value = null
    }

    fun solveCurrentQuestion() {
        val question = _solverQuestionText.value
        val image = _solverImageBase64.value
        
        if (question.isBlank() && image == null) return

        viewModelScope.launch {
            _solverIsLoading.value = true
            _solverExplanation.value = null
            
            val result = GeminiMathService.solveMathQuestion(question, image)
            
            if (result == "API_KEY_MISSING") {
                _solverExplanation.value = "API_KEY_MISSING_ERROR"
            } else {
                _solverExplanation.value = result
                // Automatically save solved question to database
                val grade = profile.value?.currentGrade ?: "High School"
                val topic = determineTopic(question)
                
                repository.saveQuestion(
                    SavedQuestion(
                        questionText = question.ifBlank { "Drawn Equation/Problem Image" },
                        imageBase64 = image,
                        solutionText = result,
                        gradeLevel = grade,
                        topic = topic
                    )
                )
                // Increment solved count in statistics
                repository.updateProfile(grade, isAnswerCorrect = true)
            }
            _solverIsLoading.value = false
        }
    }

    // --- CANVAS METHODS ---

    fun startStroke(offset: Offset) {
        _currentStroke.value = Stroke(points = listOf(offset))
    }

    fun addToStroke(offset: Offset) {
        val current = _currentStroke.value ?: return
        _currentStroke.value = current.copy(points = current.points + offset)
    }

    fun endStroke() {
        val current = _currentStroke.value ?: return
        _strokes.value = _strokes.value + current
        _currentStroke.value = null
    }

    fun clearCanvas() {
        _strokes.value = emptyList()
        _currentStroke.value = null
        _solverImageBase64.value = null
    }

    /**
     * Rasterizes the hand-drawn strokes onto a bitmap and exports to JPEG base64.
     */
    fun rasterizeCanvas(width: Int, height: Int) {
        if (_strokes.value.isEmpty()) return

        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // White sheet background

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = 6f
                color = Color.BLACK
            }

            for (stroke in _strokes.value) {
                if (stroke.points.isNotEmpty()) {
                    val path = Path()
                    path.moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        path.lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                    canvas.drawPath(path, paint)
                }
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            
            _solverImageBase64.value = base64
        } catch (e: Exception) {
            Log.e("MathViewModel", "Failed to rasterize drawing", e)
        }
    }

    // --- PRESETS ---

    fun loadPreset(title: String, question: String, strokesPreset: List<Stroke>) {
        clearSolver()
        _solverQuestionText.value = question
        _strokes.value = strokesPreset
        // Automatically rasterize standard size for processing (e.g. 600x400)
        if (strokesPreset.isNotEmpty()) {
            rasterizeCanvas(600, 400)
        }
    }

    // --- GENERATOR METHODS ---

    fun setGeneratorGrade(grade: String) {
        _generatorGrade.value = grade
    }

    fun setGeneratorTopic(topic: String) {
        _generatorTopic.value = topic
    }

    fun setGeneratorDifficulty(difficulty: String) {
        _generatorDifficulty.value = difficulty
    }

    fun setGeneratorCount(count: Int) {
        _generatorCount.value = count
    }

    fun generatePaper() {
        val grade = _generatorGrade.value
        val topic = _generatorTopic.value
        val diff = _generatorDifficulty.value
        val count = _generatorCount.value

        viewModelScope.launch {
            _generatorIsLoading.value = true
            _generatorError.value = null

            val resultJson = GeminiMathService.generateQuestionPaper(grade, topic, diff, count)
            
            if (resultJson == "API_KEY_MISSING") {
                _generatorError.value = "API_KEY_MISSING"
                _generatorIsLoading.value = false
                return@launch
            }

            try {
                // Clean markdown block wrapper if AI returned it anyway
                var cleanJson = resultJson.trim()
                if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.substringAfter("[").substringBeforeLast("]")
                    cleanJson = "[$cleanJson]"
                }

                // Verify valid JSON
                val array = JSONArray(cleanJson)
                if (array.length() == 0) {
                    throw Exception("Generated paper has no questions.")
                }

                val title = "$topic Practice Test ($diff)"
                val paper = QuestionPaper(
                    title = title,
                    grade = grade,
                    topic = topic,
                    difficulty = diff,
                    questionsJson = cleanJson,
                    score = 0,
                    maxScore = count,
                    isCompleted = false
                )

                val paperId = repository.insertQuestionPaper(paper)
                val insertedPaper = paper.copy(id = paperId.toInt())
                
                _selectedPaper.value = insertedPaper
                _activeExamAnswers.value = emptyMap()
                
                // Go to Practice Screen!
                navigateTo(MathScreen.ACTIVE_EXAM)
            } catch (e: Exception) {
                Log.e("MathViewModel", "Error parsing generated paper JSON: ${e.message}", e)
                _generatorError.value = "Failed to parse questions. Please try generating again!\nTechnical: ${e.message}"
            }
            _generatorIsLoading.value = false
        }
    }

    // --- PRACTICE / GRADING METHODS ---

    fun selectPaper(paper: QuestionPaper) {
        _selectedPaper.value = paper
        if (paper.isCompleted) {
            navigateTo(MathScreen.EXAM_RESULT)
        } else {
            _activeExamAnswers.value = emptyMap()
            navigateTo(MathScreen.ACTIVE_EXAM)
        }
    }

    fun deletePaper(id: Int) {
        viewModelScope.launch {
            repository.deleteQuestionPaperById(id)
            if (_selectedPaper.value?.id == id) {
                _selectedPaper.value = null
            }
        }
    }

    fun selectSavedQuestion(question: SavedQuestion?) {
        _selectedSavedQuestion.value = question
    }

    fun deleteSavedQuestion(id: Int) {
        viewModelScope.launch {
            repository.deleteSavedQuestionById(id)
            if (_selectedSavedQuestion.value?.id == id) {
                _selectedSavedQuestion.value = null
            }
        }
    }

    fun setExamAnswer(questionIndex: Int, selectedOption: String) {
        val currentAnswers = _activeExamAnswers.value.toMutableMap()
        currentAnswers[questionIndex] = selectedOption
        _activeExamAnswers.value = currentAnswers
    }

    fun submitExam() {
        val paper = _selectedPaper.value ?: return
        val answers = _activeExamAnswers.value
        
        viewModelScope.launch {
            _activeExamGradingLoading.value = true
            
            // Build Answers JSON map for grading context
            val answersObj = JSONObject()
            answers.forEach { (index, value) ->
                answersObj.put(index.toString(), value)
            }
            val answersJsonStr = answersObj.toString()

            // Call AI Grader to generate full report
            val gradingReport = GeminiMathService.gradeExamSubmission(
                title = paper.title,
                questionsJson = paper.questionsJson,
                answersJson = answersJsonStr
            )

            // Calculate objective score
            var correctCount = 0
            try {
                val questionsArr = JSONArray(paper.questionsJson)
                for (i in 0 until questionsArr.length()) {
                    val questionObj = questionsArr.getJSONObject(i)
                    val correctAns = questionObj.optString("correctAnswer")
                    val studentAns = answers[i] ?: ""
                    if (studentAns.trim().equals(correctAns.trim(), ignoreCase = true)) {
                        correctCount++
                    }
                }
            } catch (e: Exception) {
                Log.e("MathViewModel", "Error grading score manually", e)
            }

            val updatedPaper = paper.copy(
                isCompleted = true,
                userAnswersJson = answersJsonStr,
                feedbackJson = gradingReport,
                score = correctCount
            )

            repository.insertQuestionPaper(updatedPaper)
            _selectedPaper.value = updatedPaper

            // Update user practice profile statistics
            val totalQuestions = paper.maxScore
            for (i in 0 until totalQuestions) {
                val isCorrect = answers[i]?.trim().equals(
                    JSONObject(JSONArray(paper.questionsJson).getString(i)).optString("correctAnswer").trim(),
                    ignoreCase = true
                )
                repository.updateProfile(paper.grade, isCorrect)
            }

            _activeExamGradingLoading.value = false
            navigateTo(MathScreen.EXAM_RESULT)
        }
    }

    fun setProfileGrade(grade: String) {
        viewModelScope.launch {
            repository.setGrade(grade)
        }
    }

    // Helper to determine general topic based on problem text
    private fun determineTopic(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("integral") || t.contains("derivative") || t.contains("dx") || t.contains("calculus") -> "Calculus"
            t.contains("geometry") || t.contains("circle") || t.contains("triangle") || t.contains("area") -> "Geometry"
            t.contains("matrix") || t.contains("vector") -> "Linear Algebra"
            t.contains("probability") || t.contains("mean") || t.contains("median") || t.contains("stats") -> "Statistics"
            t.contains("sin") || t.contains("cos") || t.contains("tan") || t.contains("theta") || t.contains("angle") -> "Trigonometry"
            else -> "Algebra"
        }
    }
}

class MathViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MathViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MathViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
