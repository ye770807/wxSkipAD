package cc.ahaly.wxskipad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import cc.ahaly.wxskipad.ui.theme.WxSkipADTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var logReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册广播接收器
        logReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                var logtag = intent.getStringExtra("log_tag")
                val logMessage = intent.getStringExtra("log_message") ?: "无日志内容"
                // 更新日志内容（通过Compose）
                appendLog(logtag + "\t" + logMessage)
            }
        }

        // 注册广播接收器，监听cc.ahaly.wxskipad.LOG_BROADCAST广播
        val intentFilter = IntentFilter("cc.ahaly.wxskipad.LOG_BROADCAST")
        registerReceiver(logReceiver, intentFilter)

        // 加载Compose UI
        setContent {
            WxSkipADTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LogScreen() // 显示日志界面
                }
            }
        }
    }

    // 在Activity销毁时，注销广播接收器
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(logReceiver)
    }

    companion object {
        // 定义一个存储日志内容的变量
        var currentLog = mutableStateOf("")

        // 追加日志内容的方法
        fun appendLog(log: String) {
            currentLog.value += log + "\n" // 追加日志，而不是替换
        }
    }
}

@Composable
fun LogScreen() {
    val context = LocalContext.current // 获取当前的上下文
    val logText by MainActivity.currentLog // 通过状态值观察日志内容
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 日志显示区域，支持滚动
        Text(
            text = logText,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )

        // 确保页面自动滚动到底部
        LaunchedEffect(logText) {
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 清空日志按钮
            Button(onClick = {
                MainActivity.currentLog.value = "日志接受中...\n" // 清空日志
                Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show()
            }) {
                Text(text = "清空日志")
            }

            // 复制日志按钮
            Button(onClick = {
                clipboardManager.setText(AnnotatedString(logText)) // 复制日志内容到剪贴板
                Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT).show()
            }) {
                Text(text = "复制日志")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 发送测试广播按钮
        Button(onClick = {
            // 手动发送调试广播
            val intent = Intent("cc.ahaly.wxskipad.LOG_BROADCAST")
            intent.putExtra("log_tag", "信息\t")
            intent.putExtra("log_message", "这是一条调试日志消息")
            context.sendBroadcast(intent)
            Toast.makeText(context, "测试广播已发送", Toast.LENGTH_SHORT).show()
        }) {
            Text(text = "发送测试广播")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LogScreenPreview() {
    WxSkipADTheme {
        LogScreen()
    }
}
