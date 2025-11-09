package com.nimith.echonote.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class SafeApiCaller {
    suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): NetworkResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                NetworkResult.Success(apiCall.invoke())
            } catch (throwable: Throwable) {
                when (throwable) {
                    is HttpException -> {
                        NetworkResult.Error(Exception(throwable.response()?.errorBody()?.string()))
                    }
                    is IOException -> {
                        NetworkResult.Error(Exception("Network error"))
                    }
                    else -> {
                        NetworkResult.Error(Exception("Unexpected error"))
                    }
                }
            }
        }
    }
}