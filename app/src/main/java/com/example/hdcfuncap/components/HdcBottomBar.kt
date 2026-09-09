package com.example.hdcfuncap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HdcBottomBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val azulHdc = Color(0xFF6B9DFE)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp)
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            BottomNavItem(
                icon = Icons.Outlined.Home, label = "Início", isSelected = currentScreen == "home", color = azulHdc, onClick = { onNavigate("home") }
            )
            BottomNavItem(
                icon = Icons.Outlined.Description, label = "Prescrições", isSelected = currentScreen == "prescricoes", color = azulHdc, onClick = { onNavigate("prescricoes") }
            )

            Spacer(modifier = Modifier.width(70.dp))

            BottomNavItem(
                icon = Icons.Outlined.Schedule, label = "Histórico", isSelected = currentScreen == "historico", color = azulHdc, onClick = { onNavigate("historico") }
            )
            BottomNavItem(
                icon = Icons.Outlined.Person, label = "Perfil", isSelected = currentScreen == "perfil", color = azulHdc, onClick = { onNavigate("perfil") }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .clickable { onNavigate("registrar") }
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(8.dp, CircleShape)
                    .background(azulHdc, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Registrar",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val corTextoRegistrar = if (currentScreen == "registrar") azulHdc else Color.Gray

            Text(
                text = "Registrar",
                fontSize = 12.sp,
                color = corTextoRegistrar
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val corItem = if (isSelected) color else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = corItem,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = corItem
        )
    }
}