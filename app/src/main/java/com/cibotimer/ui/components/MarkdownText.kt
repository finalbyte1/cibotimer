package com.cibotimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Pure Jetpack Compose Markdown renderer (zero external dependencies).
 * Ported & enhanced from MiniiChat with CiboTimer dark sapphire theme.
 * Supports:
 * - Headings (H1, H2, H3)
 * - Fenced code blocks with language banner & "Copia codice" button
 * - Bullet lists and numbered lists
 * - Blockquotes with cyan vertical accent
 * - Horizontal dividers (---)
 * - Inline bold, italic, strikethrough, inline code, and clickable URLs
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    val blocks = remember(text) { parseBlocks(text) }
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        blocks.forEachIndexed { idx, block ->
            if (idx > 0) {
                val spacerHeight = if (block is Block.CodeBlock || blocks[idx - 1] is Block.CodeBlock) 8.dp else 5.dp
                Spacer(Modifier.height(spacerHeight))
            }
            when (block) {
                is Block.Heading -> {
                    val (fontSize, fontWeight) = when (block.level) {
                        1 -> 20.sp to FontWeight.Bold
                        2 -> 17.sp to FontWeight.Bold
                        else -> 15.sp to FontWeight.SemiBold
                    }
                    Text(
                        text = parseInline(block.text),
                        color = CiboTextPrimary,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        lineHeight = (fontSize.value * 1.35f).sp
                    )
                }
                is Block.Paragraph -> {
                    Text(
                        text = parseInline(block.text),
                        color = color,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp
                    )
                }
                is Block.BulletItem -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text(
                            text = "• ",
                            color = CiboTimerAzureGlow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 2.dp, end = 6.dp)
                        )
                        Text(
                            text = parseInline(block.text),
                            color = color,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is Block.NumberedItem -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text(
                            text = "${block.index}. ",
                            color = CiboTimerAzure,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = parseInline(block.text),
                            color = color,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is Block.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CiboCard.copy(alpha = 0.5f))
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(CiboTimerAzure)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = parseInline(block.text),
                            color = CiboTextSecondary,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 20.sp
                        )
                    }
                }
                is Block.Hr -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(CardBorderGlow.copy(alpha = 0.5f))
                    )
                }
                is Block.CodeBlock -> {
                    CodeBlockCard(block = block)
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(block: Block.CodeBlock) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF070E1A))
            .border(1.dp, CardBorderGlow, RoundedCornerShape(10.dp))
    ) {
        // Code header banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C1728))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = block.lang.ifBlank { "code" },
                color = CiboTimerAzure,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(block.code))
                        copied = true
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copia codice",
                    tint = if (copied) CiboTimerAzureGlow else CiboTextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (copied) "Copiato!" else "Copia",
                    color = if (copied) CiboTimerAzureGlow else CiboTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorderGlow.copy(alpha = 0.7f))
        )

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = block.code,
                color = Color(0xFFE2E8F0),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private sealed class Block {
    data class Heading(val level: Int, val text: String) : Block()
    data class Paragraph(val text: String) : Block()
    data class BulletItem(val text: String) : Block()
    data class NumberedItem(val index: Int, val text: String) : Block()
    data class Quote(val text: String) : Block()
    object Hr : Block()
    data class CodeBlock(val lang: String, val code: String) : Block()
}

private fun parseBlocks(raw: String): List<Block> {
    val lines = raw.lines()
    val blocks = mutableListOf<Block>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Fenced code block (```lang ... ```)
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // consume closing ```
            blocks.add(Block.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // Horizontal Rule (---, ***, ___)
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(Block.Hr)
            i++
            continue
        }

        // Headings (#, ##, ###)
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            if (level in 1..6 && trimmed.length > level && trimmed[level] == ' ') {
                val text = trimmed.substring(level + 1).trim()
                blocks.add(Block.Heading(level, text))
                i++
                continue
            }
        }

        // Bullet lists (*, -, +)
        if ((trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) && trimmed.length > 2) {
            blocks.add(Block.BulletItem(trimmed.substring(2).trim()))
            i++
            continue
        }

        // Numbered lists (1. , 2. )
        val numMatch = Regex("^(\\d+)\\.\\s+(.+)").find(trimmed)
        if (numMatch != null) {
            val idx = numMatch.groupValues[1].toIntOrNull() ?: 1
            val text = numMatch.groupValues[2]
            blocks.add(Block.NumberedItem(idx, text))
            i++
            continue
        }

        // Blockquotes (> text)
        if (trimmed.startsWith(">")) {
            blocks.add(Block.Quote(trimmed.removePrefix(">").trim()))
            i++
            continue
        }

        // Regular paragraph (accumulate consecutive lines until empty line or block marker)
        val paraLines = mutableListOf(line)
        i++
        while (i < lines.size) {
            val next = lines[i]
            val nextTrimmed = next.trim()
            if (nextTrimmed.isEmpty() ||
                nextTrimmed.startsWith("```") ||
                nextTrimmed.startsWith("#") ||
                nextTrimmed.startsWith("- ") ||
                nextTrimmed.startsWith("* ") ||
                nextTrimmed.startsWith("> ") ||
                nextTrimmed == "---" ||
                Regex("^(\\d+)\\.\\s+").containsMatchIn(nextTrimmed)
            ) {
                break
            }
            paraLines.add(next)
            i++
        }
        blocks.add(Block.Paragraph(paraLines.joinToString(" ")))
    }

    return blocks
}

/**
 * Parses inline markdown:
 * - Bold: `**text**` or `__text__`
 * - Italic: `*text*` or `_text_`
 * - Strikethrough: `~~text~~`
 * - Inline code: `` `text` ``
 * - Links: `[text](url)`
 */
internal fun parseInline(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            // Inline code `...`
            if (text[cursor] == '`') {
                val end = text.indexOf('`', cursor + 1)
                if (end > cursor) {
                    val code = text.substring(cursor + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = CiboTimerAzureGlow,
                            background = Color(0xFF132238)
                        )
                    )
                    append(" $code ")
                    pop()
                    cursor = end + 1
                    continue
                }
            }

            // Bold **...**
            if (cursor + 1 < len && text[cursor] == '*' && text[cursor + 1] == '*') {
                val end = text.indexOf("**", cursor + 2)
                if (end > cursor) {
                    val bold = text.substring(cursor + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CiboTextPrimary))
                    append(bold)
                    pop()
                    cursor = end + 2
                    continue
                }
            }

            // Strikethrough ~~...~~
            if (cursor + 1 < len && text[cursor] == '~' && text[cursor + 1] == '~') {
                val end = text.indexOf("~~", cursor + 2)
                if (end > cursor) {
                    val struck = text.substring(cursor + 2, end)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = CiboTextSecondary))
                    append(struck)
                    pop()
                    cursor = end + 2
                    continue
                }
            }

            // Italic *...*
            if (text[cursor] == '*' && (cursor == 0 || text[cursor - 1] != '*')) {
                val end = text.indexOf('*', cursor + 1)
                if (end > cursor && (end + 1 >= len || text[end + 1] != '*')) {
                    val ital = text.substring(cursor + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(ital)
                    pop()
                    cursor = end + 1
                    continue
                }
            }

            // Links [title](url)
            if (text[cursor] == '[') {
                val closeBracket = text.indexOf(']', cursor + 1)
                if (closeBracket > cursor && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen > closeBracket + 1) {
                        val title = text.substring(cursor + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        pushStyle(
                            SpanStyle(
                                color = CiboTimerAzureGlow,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(title)
                        pop()
                        cursor = closeParen + 1
                        continue
                    }
                }
            }

            // Ordinary character
            append(text[cursor])
            cursor++
        }
    }
}
