package com.example.myspeechy.screens

import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.domain.Result
import com.example.myspeechy.presentation.lesson.meditation.MeditationStatsViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.vertical.VerticalAxis
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.context.MeasureContext
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MeditationStatsScreen(viewModel: MeditationStatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val backgroundGradientStops by animateFloatAsState(if (uiState.moveGradientColors) 0.9f else 0.5f, label="",
        animationSpec = tween(4000)
    )
    LaunchedEffect(Unit) {
        viewModel.loadResultFlow.collectLatest { res ->
            if (res.error.isNotEmpty()) {
                Toasty.error(context, res.error, Toast.LENGTH_SHORT, true).show()
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.setUpListener(false)
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setUpListener(true)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        backgroundGradientStops - 0.8f to MaterialTheme.colorScheme.surfaceTint,
                        backgroundGradientStops to MaterialTheme.colorScheme.inversePrimary
                    )
                )
            )
            .padding(20.dp)) {
        Text("Meditation time" +
                if (uiState.maxValue == 0) "" else if (uiState.maxValue <= 60) " in seconds" else " in minutes",
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onBackground
            ))
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)) {
            when (uiState.loadResult) {
                is Result.Success -> {
                    AnimatedVisibility(uiState.statsMap.isNotEmpty(),
                        enter = fadeIn(tween(1000))
                    ) {
                        MeditationStatsChart(
                            uiState.maxValue,
                            uiState.statsMap,
                            uiState.labelList)
                    }
                    if (uiState.statsMap.isEmpty()) {
                        Text("No stats here yet",
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                is Result.InProgress -> CircularProgressIndicator()
                is Result.Error -> Text("Error",
                    color = MaterialTheme.colorScheme.onBackground)
                else -> {}
            }
        }

    }
}

@Composable
fun MeditationStatsChart(
    maxValue: Int,
    statsMap: Map<String, Int>,
    labelList: List<Float>) {
    val data by remember(statsMap) {
        mutableStateOf(buildMap {
            for (entry in statsMap.entries) {
                put(statsMap.entries.indexOf(entry).toFloat(), entry.key)
            }
        })
    }
    val verticalItemPlacer = MyVerticalAxisItemPlacer(labelList)

    //if the max number of seconds is bigger than 60, scale values to minute format,
    //otherwise keep them in seconds
    val entries by remember(statsMap) {
        mutableStateOf(List(statsMap.size)
        { entryOf(it, statsMap.values.toList()[it]/(if (maxValue > 60) 60f else 1f)) })
    }
    BoxWithConstraints {
        Chart(
            chart = columnChart(
                columns = listOf(lineComponent(
                    color = Color.Magenta,
                    thickness = 5.dp,
                    shape = RoundedCornerShape(30)))
            ),
            model = entryModelOf(entries),
            startAxis = rememberStartAxis(
                label = textComponent(
                    color = MaterialTheme.colorScheme.onBackground,
                    textSize = 15.sp
                ),
                guideline = LineComponent(MaterialTheme.colorScheme.onPrimary.toArgb()),
                itemPlacer = verticalItemPlacer),
            bottomAxis = rememberBottomAxis(
                label = textComponent(
                    color = MaterialTheme.colorScheme.onBackground,
                    textSize = 15.sp,
                    typeface = Typeface.SANS_SERIF
                ),
                valueFormatter = { value, _ ->
                    if (data.keys.contains(value))
                        LocalDate.parse(data[value])?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "" else ""
                }),
            modifier = Modifier
                .height(this.maxHeight)
                .clip(RoundedCornerShape(4))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(0.6f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4))
        )
    }
}

class MyVerticalAxisItemPlacer(private val labelList: List<Float>) : AxisItemPlacer.Vertical {
    override fun getHeightMeasurementLabelValues(
        context: MeasureContext,
        position: AxisPosition.Vertical
    ): List<Float> {
        return AxisItemPlacer.Vertical.default().getHeightMeasurementLabelValues(context, position)
    }

    override fun getTopVerticalAxisInset(
        verticalLabelPosition: VerticalAxis.VerticalLabelPosition,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float {
        return AxisItemPlacer.Vertical.default().getTopVerticalAxisInset(
            verticalLabelPosition,
            maxLabelHeight,
            maxLineThickness
        )
    }

    override fun getWidthMeasurementLabelValues(
        context: MeasureContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical
    ): List<Float> {
        return AxisItemPlacer.Vertical.default().getWidthMeasurementLabelValues(
            context,
            axisHeight,
            maxLabelHeight,
            position
        )
    }
    override fun getLabelValues(
        context: ChartDrawContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical
    ): List<Float> {
        return labelList
    }
    override fun getBottomVerticalAxisInset(
        verticalLabelPosition: VerticalAxis.VerticalLabelPosition,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float {
        return AxisItemPlacer.Vertical.default().getBottomVerticalAxisInset(
            verticalLabelPosition,
            maxLabelHeight,
            maxLineThickness
        )
    }
}
