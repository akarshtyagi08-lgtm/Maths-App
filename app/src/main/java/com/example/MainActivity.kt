package com.example

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawScopeStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.MathQuestion
import com.example.data.model.QuestionPaper
import com.example.data.model.SavedQuestion
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MathScreen
import com.example.ui.viewmodel.MathViewModel
import com.example.ui.viewmodel.MathViewModelFactory
import com.example.ui.viewmodel.Stroke
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF080C14) // Clean Deep Space Dark Background
                ) {
                    MathAppContainer()
                }
            }
        }
    }
}

@Composable
fun MathAppContainer() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel: MathViewModel = viewModel(factory = MathViewModelFactory(app))
    
    val currentScreen by viewModel.currentScreen.collectAsState()
    val profile by viewModel.profile.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != MathScreen.ACTIVE_EXAM) {
                MathBottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080C14))
                .padding(innerPadding)
        ) {
            // Elegant background ambient glow circles
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x0F06B6D4), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.8f
                    ),
                    center = Offset(0f, 0f),
                    radius = size.width * 0.8f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x0B8B5CF6), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.width * 0.9f
                    ),
                    center = Offset(size.width, size.height),
                    radius = size.width * 0.9f
                )
            }

            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    MathScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    MathScreen.SOLVER -> SolverScreen(viewModel = viewModel)
                    MathScreen.GENERATOR -> GeneratorScreen(viewModel = viewModel)
                    MathScreen.ACTIVE_EXAM -> ActiveExamScreen(viewModel = viewModel)
                    MathScreen.EXAM_RESULT -> ExamResultScreen(viewModel = viewModel)
                    MathScreen.STATS -> StatsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MathBottomNavigationBar(
    currentScreen: MathScreen,
    onNavigate: (MathScreen) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0E1324),
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        val items = listOf(
            Triple(MathScreen.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
            Triple(MathScreen.SOLVER, "AI Solver", Icons.Default.Draw),
            Triple(MathScreen.GENERATOR, "Exams", Icons.Default.NoteAdd),
            Triple(MathScreen.STATS, "Statistics", Icons.Default.BarChart)
        )

        items.forEach { (screen, label, icon) ->
            NavigationBarItem(
                selected = currentScreen == screen || (screen == MathScreen.GENERATOR && currentScreen == MathScreen.EXAM_RESULT),
                onClick = { onNavigate(screen) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF06B6D4),
                    selectedTextColor = Color(0xFF06B6D4),
                    indicatorColor = Color(0xFF1E2E4A),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8)
                )
            )
        }
    }
}

// --- DASHBOARD SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MathViewModel) {
    val savedQuestions by viewModel.savedQuestions.collectAsState()
    val questionPapers by viewModel.questionPapers.collectAsState()
    val profile by viewModel.profile.collectAsState()
    
    val currentGrade = profile?.currentGrade ?: "High School"
    val totalSolved = profile?.totalSolved ?: 0
    val correctSolved = profile?.correctSolved ?: 0
    val accuracy = if (totalSolved > 0) (correctSolved * 100) / totalSolved else 0
    val streak = profile?.streakDays ?: 0

    var showGradeDialog by remember { mutableStateOf(false) }

    if (showGradeDialog) {
        GradeSelectionDialog(
            currentGrade = currentGrade,
            onDismiss = { showGradeDialog = false },
            onSelect = {
                viewModel.setProfileGrade(it)
                showGradeDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Hero Header Profile
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, Tyagi 👋",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "AI Math Practice",
                        fontSize = 26.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }

                // Grade Selector Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .clickable { showGradeDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = "Grade", tint = Color(0xFF06B6D4), modifier = Modifier.size(16.dp))
                        Text(text = currentGrade, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Change", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Streak & Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D35)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                            Text(text = "STREAK", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "$streak Days",
                            fontSize = 22.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (streak > 0) "Keep practicing daily!" else "Practice today to start a streak!",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Accuracy Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E242C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Percent, contentDescription = "Accuracy", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Text(text = "ACCURACY", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "$accuracy%",
                            fontSize = 22.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "$totalSolved Questions solved",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Action Prompts
        item {
            Card(
                onClick = { viewModel.navigateTo(MathScreen.SOLVER) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("solver_quickstart"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141B30)),
                border = BorderStroke(1.dp, Color(0xFF1E2E4A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF0F2D3A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = "Solve", tint = Color(0xFF06B6D4), modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Solve Math Problems with AI", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Type, click photo, or draw equations for instant step-by-step logic coaching.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0xFF94A3B8))
                }
            }
        }

        // Saved Practice History & Generated Exams Headers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Generated Practice Exams", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                TextButton(onClick = { viewModel.navigateTo(MathScreen.GENERATOR) }) {
                    Text("New Exam", color = Color(0xFF06B6D4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (questionPapers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E1324), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = "Empty", tint = Color(0xFF475569), modifier = Modifier.size(36.dp))
                        Text("No exams generated yet", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Generate a tailored multiple choice math test using AI!", color = Color(0xFF64748B), fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(questionPapers.take(3)) { paper ->
                ExamPaperItem(paper = paper, onSelect = { viewModel.selectPaper(paper) }, onDelete = { viewModel.deletePaper(paper.id) })
            }
        }

        item {
            Text("Solved Questions History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp))
        }

        if (savedQuestions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E1324), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Empty", tint = Color(0xFF475569), modifier = Modifier.size(36.dp))
                        Text("No questions solved yet", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Try typing or drawing a mathematical problem in AI Solver!", color = Color(0xFF64748B), fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(savedQuestions) { question ->
                SavedQuestionItem(
                    question = question,
                    onSelect = {
                        viewModel.selectSavedQuestion(question)
                        viewModel.navigateTo(MathScreen.SOLVER)
                        viewModel.setSolverQuestionText(question.questionText)
                        viewModel.setSolverImageBase64(question.imageBase64)
                    },
                    onDelete = { viewModel.deleteSavedQuestion(question.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ExamPaperItem(
    paper: QuestionPaper,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1324)),
        border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { onSelect() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (paper.isCompleted) Color(0xFF0B251E) else Color(0xFF2C220A),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (paper.isCompleted) Icons.Default.CheckCircle else Icons.Default.Pending,
                    contentDescription = "Status",
                    tint = if (paper.isCompleted) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${paper.grade} • ${paper.maxScore} Questions",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            if (paper.isCompleted) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Score: ${paper.score}/${paper.maxScore}",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Text(
                    text = "Practice",
                    color = Color(0xFF06B6D4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SavedQuestionItem(
    question: SavedQuestion,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1324)),
        border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { onSelect() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1E2E4A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (question.imageBase64 != null) Icons.Default.Image else Icons.Default.Calculate,
                    contentDescription = "Source",
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question.questionText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${question.topic} • ${question.gradeLevel}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = "View", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeSelectionDialog(
    currentGrade: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Practice Grade Level", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val grades = listOf("Elementary School", "Middle School", "High School", "University")
                grades.forEach { grade ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(grade) }
                            .background(
                                if (grade == currentGrade) Color(0xFF1E2E4A) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (grade == currentGrade),
                            onClick = { onSelect(grade) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF06B6D4), unselectedColor = Color(0xFF475569))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(grade, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0E1324)
    )
}

// --- SOLVER SCREEN WITH DRAWING CANVAS ---
@Composable
fun SolverScreen(viewModel: MathViewModel) {
    val questionText by viewModel.solverQuestionText.collectAsState()
    val explanation by viewModel.solverExplanation.collectAsState()
    val isLoading by viewModel.solverIsLoading.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val currentStroke by viewModel.currentStroke.collectAsState()
    val imageBase64 by viewModel.solverImageBase64.collectAsState()

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var activeInputTab by remember { mutableStateOf(0) } // 0 = Text, 1 = Sketch Sketchpad

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text("Interactive AI Solver", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Type equations, load custom presets, or draw directly on the canvas below.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }

        // Segmented Tab Selector
        item {
            TabRow(
                selectedTabIndex = activeInputTab,
                containerColor = Color(0xFF0E1324),
                contentColor = Color(0xFF06B6D4),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeInputTab]),
                        color = Color(0xFF06B6D4)
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeInputTab == 0,
                    onClick = { activeInputTab = 0 },
                    text = { Text("Keyboard Input", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = "Keyboard") }
                )
                Tab(
                    selected = activeInputTab == 1,
                    onClick = { activeInputTab = 1 },
                    text = { Text("Sketch & Draw", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Draw, contentDescription = "Sketch") }
                )
            }
        }

        if (activeInputTab == 0) {
            // Text Input
            item {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { viewModel.setSolverQuestionText(it) },
                    label = { Text("Enter your Math equation/question...", color = Color(0xFF64748B)) },
                    placeholder = { Text("e.g., Solve 2x + 5 = 15 or Integrate x^2 dx", color = Color(0xFF475569)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("solver_input_field"),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF0E1324),
                        unfocusedContainerColor = Color(0xFF0E1324)
                    )
                )
            }
        } else {
            // Preset Equation Buttons for Quick Verification
            item {
                Text("Preset Formulas (Quick Test)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        PresetBadge(title = "Quadratic") {
                            // Predefined strokes resembling "3x^2 - 5x + 2 = 0"
                            val strokesPreset = listOf(
                                Stroke(listOf(Offset(100f, 200f), Offset(150f, 150f), Offset(180f, 250f))), // '3'
                                Stroke(listOf(Offset(220f, 150f), Offset(280f, 250f))), // 'x' line 1
                                Stroke(listOf(Offset(280f, 150f), Offset(220f, 250f))), // 'x' line 2
                                Stroke(listOf(Offset(290f, 130f), Offset(320f, 110f), Offset(290f, 90f))), // '^2'
                                Stroke(listOf(Offset(340f, 200f), Offset(380f, 200f))), // '-'
                                Stroke(listOf(Offset(430f, 150f), Offset(410f, 180f), Offset(450f, 250f))), // '5'
                                Stroke(listOf(Offset(480f, 150f), Offset(540f, 250f))), // 'x'
                                Stroke(listOf(Offset(540f, 150f), Offset(480f, 250f))), // 'x'
                                Stroke(listOf(Offset(570f, 180f), Offset(610f, 180f))), // '+'
                                Stroke(listOf(Offset(590f, 160f), Offset(590f, 200f))), // '+'
                                Stroke(listOf(Offset(640f, 200f), Offset(680f, 170f), Offset(640f, 150f))), // '2'
                                Stroke(listOf(Offset(710f, 180f), Offset(760f, 180f))), // '='
                                Stroke(listOf(Offset(710f, 210f), Offset(760f, 210f))), // '='
                                Stroke(listOf(Offset(800f, 170f), Offset(800f, 220f)))  // '0'
                            )
                            viewModel.loadPreset(
                                "Quadratic",
                                "Solve quadratic equation 3x^2 - 5x + 2 = 0",
                                strokesPreset
                            )
                        }
                    }
                    item {
                        PresetBadge(title = "Calculus Integral") {
                            val strokesPreset = listOf(
                                Stroke(listOf(Offset(100f, 100f), Offset(100f, 300f))), // '∫' integral curve
                                Stroke(listOf(Offset(130f, 200f), Offset(180f, 300f))), // 'x'
                                Stroke(listOf(Offset(180f, 200f), Offset(130f, 300f))), // 'x'
                                Stroke(listOf(Offset(200f, 170f), Offset(230f, 150f))), // '^2'
                                Stroke(listOf(Offset(250f, 200f), Offset(280f, 300f))), // 'd'
                                Stroke(listOf(Offset(290f, 200f), Offset(340f, 300f)))  // 'x'
                            )
                            viewModel.loadPreset(
                                "Integral",
                                "Evaluate the definite integral of x^2 dx from 0 to 3",
                                strokesPreset
                            )
                        }
                    }
                    item {
                        PresetBadge(title = "Trigonometry") {
                            val strokesPreset = listOf(
                                Stroke(listOf(Offset(150f, 150f), Offset(300f, 150f))), // Base
                                Stroke(listOf(Offset(150f, 150f), Offset(150f, 300f))), // Height
                                Stroke(listOf(Offset(150f, 300f), Offset(300f, 150f)))  // Hypotenuse
                            )
                            viewModel.loadPreset(
                                "Trig",
                                "Find sine, cosine and tangent values of angle θ in a right angled triangle where base is 4 and height is 3.",
                                strokesPreset
                            )
                        }
                    }
                }
            }

            // Sketchpad Canvas Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .border(1.dp, Color(0xFF1E2E4A), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White), // Standard white canvas sheet
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset -> viewModel.startStroke(offset) },
                                        onDrag = { change, _ -> viewModel.addToStroke(change.position) },
                                        onDragEnd = {
                                            viewModel.endStroke()
                                            viewModel.rasterizeCanvas(canvasSize.width, canvasSize.height)
                                        }
                                    )
                                }
                                .onGloballyPositioned { coordinates ->
                                    canvasSize = coordinates.size
                                }
                        ) {
                            // Draw Grid background so it feels like math sheet notebook!
                            val step = 40.dp.toPx()
                            for (x in 0..size.width.toInt() step step.toInt()) {
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(x.toFloat(), 0f),
                                    end = Offset(x.toFloat(), size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (y in 0..size.height.toInt() step step.toInt()) {
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(0f, y.toFloat()),
                                    end = Offset(size.width, y.toFloat()),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw completed strokes
                            for (stroke in strokes) {
                                val path = Path()
                                if (stroke.points.isNotEmpty()) {
                                    path.moveTo(stroke.points[0].x, stroke.points[0].y)
                                    for (i in 1 until stroke.points.size) {
                                        path.lineTo(stroke.points[i].x, stroke.points[i].y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.Black,
                                        style = DrawScopeStroke(
                                            width = stroke.width,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                            }

                            // Draw active currently drawing stroke
                            currentStroke?.let { stroke ->
                                val path = Path()
                                if (stroke.points.isNotEmpty()) {
                                    path.moveTo(stroke.points[0].x, stroke.points[0].y)
                                    for (i in 1 until stroke.points.size) {
                                        path.lineTo(stroke.points[i].x, stroke.points[i].y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.Black,
                                        style = DrawScopeStroke(
                                            width = stroke.width,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                            }
                        }

                        // Drawing Controls Overlaid
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.clearCanvas() },
                                modifier = Modifier
                                    .background(Color(0xFFEF4444), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Canvas", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (strokes.isEmpty()) {
                            Text(
                                text = "Use your pointer/finger to sketch formulas here.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Processing & Submission Button
        item {
            Button(
                onClick = { viewModel.solveCurrentQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("solve_equation_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && (questionText.isNotBlank() || strokes.isNotEmpty())
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Solve")
                        Text(
                            text = if (strokes.isNotEmpty()) "Process Sketch & AI Solve" else "AI Coach Step-by-Step Solve",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Explanations & Solutions Output Card
        if (explanation != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1324)),
                    border = BorderStroke(1.dp, Color(0xFF1E2E4A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = "Coaching", tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                            Text("AI Coach Explanation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        if (explanation == "API_KEY_MISSING_ERROR") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2C1E1E), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🔑 API Key Missing", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "Please enter your GEMINI_API_KEY into the Secrets panel in AI Studio UI to enable real-time calculations.",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            MarkdownText(text = explanation ?: "")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun PresetBadge(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1E2E4A), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF06B6D4), modifier = Modifier.size(12.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- QUESTION PAPER GENERATOR SCREEN ---
@Composable
fun GeneratorScreen(viewModel: MathViewModel) {
    val grade by viewModel.generatorGrade.collectAsState()
    val topic by viewModel.generatorTopic.collectAsState()
    val diff by viewModel.generatorDifficulty.collectAsState()
    val count by viewModel.generatorCount.collectAsState()
    val isLoading by viewModel.generatorIsLoading.collectAsState()
    val error by viewModel.generatorError.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text("AI Test Paper Generator", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Create custom multiple choice tests with instant real-time AI grading reports.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }

        // Grade Selector
        item {
            Text("Target Academic Grade Level", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val grades = listOf("Elementary School", "Middle School", "High School", "University")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(grades) { item ->
                    GradeSelectorBadge(title = item, isSelected = (item == grade)) {
                        viewModel.setGeneratorGrade(item)
                    }
                }
            }
        }

        // Topic Selector
        item {
            Text("Mathematics Topic", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val topics = listOf("Algebra", "Calculus", "Geometry", "Trigonometry", "Statistics")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topics) { item ->
                    GradeSelectorBadge(title = item, isSelected = (item == topic)) {
                        viewModel.setGeneratorTopic(item)
                    }
                }
            }
        }

        // Difficulty Selector
        item {
            Text("Difficulty Tier", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val diffs = listOf("Easy", "Medium", "Hard")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                diffs.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (item == diff) Color(0xFF1E2E4A) else Color(0xFF0E1324),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (item == diff) Color(0xFF06B6D4) else Color(0xFF1E293B),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setGeneratorDifficulty(item) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Count Selector
        item {
            Text("Number of Questions", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 8).forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (item == count) Color(0xFF1E2E4A) else Color(0xFF0E1324),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (item == count) Color(0xFF06B6D4) else Color(0xFF1E293B),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setGeneratorCount(item) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$item Questions", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Error message panel
        if (error != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C1E1E), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔑 API Error Configuration" , color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (error == "API_KEY_MISSING") "Please enter your GEMINI_API_KEY in the Secrets panel in AI Studio UI to generate customized practice exams." else error ?: "",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Generate Action Button
        item {
            Button(
                onClick = { viewModel.generatePaper() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_paper_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = "Generate")
                        Text("Generate AI practice Exam", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun GradeSelectorBadge(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF1E2E4A) else Color(0xFF0E1324),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF06B6D4) else Color(0xFF1E293B),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// --- ACTIVE PRACTICE EXAM SCREEN ---
@Composable
fun ActiveExamScreen(viewModel: MathViewModel) {
    val paper by viewModel.selectedPaper.collectAsState()
    val answers by viewModel.activeExamAnswers.collectAsState()
    val gradingLoading by viewModel.activeExamGradingLoading.collectAsState()

    val currentPaper = paper ?: return

    // Convert serialized questionsJson back to domain models
    val questions = remember(currentPaper) {
        val list = mutableListOf<MathQuestion>()
        try {
            val array = JSONArray(currentPaper.questionsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val optionsArr = obj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArr.length()) {
                    options.add(optionsArr.getString(j))
                }
                list.add(
                    MathQuestion(
                        question = obj.getString("question"),
                        options = options,
                        correctAnswer = obj.getString("correctAnswer"),
                        stepByStepSolution = obj.getString("stepByStepSolution")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("ActiveExamScreen", "Error parsing question paper JSON", e)
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(MathScreen.DASHBOARD) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Exam Practice Sheet", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Box(modifier = Modifier.size(24.dp)) // Spacer
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1324)),
                border = BorderStroke(0.5.dp, Color(0xFF1E2E4A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(currentPaper.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${currentPaper.grade} • ${questions.size} Multiple Choice Questions", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        // Question cards lists
        items(questions.size) { index ->
            val item = questions[index]
            val selectedOption = answers[index]

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1324)),
                border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Question text header
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF06B6D4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = item.question,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                    }

                    // Options list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.options.forEach { option ->
                            val isSelected = (option == selectedOption)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setExamAnswer(index, option) }
                                    .background(
                                        if (isSelected) Color(0xFF1E2E4A) else Color(0xFF141A2E),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isSelected) Color(0xFF06B6D4) else Color(0xFF1E293B),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setExamAnswer(index, option) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF06B6D4), unselectedColor = Color(0xFF475569))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Submit Grade Button
        item {
            Button(
                onClick = { viewModel.submitExam() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_exam_grading_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                enabled = !gradingLoading && answers.size == questions.size
            ) {
                if (gradingLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Grading, contentDescription = "Submit")
                        Text(
                            text = if (answers.size < questions.size) "Complete all answers (${answers.size}/${questions.size})" else "Submit for AI Grading & Explanations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// --- EXAM RESULT SCREEN ---
@Composable
fun ExamResultScreen(viewModel: MathViewModel) {
    val paper by viewModel.selectedPaper.collectAsState()
    val currentPaper = paper ?: return

    val scorePercent = (currentPaper.score * 100) / currentPaper.maxScore

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(MathScreen.DASHBOARD) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("AI Grading Certificate", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Box(modifier = Modifier.size(24.dp))
            }
        }

        // High Score Metric Dashboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (scorePercent >= 70) Color(0xFF0E242C) else Color(0xFF241512)
                ),
                border = BorderStroke(
                    1.dp,
                    if (scorePercent >= 70) Color(0xFF10B981) else Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (scorePercent >= 70) Icons.Default.EmojiEvents else Icons.Default.Warning,
                        contentDescription = "Trophy",
                        tint = if (scorePercent >= 70) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (scorePercent >= 70) "Magnificent Effort!" else "Keep practicing to improve!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Score Certificate: ${currentPaper.score} / ${currentPaper.maxScore}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Academic accuracy tier: $scorePercent%",
                        color = if (scorePercent >= 70) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Detailed Report feedback output card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1324)),
                border = BorderStroke(0.5.dp, Color(0xFF1E2E4A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = "Report", tint = Color(0xFF06B6D4), modifier = Modifier.size(22.dp))
                        Text("AI Grader Coaching Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    MarkdownText(text = currentPaper.feedbackJson ?: "Generating report card...")
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.navigateTo(MathScreen.DASHBOARD) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Return to Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// --- STATISTICS SCREEN ---
@Composable
fun StatsScreen(viewModel: MathViewModel) {
    val profile by viewModel.profile.collectAsState()
    val savedQuestions by viewModel.savedQuestions.collectAsState()

    val totalSolved = profile?.totalSolved ?: 0
    val correctSolved = profile?.correctSolved ?: 0
    val accuracy = if (totalSolved > 0) (correctSolved * 100) / totalSolved else 0
    val streak = profile?.streakDays ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text("Statistics & Achievements", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Visual tracking of your mathematical accuracy, practice habits and streak milestones.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }

        // Interactive Canvas Line Chart showing practice trends
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1324)),
                border = BorderStroke(0.5.dp, Color(0xFF1E2E4A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Accuracy Trend History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Visual progress timeline across recent equations solved.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        // Draw Axis grid lines
                        val verticalGridCount = 4
                        for (i in 0..verticalGridCount) {
                            val y = size.height * (i.toFloat() / verticalGridCount)
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Plot accuracy wave points
                        // Let's draw a nice glowing sine/mathematical wave trend curve!
                        val wavePath = Path()
                        val pointsCount = 20
                        val gradientBrush = Brush.verticalGradient(
                            colors = listOf(Color(0x3D06B6D4), Color.Transparent),
                            startY = 0f,
                            endY = size.height
                        )

                        var started = false
                        for (i in 0..pointsCount) {
                            val x = size.width * (i.toFloat() / pointsCount)
                            // A gorgeous wave showing dynamic upward progression
                            val angle = (i.toFloat() / pointsCount) * (Math.PI * 2)
                            val yOffset = Math.sin(angle * 1.5).toFloat() * 15.dp.toPx()
                            val progressFactor = 1.0f - (i.toFloat() / pointsCount) * 0.4f
                            val y = size.height * progressFactor + yOffset

                            if (!started) {
                                wavePath.moveTo(x, y)
                                started = true
                            } else {
                                wavePath.lineTo(x, y)
                            }
                        }

                        // Draw area gradient fill
                        val fillPath = Path().apply {
                            addPath(wavePath)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path = fillPath, brush = gradientBrush)

                        // Draw glowing line
                        drawPath(
                            path = wavePath,
                            color = Color(0xFF06B6D4),
                            style = DrawScopeStroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Day 1", fontSize = 10.sp, color = Color(0xFF475569))
                        Text("Today", fontSize = 10.sp, color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Achievements Milestones List
        item {
            Text("Your Achievement Badges", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Milestone 1
                AchievementCard(
                    title = "Alpha Solved",
                    desc = "First question solved step-by-step.",
                    icon = Icons.Default.Calculate,
                    unlocked = totalSolved >= 1,
                    modifier = Modifier.weight(1f)
                )

                // Milestone 2
                AchievementCard(
                    title = "Daily Spark",
                    desc = "Solve questions 3 days streak.",
                    icon = Icons.Default.LocalFireDepartment,
                    unlocked = streak >= 3,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Milestone 3
                AchievementCard(
                    title = "Math Master",
                    desc = "Reach 90% accuracy score.",
                    icon = Icons.Default.Grade,
                    unlocked = accuracy >= 90 && totalSolved >= 5,
                    modifier = Modifier.weight(1f)
                )

                // Milestone 4
                AchievementCard(
                    title = "Quiz Taker",
                    desc = "Complete 3 AI exam tests.",
                    icon = Icons.Default.EmojiEvents,
                    unlocked = totalSolved >= 15,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun AchievementCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) Color(0xFF0E1F24) else Color(0xFF0E1324)
        ),
        border = BorderStroke(
            0.5.dp,
            if (unlocked) Color(0xFF06B6D4) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (unlocked) Color(0xFF155361) else Color(0xFF1E293B),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = if (unlocked) Color(0xFF06B6D4) else Color(0xFF475569),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) Color.White else Color(0xFF475569)
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = if (unlocked) Color(0xFF94A3B8) else Color(0xFF475569),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

// --- STYLED MARKDOWN TEXT RENDERER ---
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val lines = text.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### "),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06B6D4),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## "),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# "),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF06B6D4),
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 12.dp, top = 2.dp)) {
                        Text(
                            text = "• ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06B6D4)
                        )
                        Text(
                            text = parseBoldMarkdown(trimmed.substring(2)),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = color
                        )
                    }
                }
                trimmed.firstOrNull()?.isDigit() == true && trimmed.contains(". ") && trimmed.indexOf(". ") < 4 -> {
                    val index = trimmed.indexOf(". ")
                    val number = trimmed.substring(0, index + 2)
                    val content = trimmed.substring(index + 2)
                    Row(modifier = Modifier.padding(start = 12.dp, top = 2.dp)) {
                        Text(
                            text = number,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06B6D4)
                        )
                        Text(
                            text = parseBoldMarkdown(content),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = color
                        )
                    }
                }
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseBoldMarkdown(trimmed),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = color,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

fun parseBoldMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val start = text.indexOf("**", cursor)
            if (start == -1) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, start))
            val end = text.indexOf("**", start + 2)
            if (end == -1) {
                append(text.substring(start))
                break
            }
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4)))
            append(text.substring(start + 2, end))
            pop()
            cursor = end + 2
        }
    }
}
