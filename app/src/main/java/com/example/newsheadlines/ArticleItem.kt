package com.example.newsheadlines.components

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsheadlines.DisplayNewsActivity
import com.example.newsheadlines.NewsArticle
import android.util.Base64

@Composable
fun ArticleItem(article: NewsArticle) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, DisplayNewsActivity::class.java).apply {
                    putExtra("articleId", article.id) // Only pass the articleId
                }
                context.startActivity(intent)
            }
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        elevation = 6.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!article.urlToImage.isNullOrEmpty()) {
                val bitmap = remember(article.urlToImage) {
                    try {
                        val decodedBytes = Base64.decode(article.urlToImage, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "News Image: ${article.title}",
                        modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.description,
                color = MaterialTheme.colors.onSurface,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "By ${article.author} | ${article.category}",
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}