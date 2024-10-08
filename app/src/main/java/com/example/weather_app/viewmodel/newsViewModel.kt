package com.example.weather_app.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weather_app.data.news.FirebaseNewsRepository
import com.example.weather_app.data.news.newsDB
import com.example.weather_app.data.news.newsRepository
import com.example.weather_app.model.News
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: newsRepository
    private val firebaseRepository: FirebaseNewsRepository = FirebaseNewsRepository()
    val allNews: LiveData<List<News>>

    init {
        val newsDAO = newsDB.getDatabase(application).newsDAO()
        repository = newsRepository(newsDAO)
        allNews = repository.allNews
    }

    val latestNews: LiveData<List<News>> = repository.getLatestNews()
    private val _newsByTitle = MutableLiveData<News?>()
    val newsByTitle: LiveData<News?> get() = _newsByTitle

    fun getNewsByTitle(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseRepository.getNewsByTitle(title) { news ->
                _newsByTitle.postValue(news)
            }
        }
    }

    fun getNews(id: Int): LiveData<News?> {
        return repository.getNewsStream(id)
    }

    fun getNewsCount(): LiveData<Int> {
        val countLiveData = MutableLiveData<Int>()
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getCountNews()
            countLiveData.postValue(count)
        }
        return countLiveData
    }

    fun insert(news: News, context: Context, onSuccess: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.insertNews(news)
            firebaseRepository.insertOrUpdateNewsInFirebase(news, context) {
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("NewsViewModel", "Error inserting news", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error inserting news", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun update(news: News, context: Context, onSuccess: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.updateNews(news)
            firebaseRepository.insertOrUpdateNewsInFirebase(news, context) {
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("NewsViewModel", "Error updating news", e)
        }
    }

    fun delete(news: News) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.deleteNews(news)
            firebaseRepository.deleteNewsFromFirebase(news.title)
        } catch (e: Exception) {
            Log.e("NewsViewModel", "Error deleting news", e)
        }
    }
}
