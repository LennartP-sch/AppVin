package com.appvin.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvin.data.ChatMessage
import com.appvin.data.parseSources

private val starterQuestions = listOf(
    "What should I drink tonight?",
    "We're having steak – which bottle?",
    "Which wines should I drink soon?",
    "Suggest a wine for a special occasion",
)

@Composable
fun ChatScreen(vm: CellarViewModel) {
    val messages by vm.chatMessages.collectAsStateWithLifecycle()
    val busy by vm.chatBusy.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size + 1)
    }

    fun send(text: String) {
        if (text.isBlank() || busy) return
        vm.sendChat(text.trim())
        input = ""
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sommelier", style = MaterialTheme.typography.headlineMedium)
                Text("Knows your cellar · searches the web", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (messages.isNotEmpty()) {
                IconButton(onClick = vm::clearChat) { Icon(Icons.Filled.DeleteSweep, "Start a new conversation") }
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                if (messages.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ask me anything about your wines. For example:", style = MaterialTheme.typography.bodyLarge)
                        starterQuestions.forEach { q -> AssistChip(onClick = { send(q) }, label = { Text(q) }) }
                    }
                }
            }
            items(messages, key = { it.id }) { message -> MessageBubble(message) }
            item {
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(12.dp))
                        Text("Thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("What would you like to drink?") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                maxLines = 4,
            )
            Spacer(Modifier.size(8.dp))
            FilledIconButton(onClick = { send(input) }, enabled = input.isNotBlank() && !busy, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val mine = message.fromUser
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Surface(
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (mine) 20.dp else 4.dp, bottomEnd = if (mine) 4.dp else 20.dp,
            ),
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Text(renderMarkdown(message.text), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
        }
        val sources = parseSources(message.sources)
        if (sources.isNotEmpty()) {
            val uriHandler = LocalUriHandler.current
            Column(Modifier.padding(start = 8.dp, top = 6.dp)) {
                Text("Sources", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sources.take(5).forEach { (title, url) ->
                    Text(
                        title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp).clickable { uriHandler.openUri(url) },
                    )
                }
            }
        }
        message.searchSuggestionsHtml?.let { SearchSuggestions(it) }
    }
}

/** Google requires showing these "Search suggestions" alongside answers grounded with Google Search. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SearchSuggestions(html: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        return true
                    }
                }
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 6.dp),
    )
}

/** Just enough Markdown for chat replies: **bold**, bullet lists and headings. */
private fun renderMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, rawLine ->
        if (index > 0) append("\n")
        var line = rawLine
        when {
            line.trimStart().startsWith("* ") || line.trimStart().startsWith("- ") -> {
                val indent = line.length - line.trimStart().length
                line = " ".repeat(indent) + "• " + line.trimStart().drop(2)
            }
            line.startsWith("#") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line.trimStart('#', ' ')) }
                return@forEachIndexed
            }
        }
        val parts = line.split("**")
        parts.forEachIndexed { i, part ->
            if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
        }
    }
}
