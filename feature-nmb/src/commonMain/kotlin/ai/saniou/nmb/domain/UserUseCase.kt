package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.Cookie
import ai.saniou.nmb.data.entity.LoginRequest
import ai.saniou.nmb.data.entity.LoginResponse
import ai.saniou.nmb.data.repository.ForumRepository

class UserUseCase(
    private val forumRepository: ForumRepository
) {
    /**
     * 用户登录
     */
    suspend fun login(email: String, password: String, verify: String): LoginResponse {
        return try {
            val request = LoginRequest(
                email = email,
                password = password,
                verify = verify
            )
            forumRepository.login(request)
        } catch (e: Exception) {
            // 可以添加日志记录
            // Logger.e("UserUseCase login Exception ${e.message}")
            LoginResponse(
                success = false,
                message = "登录失败: ${e.message}"
            )
        }
    }
    
    /**
     * 获取用户饼干列表
     */
    suspend fun getCookiesList(): List<Cookie> {
        return try {
            val response = forumRepository.getCookiesList()
            if (response.success) {
                response.cookies
            } else {
                throw RuntimeException(response.message)
            }
        } catch (e: Exception) {
            // 可以添加日志记录
            // Logger.e("UserUseCase getCookiesList Exception ${e.message}")
            throw e
        }
    }
    
    /**
     * 申请新饼干
     */
    suspend fun applyNewCookie(): String {
        return try {
            forumRepository.applyNewCookie()
        } catch (e: Exception) {
            // 可以添加日志记录
            // Logger.e("UserUseCase applyNewCookie Exception ${e.message}")
            "申请饼干失败: ${e.message}"
        }
    }
    
    /**
     * 注册账号
     */
    suspend fun register(
        email: String,
        password: String,
        passwordConfirm: String,
        verify: String
    ): String {
        return try {
            forumRepository.register(email, password, passwordConfirm, verify)
        } catch (e: Exception) {
            // 可以添加日志记录
            // Logger.e("UserUseCase register Exception ${e.message}")
            "注册失败: ${e.message}"
        }
    }
    
    /**
     * 重置密码
     */
    suspend fun resetPassword(email: String, verify: String): String {
        return try {
            forumRepository.resetPassword(email, verify)
        } catch (e: Exception) {
            // 可以添加日志记录
            // Logger.e("UserUseCase resetPassword Exception ${e.message}")
            "重置密码失败: ${e.message}"
        }
    }
}
