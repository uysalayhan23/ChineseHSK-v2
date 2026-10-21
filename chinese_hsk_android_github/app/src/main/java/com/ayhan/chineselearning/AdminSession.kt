package com.ayhan.chineselearning

object AdminSession {
    const val DEFAULT_PASSWORD = "HSK2026!"

    @Volatile
    var active: Boolean = false

    fun login(password: String): Boolean {
        val ok = password == DEFAULT_PASSWORD
        if (ok) active = true
        return ok
    }

    fun logout() {
        active = false
    }
}
