package ai.saniou.thread.data.source.nmb.remote.dto

import kotlinx.serialization.Serializable

/**
 * 用户信息（饼干）
 */
@Serializable
data class User(
    val id: String,
    val name: String,
    val cookie: String,
    val userHash: String,
    val isActive: Boolean = true
)

/**
 * 登录请求
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val verify: String
)

/**
 * 登录响应
 */
@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val cookie: String? = null,
    val userHash: String? = null
)

/**
 * 饼干列表响应
 */
@Serializable
data class CookieListResponse(
    val success: Boolean,
    val message: String,
    val cookies: List<Cookie> = emptyList()
)

/**
 * 饼干信息
 */
@Serializable
data class Cookie(
    val id: String,
    val name: String,
    val value: String,
    val userHash: String,
    val isActive: Boolean
)
