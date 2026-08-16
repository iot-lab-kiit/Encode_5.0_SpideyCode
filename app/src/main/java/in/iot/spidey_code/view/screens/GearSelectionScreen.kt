package `in`.iot.spidey_code.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.view.components.FilterCard
import `in`.iot.spidey_code.vm.GearSelectionViewModel

@Composable
fun GearSelectionScreen(
    onNavigateToCamera: (FilterType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GearSelectionViewModel = viewModel()
) {
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            Text(
                text = "Pick Your Gear",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select an active suit filter before starting the camera preview.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // 2x2 Filter Card Grid Layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    FilterCard(
                        filterType = FilterType.CLASSIC_MASK,
                        title = "Classic Mask",
                        description = "Iconic red & webbed hero mask overlay.",
                        isSelected = selectedFilter == FilterType.CLASSIC_MASK,
                        onSelect = { viewModel.selectFilter(FilterType.CLASSIC_MASK) }
                    )
                }
                item {
                    FilterCard(
                        filterType = FilterType.WEB_SHOOTER,
                        title = "Web Shooter",
                        description = "Tactical web shooter reticle & crosshair.",
                        isSelected = selectedFilter == FilterType.WEB_SHOOTER,
                        onSelect = { viewModel.selectFilter(FilterType.WEB_SHOOTER) }
                    )
                }
                item {
                    FilterCard(
                        filterType = FilterType.SPIDEY_SENSE,
                        title = "Spidey Sense",
                        description = "Sensory aura lines & warning pulse.",
                        isSelected = selectedFilter == FilterType.SPIDEY_SENSE,
                        onSelect = { viewModel.selectFilter(FilterType.SPIDEY_SENSE) }
                    )
                }
                item {
                    FilterCard(
                        filterType = FilterType.NONE,
                        title = "No Mask",
                        description = "Clean view without active overlay.",
                        isSelected = selectedFilter == FilterType.NONE,
                        onSelect = { viewModel.selectFilter(FilterType.NONE) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onNavigateToCamera(selectedFilter) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "CONTINUE TO CAMERA",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
