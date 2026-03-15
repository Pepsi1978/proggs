package com.quizverse.app.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quizverse.app.QuizVerseApp
import com.quizverse.app.data.repository.QuizRepository

/**
 * Factory that creates all ViewModels which require a [QuizRepository] dependency.
 *
 * Usage inside a Composable:
 * ```kotlin
 * val viewModel: QuizViewModel = viewModel(factory = QuizViewModelFactory.create())
 * ```
 *
 * Or manually:
 * ```kotlin
 * val app = LocalContext.current.applicationContext as QuizVerseApp
 * val repository = QuizRepository(app.database)
 * val viewModel: QuizViewModel = viewModel(factory = QuizViewModelFactory(repository))
 * ```
 */
class QuizViewModelFactory(
    private val repository: QuizRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repository) as T

            modelClass.isAssignableFrom(QuizViewModel::class.java) ->
                QuizViewModel(repository) as T

            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(repository) as T

            else -> throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}. " +
                "Add it to QuizViewModelFactory.create()."
            )
        }
    }

    companion object {
        /**
         * Convenience factory builder for use inside a Composable.
         * Reads the application context via [LocalContext] and builds the repository automatically.
         *
         * Example:
         * ```kotlin
         * val viewModel: HomeViewModel = viewModel(factory = QuizViewModelFactory.create())
         * ```
         */
        @Composable
        fun create(): QuizViewModelFactory {
            val app = LocalContext.current.applicationContext as QuizVerseApp
            val repository = QuizRepository(app.database)
            return QuizViewModelFactory(repository)
        }
    }
}
