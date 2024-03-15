package com.example.myspeechy.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.utils.lesson.meditation.MeditationStatsViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.vertical.VerticalAxis
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.context.MeasureContext
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MeditationStatsScreen(viewModel: MeditationStatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var moveColors by remember {
        mutableStateOf(false)
    }
    val backgroundGradientStops by animateFloatAsState(if (moveColors) 0.9f else 0.5f, label="",
        animationSpec = tween(4000)
    )
    LaunchedEffect(Unit) {
        moveColors = true
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        backgroundGradientStops - 0.8f to Color.Magenta,
                        backgroundGradientStops to Color.Cyan
                    )
                )
            )) {
        Text("Meditation time in minutes",
            style = MaterialTheme.typography.titleLarge)
        AnimatedVisibility(moveColors && uiState.statsMap.isNotEmpty(),
            enter = fadeIn(tween(1000))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 5.dp, end = 5.dp, top = 30.dp)) {
                MeditationStatsChart(uiState.statsMap)
            }
        }
        if (uiState.statsMap.isEmpty()) {
            Text("No stats here yet")
        }
    }
}

@Composable
fun MeditationStatsChart(statsMap: Map<String, Int>) {
    val data = mutableMapOf<Float, String>()
    var maxValue = 0
    var i = 0
    statsMap.entries.forEach { entry ->
        val value = entry.value
        if (value > maxValue) {
            maxValue = value
        }
        data[i.toFloat()] = entry.key
        i++
    }
    val verticalItemPlacer = MyVerticalAxisItemPlacer(maxValue)
    val entries = List(statsMap.size)
        { entryOf(it, statsMap.values.toList()[it]) }
    Chart(
        chart = columnChart(
            columns = listOf(lineComponent(
                color = Color.Magenta,
                thickness = 5.dp,
                shape = RoundedCornerShape(30)))
        ),
        model = entryModelOf(entries),
        startAxis = rememberStartAxis(
            itemPlacer = verticalItemPlacer),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ ->
                if (data.keys.contains(value))
                    LocalDate.parse(data[value])?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "" else ""
        }),
        modifier = Modifier
            .height(400.dp)
            .clip(RoundedCornerShape(8))
            .background(Color.White.copy(0.6f))
            .border(1.dp, Color.White, RoundedCornerShape(8))
    )
}

class MyVerticalAxisItemPlacer(private val maxValue: Int) : AxisItemPlacer.Vertical {
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
        val labelList = mutableListOf<Float>()
        for (i in 0..(if (maxValue < 60) maxValue else 60) step 5) {
            labelList.add(i.toFloat())
        }
        return labelList.toList()
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
