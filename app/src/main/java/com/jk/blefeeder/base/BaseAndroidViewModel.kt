package com.jk.blefeeder.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wyj.base.http.Constants
import com.wyj.base.http.Resource
import com.wyj.base.http.ResponseListener
import com.wyj.base.http.retrofit2.ApiException
import com.wyj.base.http.retrofit2.HttpResponse
import com.wyj.base.logE
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import java.lang.Exception
import java.net.SocketException
import java.net.UnknownHostException

open class BaseAndroidViewModel(val app: Application) : AndroidViewModel(app) {

    val tostMsg = MutableLiveData<String>()
    val errorMsg = MutableLiveData<String>()
    val completeMsg = MutableLiveData<String>()
    val loginTimeOut = MutableLiveData<Boolean>()
    val loading = MutableLiveData<Boolean>()
    val loadingTime = MutableLiveData<Int>()

    val mDisposable by lazy { CompositeDisposable() }

    override fun onCleared() {
        viewModelScope.cancel()
        mDisposable.clear()
        super.onCleared()
    }

    private fun launchOnUI(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            block()
        }
    }

    private fun launchOnUI(
        block: suspend CoroutineScope.() -> Unit,
        thread: CoroutineDispatcher
    ): Job =
        viewModelScope.launch(thread) {
            block()
        }

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        launchOnUI(block)
    }

    fun launch(block: suspend CoroutineScope.() -> Unit, thread: CoroutineDispatcher): Job =
        launchOnUI(block, thread)

    fun <T> launch(
        block: suspend CoroutineScope.() -> HttpResponse<T>,
        listener: ResponseListener<Resource<T>>?
    ) {
        launchOnUI {
            tryBlock(block, listener)
        }
    }

    fun <T> launch(
        block: suspend CoroutineScope.() -> HttpResponse<T>,
        thread: CoroutineDispatcher,
        listener: ResponseListener<Resource<T>>?
    ): Job =
        launchOnUI({
            tryBlock(block, listener)
        }, thread)


    private suspend fun <T> tryBlock(
        block: suspend CoroutineScope.() -> HttpResponse<T>,
        listener: ResponseListener<Resource<T>>?
    ) {
        if (loading.value != true) {
//            loading.postValue(true)
            loading.value = true
        }
        coroutineScope {
            try {
                val response = block()
                when (response.code) {
                    20000 -> {
                        listener?.onResponse(Resource.success(response.msg))
                        loading.value = false
                    }
                    else -> {
                        listener?.onResponse(Resource.error(response.code ?: -1))
                        loading.value = false
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is ApiException -> {
                        logE("${e.message}----------${e.code}")
                        when (e.code) {
//                            ServiceCode.LOGINED_TIME_OUT -> {
//                                loginTimeOut.value = true
//                            }
//                            20002 -> loginTimeOut.value = true
                            Constants.WEB_RESP_CODE_FAILURE -> listener?.onResponse(
                                Resource.success(
                                    null
                                )
                            )
                            else -> {
                                errorMsg.value = "${e.code}"
                                listener?.onResponse(Resource.error(e.code))
                            }
                        }
                        loading.value = false
                    }
                    is SocketException -> {
                        listener?.onResponse(Resource.retry())
                    }
                    is UnknownHostException -> {

                        listener?.onResponse(Resource.retry())
                    }
                    else -> {
                        loading.value = false
                        logE("${e.message},${Constants.WEB_RESP_CODE_DEFAULT_FAILURE}")
                        listener?.onResponse(Resource.error(Constants.WEB_RESP_CODE_DEFAULT_FAILURE))
                    }
                }
            }
        }
    }

    open fun onResume() {}

    open fun onPause() {}

    open fun onDestory() {}

    fun formatError(code: String?, error: String?) {
        loading.value = false
        errorMsg.value = error
    }
}