package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ForumComment
import com.example.data.ForumPost
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.ElevatedArmor
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.UiState

@Composable
fun ForumScreen(
    currentUserId: String,
    postsState: UiState<List<ForumPost>>,
    selectedPostId: String?,
    commentsState: UiState<List<ForumComment>>,
    onCreatePost: (category: String, title: String, body: String) -> Unit,
    onToggleLike: (ForumPost) -> Unit,
    onOpenComments: (String?) -> Unit,
    onSubmitComment: (String, String) -> Unit
) {
    val categories = listOf("All", "Squad Up", "Loadouts", "Tournaments", "Feedback")
    var selectedCategory by remember { mutableStateOf("All") }
    var showNewPostDialog by remember { mutableStateOf(false) }

    var draftCategory by remember { mutableStateOf("Squad Up") }
    var draftTitle by remember { mutableStateOf("") }
    var draftBody by remember { mutableStateOf("") }
    var replyText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BHARAT COMMUNITY FORUM", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                    Text("Find Ranked Squads, Share Loadouts & Season Feedback", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Button(
                    onClick = { showNewPostDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp).testTag("new_forum_post_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("POST", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val selected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) BharatTeal else TacticalSlate)
                            .border(1.dp, if (selected) BharatTeal else CardBorder, RoundedCornerShape(50))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(cat, style = MaterialTheme.typography.labelMedium, color = if (selected) ObsidianBg else TextPrimary)
                    }
                }
            }
        }

        when (postsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SaffronGold)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(postsState.message, color = CrimsonStrike, style = MaterialTheme.typography.bodyMedium)
                }
            }
            is UiState.Success -> {
                val filtered = if (selectedCategory == "All") postsState.data else postsState.data.filter { it.category == selectedCategory }
                if (filtered.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No discussions yet in $selectedCategory", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Start the conversation — post your Jaipur Fort strategy or LFG squad call!", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { post ->
                        val liked = post.likedByUids.contains(currentUserId)
                        val isExpanded = selectedPostId == post.id
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, if (isExpanded) BharatTeal else CardBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(post.authorName, style = MaterialTheme.typography.labelLarge, color = SaffronGold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("[${post.authorRank}]", style = MaterialTheme.typography.labelSmall, color = BharatTeal)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElevatedArmor)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(post.category, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }

                                Text(post.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(post.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (liked) SaffronGold.copy(alpha = 0.2f) else ElevatedArmor)
                                            .clickable { onToggleLike(post) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.ThumbUp, contentDescription = "Upvote", tint = if (liked) SaffronGold else TextSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${post.likesCount}", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ElevatedArmor)
                                            .clickable { onOpenComments(if (isExpanded) null else post.id) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Replies", tint = BharatTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${post.commentsCount} Replies", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                    }
                                }

                                if (isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ObsidianBg)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        when (commentsState) {
                                            is UiState.Loading -> Text("Loading replies...", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            is UiState.Error -> Text(commentsState.message, style = MaterialTheme.typography.labelSmall, color = CrimsonStrike)
                                            is UiState.Success -> {
                                                if (commentsState.data.isEmpty()) {
                                                    Text("No replies yet. Be the first!", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                                } else {
                                                    commentsState.data.forEach { comment ->
                                                        Column {
                                                            Text(comment.authorName, style = MaterialTheme.typography.labelSmall, color = SaffronGold)
                                                            Text(comment.body, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = replyText,
                                                onValueChange = { replyText = it.take(380) },
                                                placeholder = { Text("Write reply...") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (replyText.isNotBlank()) {
                                                        onSubmitComment(post.id, replyText)
                                                        replyText = ""
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Reply", tint = SaffronGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }

    if (showNewPostDialog) {
        AlertDialog(
            onDismissRequest = { showNewPostDialog = false },
            containerColor = TacticalSlate,
            title = { Text("Create Community Post", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Squad Up", "Loadouts", "Tournaments", "Feedback")) { cat ->
                            val sel = draftCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) SaffronGold else ElevatedArmor)
                                    .clickable { draftCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cat, style = MaterialTheme.typography.labelSmall, color = if (sel) ObsidianBg else TextPrimary)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it.take(78) },
                        label = { Text("Headline (min 3 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftBody,
                        onValueChange = { draftBody = it.take(780) },
                        label = { Text("Discussion Details (min 5 chars)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftTitle.trim().length >= 3 && draftBody.trim().length >= 5) {
                            onCreatePost(draftCategory, draftTitle, draftBody)
                            draftTitle = ""
                            draftBody = ""
                            showNewPostDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBg)
                ) {
                    Text("PUBLISH")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPostDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}
