package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IceSkating
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Product
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory

data class MenuCategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

val storeCategoriesList = listOf(
    MenuCategoryItem(
        id = "todo",
        title = "Todo el Catálogo",
        subtitle = "Explora todas las piezas y colecciones",
        icon = Icons.Default.GridView
    ),
    MenuCategoryItem(
        id = "gorra",
        title = "Gorras & Boinas",
        subtitle = "Boinas Peaky, newsboys y sombreros tweed",
        icon = Icons.Default.Checkroom
    ),
    MenuCategoryItem(
        id = "gafa",
        title = "Gafas",
        subtitle = "Gafas de sol polarizadas y monturas vintage",
        icon = Icons.Default.Visibility
    ),
    MenuCategoryItem(
        id = "camisa",
        title = "Camisas & Trajes",
        subtitle = "Camisas algodón egipcio, chalecos y blazers",
        icon = Icons.Default.DryCleaning
    ),
    MenuCategoryItem(
        id = "correa",
        title = "Correas",
        subtitle = "Cinturones de cuero vacuno y hebillas artesanales",
        icon = Icons.Default.Straighten
    ),
    MenuCategoryItem(
        id = "pantalone",
        title = "Pantalones",
        subtitle = "Pantalones de vestir sartoriales y chinos",
        icon = Icons.Default.Accessibility
    ),
    MenuCategoryItem(
        id = "zapato",
        title = "Zapatos",
        subtitle = "Oxfords cap-toe, brogues y mocasines",
        icon = Icons.Default.IceSkating
    ),
    MenuCategoryItem(
        id = "interiore",
        title = "Ropa Interior",
        subtitle = "Bóxers micro-modal y camisetas confort",
        icon = Icons.Default.Layers
    ),
    MenuCategoryItem(
        id = "perfume",
        title = "Perfumes",
        subtitle = "Fragancias de autor y extractos exclusivos",
        icon = Icons.Default.Spa
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    products: List<Product>,
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.icono),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Menú de Categorías",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("menu_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EleganCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EleganIvory
                )
            )
        },
        containerColor = EleganIvory
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .testTag("menu_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Selecciona una categoría para explorar prendas seleccionadas y accesorios:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(storeCategoriesList) { catItem ->
                val count = if (catItem.id == "todo") {
                    products.size
                } else {
                    products.count { it.category.equals(catItem.id, ignoreCase = true) }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCategorySelected(catItem.id) }
                        .testTag("menu_item_${catItem.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (catItem.id == "todo") EleganBurgundy else Color(0xFFF6EFE9)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = catItem.icon,
                                contentDescription = catItem.title,
                                tint = if (catItem.id == "todo") Color.White else EleganBurgundy,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = catItem.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF2EBE5)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundyDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = catItem.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ir a categoría",
                            tint = EleganGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
