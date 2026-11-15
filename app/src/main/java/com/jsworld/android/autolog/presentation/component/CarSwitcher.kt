package com.jsworld.android.autolog.presentation.component

import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.domain.model.Car
import java.text.NumberFormat

/**
 * 탭 화면 상단의 차량 전환 칩. 탭하면 [CarSwitcherSheet] 가 열린다.
 */
@Composable
fun CarSwitcherChip(
    car: Car?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = car?.name ?: stringResource(R.string.car_switcher_select),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(3.dp))
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.car_switcher_switch),
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 차량 전환 시트. 차량 선택 외에 차량 추가·관리 진입점도 여기 모아둔다.
 * (탭 구조로 바뀌면서 차량 목록이 홈에서 빠졌기 때문에 이 시트가 유일한 진입점이다)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarSwitcherSheet(
    cars: List<Car>,
    selectedCarId: Long?,
    onSelect: (Car) -> Unit,
    /** null 이면 줄 자체를 그리지 않는다 — 차량 관리로 갈 수 없는 화면에서 쓴다 */
    onAddCar: (() -> Unit)? = null,
    onManageCars: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
        ) {
            Text(
                stringResource(R.string.car_switcher_switch),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.car_switcher_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            cars.forEach { car ->
                CarSwitcherRow(
                    car = car,
                    selected = car.id == selectedCarId,
                    onClick = { onSelect(car) }
                )
            }

            if (onAddCar != null || onManageCars != null) {
                Spacer(Modifier.height(10.dp))
            }
            if (onAddCar != null) {
                SheetActionRow(
                    icon = Icons.Default.Add,
                    label = stringResource(R.string.car_switcher_add),
                    onClick = onAddCar
                )
            }
            if (onManageCars != null) {
                SheetActionRow(
                    icon = Icons.Default.DirectionsCar,
                    label = stringResource(R.string.car_switcher_manage),
                    onClick = onManageCars
                )
            }
        }
    }
}

@Composable
private fun CarSwitcherRow(
    car: Car,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container =
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        color = container,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        car.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (car.isPrimary) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.common_primary_car),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    stringResource(R.string.car_switcher_plate_km, car.plate, car.mileage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.common_selected),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Int.formatComma(): String = NumberFormat.getIntegerInstance().format(this)
