package com.satwik.oodapplication.presentation.admin

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.presentation.common.components.shimmerModifier
import com.satwik.oodapplication.ui.theme.DinnerColor
import com.satwik.oodapplication.ui.theme.SuccessGreen
import com.satwik.oodapplication.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFoodCountScreen(
    viewModel: AdminFoodCountViewModel = hiltViewModel(),
    onBack: () -> Unit,
    showBack: Boolean = true,
    showBatchBreakdown: Boolean = true
) {
    val reportResource by viewModel.report.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Count Report") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Date Header
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Report Date", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(selectedDate, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val resource = reportResource) {
                is Resource.Loading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).height(80.dp).shimmerModifier())
                            Box(modifier = Modifier.weight(1f).height(80.dp).shimmerModifier())
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp).shimmerModifier())
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).shimmerModifier())
                    }
                }
                is Resource.Success -> {
                    val report = resource.data ?: return@Column
                    ReportContent(report, showBatchBreakdown)
                }
                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(resource.message ?: "Error loading report", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportContent(report: FoodCountReport, showBatchBreakdown: Boolean = true) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Total Summaries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TotalCard("Morning (B)", report.totalBreakfast, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                TotalCard("Afternoon (L)", report.totalLunch, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TotalCard("Evening (S)", report.totalSnack, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
                TotalCard("Night (D)", report.totalDinner, DinnerColor, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TotalCard("Students On Leave / Unavailable", report.totalOnLeave, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), Modifier.fillMaxWidth())
        }

        if (showBatchBreakdown) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Batch-wise Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                BatchHeader()
            }

            items(report.batchBreakdown) { batch ->
                BatchRow(batch)
            }
        }
    }
}

@Composable
fun TotalCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count.toString(), 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Black,
                color = if (color.luminance() > 0.5f) Color.Black else Color.White
            )
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold,
                color = (if (color.luminance() > 0.5f) Color.Black else Color.White).copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun BatchHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Batch", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.surface)
        Text("B", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.surface, textAlign = TextAlign.Center)
        Text("L", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.surface, textAlign = TextAlign.Center)
        Text("S", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.surface, textAlign = TextAlign.Center)
        Text("D", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.surface, textAlign = TextAlign.Center)
        Text("LV", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.surface, textAlign = TextAlign.Center)
    }
}

@Composable
fun BatchRow(batch: BatchFoodCount) {
    var expanded by remember { mutableStateOf(false) }
    
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f
    )

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { expanded = !expanded },
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(rotationZ = rotationState),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(batch.batchName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                }
                Text(batch.breakfast.toString(), modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(batch.lunch.toString(), modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(batch.snack.toString(), modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(batch.dinner.toString(), modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(batch.onLeave.toString(), modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                Column(modifier = Modifier.padding(12.dp)) {
                    batch.students.forEach { student ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(student.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(student.rollNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            }
                            StatusIndicator(student.breakfast, Modifier.weight(0.7f))
                            StatusIndicator(student.lunch, Modifier.weight(0.7f))
                            StatusIndicator(student.snack, Modifier.weight(0.7f))
                            StatusIndicator(student.dinner, Modifier.weight(0.7f))
                            StatusIndicator(student.isLeave, Modifier.weight(0.7f), isLeave = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(isPresent: Boolean, modifier: Modifier = Modifier, isLeave: Boolean = false) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isPresent) {
            val color = if (isLeave) MaterialTheme.colorScheme.error else SuccessGreen
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(4.dp)
                )
            }
        } else {
            Text("-", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
