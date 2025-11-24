package ai.saniou.nmb.workflow.post

import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.direct
import org.kodein.di.instance

data class PostPage(
    val fid: Int? = null,
    val resto: Int? = null,
    val forumName: String? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: PostViewModel = rememberScreenModel(tag = "${fid}_${resto}") {
            nmbdi.direct.instance(arg = Triple(fid, resto, forumName))
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is PostContract.Effect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                    PostContract.Effect.NavigateBack -> navigator.pop()
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (resto != null) "回复" else "发帖: ${state.forumName}") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (resto == null) { // Only show for new posts
                    OutlinedTextField(
                        value = state.postBody.name ?: "",
                        onValueChange = { viewModel.onEvent(PostContract.Event.UpdateName(it)) },
                        label = { Text("名称 (可选)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.postBody.title ?: "",
                        onValueChange = { viewModel.onEvent(PostContract.Event.UpdateTitle(it)) },
                        label = { Text("标题 (可选)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = state.content,
                    onValueChange = { viewModel.onEvent(PostContract.Event.UpdateContent(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("内容") },
                    isError = state.error != null
                )
                PostToolbar(viewModel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { /* TODO: Implement image picker */ }) {
                        Icon(Icons.Default.Info, contentDescription = "选择图片")
                        Text("图片")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.water,
                            onCheckedChange = { viewModel.onEvent(PostContract.Event.ToggleWater(it)) }
                        )
                        Text("水印")
                    }
                }
                Button(
                    onClick = { viewModel.onEvent(PostContract.Event.Submit) },
                    enabled = state.content.text.isNotBlank() && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                    Text("发送")
                }
            }
        }
    }
}

@Composable
private fun PostToolbar(viewModel: PostViewModel) {
    var showEmoticonPicker by remember { mutableStateOf(false) }
    var showDiceDialog by remember { mutableStateOf(false) }

    if (showEmoticonPicker) {
        EmoticonPickerDialog(
            onDismiss = { showEmoticonPicker = false },
            onEmoticonSelected = {
                viewModel.onEvent(PostContract.Event.InsertContent(it))
                showEmoticonPicker = false
            }
        )
    }

    if (showDiceDialog) {
        DiceInputDialog(
            onDismiss = { showDiceDialog = false },
            onConfirm = { start, end ->
                viewModel.onEvent(PostContract.Event.InsertContent("[$start-$end]"))
                showDiceDialog = false
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = {
            viewModel.onEvent(PostContract.Event.InsertContent("[code][/code]"))
        }) {
            Icon(Icons.Outlined.Info, contentDescription = "插入代码")
        }
        IconButton(onClick = {
            viewModel.onEvent(PostContract.Event.InsertContent("[img][/img]"))
        }) {
            Icon(Icons.Outlined.Info, contentDescription = "插入图片")
        }
        IconButton(onClick = { showEmoticonPicker = true }) {
            Icon(Icons.Outlined.Info, contentDescription = "表情")
        }
        IconButton(onClick = { showDiceDialog = true }) {
            Icon(Icons.Outlined.Info, contentDescription = "掷骰子")
        }
    }
}

@Composable
private fun DiceInputDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var start by remember { mutableStateOf("1") }
    var end by remember { mutableStateOf("100") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("掷骰子") },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("起点") },
                    modifier = Modifier.weight(1f)
                )
                Text("-")
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("终点") },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(start, end) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EmoticonPickerDialog(onDismiss: () -> Unit, onEmoticonSelected: (String) -> Unit) {
    val emoticonGroups = remember {
        mapOf(
            "常用" to listOf(
                "(=ﾟωﾟ)=", "(´ﾟДﾟ`)", "(｀･ω･)", "( ´_ゝ｀)", "(;´Д`)",
                "(=Д=)", "(●ε●)", "( ´∀`)", "( ´∀｀)", "(*´∀`)",
                "(｡◕∀◕｡)", "(ゝ∀･)", "(ノﾟ∀ﾟ)ノ", "(σﾟдﾟ)σ", "Σ( ﾟдﾟ)",
                "|д` )", "(`ε´ )", "(╬ﾟдﾟ)", "(|||ﾟдﾟ)", "( ﾟ∀ﾟ)",
                "(*´д`)", "( `д´)", "(`ヮ´ )", "( ´ー`)", "( ´_っ`)",
                "( ´ρ`)", "(･ω･)", "(ﾟДﾟ≡ﾟДﾟ)"
            ),
            "颜文字" to listOf(
                "|∀ﾟ", "| ω・´)", "|-` )", "|д` )", "|ー` )", "|∀` )", "( ´д`)", "(( ´д`))",
                "( ´∀`)", "( ´∀｀)", "(*´∀`)", "(*ﾟ∇ﾟ)", "(｡◕∀◕｡)", "( ´ρ`)", "(ゝ∀･)",
                "( ´_ゝ｀)", "( ´_っ`)", "( ´σ`)", "( ´∀｀)σ", "( ´∀`)ノ", "( ´д`)ノ",
                "( ´д)ノ", "( ´ρ`)ノ", "( ﾟдﾟ)ノ", "( ﾟдﾟ)σ", "( ﾟдﾟ)", "( ;ﾟдﾟ)",
                "( ;´д`)", "( ;´ρ`)", "( ;´∀`)", "( `д´)", "( `д´)σ", "( `д´)ノ",
                "( `д´)ﾉ", "(#`д´)ﾉ", "(#`д´)σ", "(#`д´)!!", "(#`д´)凸", "(╬`д´)σ",
                "(╬`д´)", "(╬`д´)ノ", "(╬`д´)ﾉ", "(╬ﾟдﾟ)", "(╬ﾟдﾟ)σ", "(╬ﾟдﾟ)ノ",
                "(╬ﾟдﾟ)ﾉ", "(|||ﾟдﾟ)", "( ﾟ∀ﾟ)", "( ﾟ∀ﾟ)σ", "( ﾟ∀ﾟ)ノ", "( ﾟ∀ﾟ)ﾉ",
                "(σﾟ∀ﾟ)σ", "(σﾟдﾟ)σ", "(σ´д`)σ", "(σ´∀`)σ", "Σ( ﾟдﾟ)", "Σ( ﾟдﾟ;)",
                "Σ( `д´)", "Σ( `д´;)", "(((( ;ﾟдﾟ)))", "(((　ﾟдﾟ)))", "( `ヮ´ )",
                "(*ﾟーﾟ)", "(⌒∇⌒*)", "(*´ω`*)", "(´ω`)", "(´ω｀*)", "(n´ω`n)",
                "(´∀｀*)", "(* ´∀`)", "(*´∀｀*)", "(* ´∀｀)", "(*ﾉ∀`*)", "(*ﾉωﾉ)",
                "(*ﾉдﾉ)", "(*´д`*)", "(*´д`)", "(*´ρ`*)", "(´Д`*)", "(´Д`)",
                "(´Д｀*)", "(;´Д`)", "(ι´Д`)", "(ヽ´Д`)", "(ノ´Д`)", "( #´Д`)",
                "( ´Д`)y━･~~", "( ´д`)y━･~~", "( ´_ゝ`)y━･~~", "( ´ρ`)y━･~~", "（ ´,_ゝ`)",
                "( ´,_ゝ`)", "（ ´∀`）", "( ´∀`)", "（ ´_ゝ`）", "( ´_ゝ`)", "（ ´ρ`）",
                "( ´ρ`)", "（ `д´）", "( `д´)", "（`ヮ´ ）", "(`ヮ´ )", "(｀･ω･)",
                "(´･ω･`)", "(･ω･)", "(=ﾟωﾟ)=", "(=ﾟωﾟ)ﾉ", "(=´∀`)", "(´∀`)",
                "(´∀｀)", "(=´∀｀)人(´∀｀=)", "( ´∀｀)人(´∀｀ )", "( ´∀`)人(`Д´ )",
                "(・∀・)", "(・∀・)ノ", "(・∀・)ﾉ", "（・∀・）", "（・∀・）", "（・∀・）ノ",
                "（・∀・）ﾉ", "(ノ∀`)", "(ノ∀｀)σ", "(σ´∀`)σ", "(σ´∀`)", "(´ﾟДﾟ`)",
                "(;ﾟДﾟ`)", "(´ﾟдﾟ`)", "(;ﾟдﾟ`)"
            )
        )
    }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val titles = emoticonGroups.keys.toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择表情") },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 50.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(emoticonGroups.values.toList()[selectedTabIndex]) { emoticon ->
                            Text(
                                text = emoticon,
                                modifier = Modifier
                                    .clickable { onEmoticonSelected(emoticon) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
