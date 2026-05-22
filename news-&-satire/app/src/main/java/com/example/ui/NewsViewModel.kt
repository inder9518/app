package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiGenerator
import com.example.data.AppDatabase
import com.example.data.NewsArticle
import com.example.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NewsFeedUiState {
    object Loading : NewsFeedUiState
    data class Success(val articles: List<NewsArticle>) : NewsFeedUiState
}

sealed interface GenerationUiState {
    object Idle : GenerationUiState
    object Loading : GenerationUiState
    data class Success(val createdArticle: NewsArticle) : GenerationUiState
    data class Error(val message: String) : GenerationUiState
}

// Data class for Fact Check Game Round
data class GameQuestion(
    val id: Int,
    val headline: String,
    val isFake: Boolean,
    val justification: String,
    val title: String,
    val body: String
)

data class GameState(
    val questions: List<GameQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val score: Int = 0,
    val selectedAnswer: Boolean? = null, // null = unanswered, true = selected REAL, false = selected SATIRE
    val isFinished: Boolean = false,
    val correctGuesses: Int = 0,
    val incorrectGuesses: Int = 0
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NewsRepository
    private val geminiGenerator = GeminiGenerator()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NewsRepository(database.newsDao())

        // Ensure database has default items loaded on start
        viewModelScope.launch {
            repository.preloadDefaultArticlesIfNeeded()
            startNewGame()
        }
    }

    // Combine database articles with matching categories and search queries reactive state flow
    val feedUiState: StateFlow<NewsFeedUiState> = combine(
        repository.allArticlesFlow,
        _selectedCategory,
        _searchQuery
    ) { articles, category, query ->
        if (articles.isEmpty()) {
            NewsFeedUiState.Loading
        } else {
            var filtered = articles
            
            // Search filter
            if (query.isNotEmpty()) {
                filtered = filtered.filter {
                    it.title.contains(query, ignoreCase = true) || 
                    it.body.contains(query, ignoreCase = true)
                }
            }

            // Category filter
            if (category != "All") {
                filtered = when (category) {
                    "Bookmarks" -> filtered.filter { it.isBookmarked }
                    "User-Created" -> filtered.filter { it.isUserCreated }
                    else -> filtered.filter { it.category == category }
                }
            }

            NewsFeedUiState.Success(filtered)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewsFeedUiState.Loading
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBookmark(article: NewsArticle) {
        viewModelScope.launch {
            repository.updateBookmark(article.id, !article.isBookmarked)
        }
    }

    fun likeArticle(article: NewsArticle) {
        viewModelScope.launch {
            repository.incrementLikes(article.id)
        }
    }

    fun deleteArticle(articleId: Int) {
        viewModelScope.launch {
            repository.deleteArticle(articleId)
        }
    }

    fun clearUserArticles() {
        viewModelScope.launch {
            repository.resetUserArticles()
            selectCategory("All")
        }
    }

    fun resetGenerationState() {
        _generationState.value = GenerationUiState.Idle
    }

    // AI Satire Generator Flow
    fun generateSatireArticle(topic: String, style: String) {
        if (topic.trim().isEmpty()) {
            _generationState.value = GenerationUiState.Error("Please enter a valid topic")
            return
        }

        _generationState.value = GenerationUiState.Loading

        viewModelScope.launch {
            try {
                val result = geminiGenerator.generateSatire(topic, style)
                
                // Map result to a NewsArticle
                val newArticle = NewsArticle(
                    title = result.title,
                    body = result.body,
                    category = if (result.isAiGenerated) "Gemini Satire" else "Offline Satire",
                    isFake = true,
                    author = if (result.isAiGenerated) "Gemini 3.5 Flash" else "Local Generator",
                    factCheckStatus = result.status,
                    likesCount = (10..500).random(),
                    isUserCreated = true
                )

                // Insert into Database
                val insertedId = repository.insertArticle(newArticle)
                val articleWithId = newArticle.copy(id = insertedId.toInt())

                _generationState.value = GenerationUiState.Success(articleWithId)
                
                // Redirect category to show user's generated articles!
                _selectedCategory.value = "User-Created"
            } catch (e: Exception) {
                _generationState.value = GenerationUiState.Error("Failed to generate: ${e.localizedMessage}")
            }
        }
    }

    // --- FACT CHECK GAME FLOW ---
    fun startNewGame() {
        viewModelScope.launch {
            // Pick a mix of 5 funny real/fake news articles
            _gameState.value = GameState(
                questions = getDailyChallengeQuestions(),
                currentIndex = 0,
                score = 0,
                selectedAnswer = null,
                isFinished = false,
                correctGuesses = 0,
                incorrectGuesses = 0
            )
        }
    }

    fun submitAnswer(userIsReal: Boolean) {
        val currentState = _gameState.value
        if (currentState.selectedAnswer != null || currentState.isFinished) return

        val currentQuestion = currentState.questions[currentState.currentIndex]
        val isCorrect = (userIsReal && !currentQuestion.isFake) || (!userIsReal && currentQuestion.isFake)

        val newCorrect = if (isCorrect) currentState.correctGuesses + 1 else currentState.correctGuesses
        val newIncorrect = if (!isCorrect) currentState.incorrectGuesses + 1 else currentState.incorrectGuesses
        val addedPoints = if (isCorrect) 100 else 0

        _gameState.value = currentState.copy(
            selectedAnswer = userIsReal,
            score = currentState.score + addedPoints,
            correctGuesses = newCorrect,
            incorrectGuesses = newIncorrect
        )
    }

    fun nextQuestion() {
        val currentState = _gameState.value
        val nextIndex = currentState.currentIndex + 1
        val isOver = nextIndex >= currentState.questions.size

        if (isOver) {
            _gameState.value = currentState.copy(isFinished = true, selectedAnswer = null)
        } else {
            _gameState.value = currentState.copy(
                currentIndex = nextIndex,
                selectedAnswer = null
            )
        }
    }

    private fun getDailyChallengeQuestions(): List<GameQuestion> {
        return listOf(
            GameQuestion(
                id = 1,
                headline = "Tabby Cat Elected Mayor of New York Wins on 'Nap & Snacks' Platform",
                isFake = true,
                justification = "While cats have been elected mayors of small Alaskan towns (like Stubbs the Cat in Talkeetna), a ginger tabby has not run, won, or overseen municipal policies in New York City.",
                title = "Feline Mayor Claims New York",
                body = "Barnaby, the chubby tabby cat, won 94% of the mayoral votes in this parody scenario."
            ),
            GameQuestion(
                id = 2,
                headline = "Sweden Literally Runs Out of Trash, Starts Importing Garbage From Neighboring Countries",
                isFake = false,
                justification = "This is ACTUALLY TRUE! Sweden's recycling program is so incredibly efficient that only less than 1% of household waste ends up in landfills. They import trash from other countries to power their waste-to-energy plants.",
                title = "The Swedish Recycling Miracle",
                body = "Due to highly active eco-grids, Sweden imports waste to generate clean municipal electricity."
            ),
            GameQuestion(
                id = 3,
                headline = "Australian Man Punches 12-foot Saltwater Crocodile to Rescue His Chihuahua",
                isFake = false,
                justification = "This is ACTUALLY TRUE! In Northern Territory, Queensland, cases of dog-lovers doing crazy hand-to-hand combat to free their pets from reptiles exist and are documented in local registries.",
                title = "Outback Chihuahua Rescued",
                body = "Toby the Chihuahua survived with just a wet tail after his owner leapt into action."
            ),
            GameQuestion(
                id = 4,
                headline = "French Seaside Town Officially Outlaws Rain to Boost Summer Beach Tourism",
                isFake = true,
                justification = "While mayors love sunshine, a French town cannot fine rainclouds or prosecute precipitation under state environmental law. This is a satire piece on extreme administrative regulations.",
                title = "Weather Mandate Fails",
                body = "Saint-Sébastien-des-Prés passed a funny gag law, but cloud humidity remains outside national jurisdiction."
            ),
            GameQuestion(
                id = 5,
                headline = "NASA Announces New Paid Subscription Plan for Earth's Standard Gravity Starting Next Tuesday",
                isFake = true,
                justification = "Gravity is a fundamental property of matter caused by mass, not a paid service managed by NASA or any administrative authority. No, you won't float away next week unless you forget to pay a bill.",
                title = "Subscription Grounding",
                body = "A humorous parody item pointing out subscription-based business models of modern digital fields."
            )
        ).shuffled() // Shuffle to make it unique every session
    }
}
