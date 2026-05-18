package com.aasra.wallberry

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.aasra.wallberry.ui.theme.*
import java.io.File
import java.io.IOException
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallberryTheme {
                WallberryApp()
            }
        }
    }
}

enum class Screen {
    Home, Saved, Settings
}

@Composable
fun WallberryApp() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var savedImages by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        bottomBar = { 
            WallberryBottomNavigation(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            ) 
        },
        containerColor = WarmCream
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onSaveImage = { uri -> savedImages = savedImages + uri.toString() }
                )
                Screen.Saved -> SavedScreen(
                    savedImages = savedImages.toList(),
                    onRemoveImage = { url -> savedImages = savedImages - url }
                )
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(onSaveImage: (Uri) -> Unit) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scrollState = rememberScrollState()

    val imageFile = remember {
        File(context.externalCacheDir, "camera_image.jpg").apply {
            if (exists()) delete()
            createNewFile()
        }
    }
    val cameraUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri = cameraUri
        }
    }

    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            try {
                bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(title = "Wallberry", subtitle = "Your Personalized Aesthetic Hub")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        PreviewSection(
            imageUri = selectedImageUri,
            onFavoriteClick = { selectedImageUri?.let { onSaveImage(it) } }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionButton(
                text = "Camera",
                icon = Icons.Default.CameraAlt,
                backgroundColor = LightCoral,
                modifier = Modifier.weight(1f),
                onClick = { cameraLauncher.launch(cameraUri) }
            )
            ActionButton(
                text = "Gallery",
                icon = Icons.Default.Collections,
                backgroundColor = CoralPink,
                modifier = Modifier.weight(1f),
                onClick = { galleryLauncher.launch("image/*") }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PrimaryButton(
            text = "✨ Set as Wallpaper ✨",
            enabled = selectedImageUri != null,
            onClick = {
                bitmap?.let {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    try {
                        wallpaperManager.setBitmap(it)
                        Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        Toast.makeText(context, "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, 
                        ComponentName(context, WallberryWallpaperService::class.java))
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, LightCoral)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LightCoral)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Live Wallpaper ✨", color = PrimaryText, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        InfoSection()
    }
}

@Composable
fun SavedScreen(savedImages: List<String>, onRemoveImage: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HeaderSection(title = "Saved", subtitle = "Your favorite picks")
        Spacer(modifier = Modifier.height(16.dp))
        if (savedImages.isEmpty()) {
            Spacer(modifier = Modifier.height(100.dp))
            Icon(Icons.Default.Favorite, null, tint = SoftPink, modifier = Modifier.size(120.dp))
            Text("No saved wallpapers yet", style = MaterialTheme.typography.bodyLarge, color = SecondaryText, textAlign = TextAlign.Center)
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(savedImages) { urlOrUri ->
                    Box(modifier = Modifier.aspectRatio(0.7f).clip(RoundedCornerShape(12.dp)).border(1.dp, SoftPink, RoundedCornerShape(12.dp))) {
                        AsyncImage(model = urlOrUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(onClick = { onRemoveImage(urlOrUri) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.White.copy(alpha = 0.6f), CircleShape).size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = RosePink, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HeaderSection(title = "Settings", subtitle = "App preferences")
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = InfoBG), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsItem(Icons.Default.Palette, "Theme Colors")
                SettingsItem(Icons.Default.Movie, "Live Wallpaper Settings")
                SettingsItem(Icons.Default.Info, "About Wallberry")
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { }, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = CoralPink)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = PrimaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = SecondaryText)
    }
}

@Composable
fun HeaderSection(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            FlowerIcon(LightPink)
            Text(title, style = MaterialTheme.typography.headlineLarge, color = CoralPink, fontWeight = FontWeight.Bold)
            FlowerIcon(LightPink)
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
    }
}

@Composable
fun PreviewSection(imageUri: Uri?, onFavoriteClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp)).background(SoftPink).border(BorderStroke(2.dp, InfoBG), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        if (imageUri != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(model = imageUri, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                IconButton(onClick = onFavoriteClick, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.White.copy(alpha = 0.7f), CircleShape)) {
                    Icon(Icons.Outlined.FavoriteBorder, null, tint = LightCoral)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Choose your image", color = SecondaryText, fontWeight = FontWeight.Medium)
                Text("📷 or 🖼️", fontSize = 24.sp)
            }
        }
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Icon(Icons.Default.Favorite, null, tint = LightCoral, modifier = Modifier.align(Alignment.BottomStart).size(16.dp))
            Icon(Icons.Default.Favorite, null, tint = LightCoral, modifier = Modifier.align(Alignment.BottomEnd).size(16.dp))
        }
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, backgroundColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(70.dp), colors = ButtonDefaults.buttonColors(containerColor = backgroundColor), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(70.dp), colors = ButtonDefaults.buttonColors(containerColor = RosePink, disabledContainerColor = RosePink.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, DarkCoral)) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun InfoSection() {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(InfoBG).border(BorderStroke(1.dp, SoftPink), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🎨 App Features", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Icon(Icons.Default.Eco, null, tint = FreshGreen.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("✓ Capture photos with camera", color = PrimaryText, fontSize = 12.sp)
        Text("✓ Select from gallery storage", color = PrimaryText, fontSize = 12.sp)
    }
}

@Composable
fun WallberryBottomNavigation(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White, modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
        NavigationBarItem(selected = currentScreen == Screen.Home, onClick = { onScreenSelected(Screen.Home) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = CoralPink, selectedTextColor = CoralPink, unselectedIconColor = SecondaryText, unselectedTextColor = SecondaryText, indicatorColor = WarmCream))
        NavigationBarItem(selected = currentScreen == Screen.Saved, onClick = { onScreenSelected(Screen.Saved) }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Saved") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = CoralPink, selectedTextColor = CoralPink, unselectedIconColor = SecondaryText, unselectedTextColor = SecondaryText, indicatorColor = WarmCream))
        NavigationBarItem(selected = currentScreen == Screen.Settings, onClick = { onScreenSelected(Screen.Settings) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = CoralPink, selectedTextColor = CoralPink, unselectedIconColor = SecondaryText, unselectedTextColor = SecondaryText, indicatorColor = WarmCream))
    }
}

@Composable
fun FlowerIcon(tint: Color) {
    Icon(imageVector = Icons.Default.FilterVintage, null, tint = tint, modifier = Modifier.size(24.dp))
}
