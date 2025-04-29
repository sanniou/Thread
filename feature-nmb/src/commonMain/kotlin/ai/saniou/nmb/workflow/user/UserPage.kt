package ai.saniou.nmb.workflow.user

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.data.entity.Cookie
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPage(
    onNavigateBack: () -> Unit,
    di: DI = nmbdi
) {
    val userViewModel: UserViewModel = viewModel {
        val viewModel by di.instance<UserViewModel>()
        viewModel
    }
    
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户中心") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is UserUiState.CookieList) {
                FloatingActionButton(
                    onClick = { userViewModel.applyNewCookie() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "申请新饼干")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            // 标签页
            TabRow(
                selectedTabIndex = userViewModel.currentTabIndex
            ) {
                listOf("饼干管理", "登录", "注册").forEachIndexed { index, title ->
                    Tab(
                        selected = userViewModel.currentTabIndex == index,
                        onClick = { userViewModel.setCurrentTabIndex(index) },
                        text = { Text(title) }
                    )
                }
            }
            
            // 内容区域
            when (val state = uiState) {
                is UserUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载中...")
                    }
                }
                is UserUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("错误: ${state.message}")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { userViewModel.retry() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                is UserUiState.CookieList -> {
                    CookieListContent(
                        cookies = state.cookies,
                        onRefresh = { userViewModel.refreshCookies() }
                    )
                }
                is UserUiState.Login -> {
                    LoginContent(
                        email = state.email,
                        password = state.password,
                        verifyCode = state.verifyCode,
                        verifyImageUrl = state.verifyImageUrl,
                        onEmailChanged = { userViewModel.updateEmail(it) },
                        onPasswordChanged = { userViewModel.updatePassword(it) },
                        onVerifyCodeChanged = { userViewModel.updateVerifyCode(it) },
                        onRefreshVerifyCode = { userViewModel.refreshVerifyCode() },
                        onLogin = { userViewModel.login() }
                    )
                }
                is UserUiState.Register -> {
                    RegisterContent(
                        email = state.email,
                        password = state.password,
                        passwordConfirm = state.passwordConfirm,
                        verifyCode = state.verifyCode,
                        verifyImageUrl = state.verifyImageUrl,
                        onEmailChanged = { userViewModel.updateEmail(it) },
                        onPasswordChanged = { userViewModel.updatePassword(it) },
                        onPasswordConfirmChanged = { userViewModel.updatePasswordConfirm(it) },
                        onVerifyCodeChanged = { userViewModel.updateVerifyCode(it) },
                        onRefreshVerifyCode = { userViewModel.refreshVerifyCode() },
                        onRegister = { userViewModel.register() }
                    )
                }
            }
        }
    }
}

@Composable
fun CookieListContent(
    cookies: List<Cookie>,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "我的饼干",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (cookies.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("没有饼干，点击右下角按钮申请新饼干")
            }
        } else {
            LazyColumn {
                items(cookies) { cookie ->
                    CookieItem(cookie)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CookieItem(cookie: Cookie) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cookie.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                if (cookie.isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = if (cookie.isActive) "有效" else "无效",
                    color = if (cookie.isActive) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "饼干值: ${cookie.value}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "用户标识: ${cookie.userHash}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LoginContent(
    email: String,
    password: String,
    verifyCode: String,
    verifyImageUrl: String,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onVerifyCodeChanged: (String) -> Unit,
    onRefreshVerifyCode: () -> Unit,
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text("邮箱") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = verifyCode,
                onValueChange = onVerifyCodeChanged,
                label = { Text("验证码") },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 验证码图片
            Box(
                modifier = Modifier
                    .size(120.dp, 40.dp)
                    .background(Color.LightGray)
                    .clickable { onRefreshVerifyCode() }
            ) {
                // 这里应该显示验证码图片
                Text(
                    text = "点击刷新",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("登录")
        }
    }
}

@Composable
fun RegisterContent(
    email: String,
    password: String,
    passwordConfirm: String,
    verifyCode: String,
    verifyImageUrl: String,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordConfirmChanged: (String) -> Unit,
    onVerifyCodeChanged: (String) -> Unit,
    onRefreshVerifyCode: () -> Unit,
    onRegister: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text("邮箱") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = passwordConfirm,
            onValueChange = onPasswordConfirmChanged,
            label = { Text("确认密码") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = verifyCode,
                onValueChange = onVerifyCodeChanged,
                label = { Text("验证码") },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 验证码图片
            Box(
                modifier = Modifier
                    .size(120.dp, 40.dp)
                    .background(Color.LightGray)
                    .clickable { onRefreshVerifyCode() }
            ) {
                // 这里应该显示验证码图片
                Text(
                    text = "点击刷新",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("注册")
        }
    }
}

@Preview
@Composable
fun UserPagePreview() {
    MaterialTheme {
        // 预览内容
    }
}
