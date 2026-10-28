package com.phonehardware.diagnostic.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.phonehardware.diagnostic.core.Evidence
import com.phonehardware.diagnostic.core.Metric
import com.phonehardware.diagnostic.ui.MainViewModel

@Composable fun TouchGrid(vm: MainViewModel) {
    val cols=6; val rows=10
    val hits = remember { mutableStateListOf<Int>() }
    var maxTouches by remember { mutableIntStateOf(0) }
    Text("Drag a finger along every cell, including edges and corners. Blue cells received actual touches; blank cells have not been assessed.")
    Box(Modifier.fillMaxWidth().height(460.dp)) {
        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                do {
                    val evt = awaitPointerEvent()
                    maxTouches = maxOf(maxTouches,evt.changes.count { it.pressed })
                    evt.changes.filter { it.pressed }.forEach { change ->
                        val x=(change.position.x/size.width*cols).toInt().coerceIn(0,cols-1)
                        val y=(change.position.y/size.height*rows).toInt().coerceIn(0,rows-1)
                        val index=y*cols+x
                        if(index !in hits) hits.add(index)
                    }
                } while(evt.changes.any { it.pressed })
            }
        }) {
            val cellWidth = size.width/cols
            val cellHeight = size.height/rows
            for(row in 0 until rows) for(col in 0 until cols) {
                val index=row*cols+col
                val top=Offset(col*cellWidth,row*cellHeight)
                drawRect(if(index in hits) Color(0xFF1A78CF) else Color(0xFF25354E),topLeft=top,size=Size(cellWidth-2,cellHeight-2))
                drawRect(Color(0xFF98BDE6),topLeft=top,size=Size(cellWidth,cellHeight),style=Stroke(1f))
            }
        }
    }
    Text("Cells activated: ${hits.size}/${cols*rows} · Maximum simultaneous contacts observed: $maxTouches")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={hits.clear();maxTouches=0}){Text("Reset")}
        Button(onClick={
            vm.interactiveResult("touch_grid",if(hits.size==cols*rows) true else null,"Compose pointer events and 6×10 cell coverage",
                listOf(Metric("Cells touched",hits.size.toString(),"/60",Evidence.MEASURED),Metric("Max simultaneous contacts",maxTouches.toString(),evidence=Evidence.MEASURED)),
                "Untouched cells are not evidence of dead zones; full coverage only confirms observed touch input.")
        }){Text("Save coverage result")}
    }
}

private data class Pattern(val name: String, val color: Color)
@Composable fun PixelScreen(vm: MainViewModel) {
    val patterns = remember { listOf(Pattern("Red",Color.Red),Pattern("Green",Color.Green),Pattern("Blue",Color.Blue),
        Pattern("White",Color.White),Pattern("Black",Color.Black),Pattern("Gray",Color.Gray),Pattern("Checkerboard",Color.DarkGray),Pattern("Gradient",Color.Transparent)) }
    var index by remember { mutableIntStateOf(0) }
    val seen = remember { mutableStateListOf<Int>(0) }
    Text("Inspect the entire panel carefully for dead/stuck pixels, tint, retention and visual artifacts. Tap Next to cycle real full-screen patterns.")
    Canvas(Modifier.fillMaxWidth().height(430.dp).clickable { index=(index+1)%patterns.size; if(index !in seen) seen.add(index) }) {
        if(index<=5) drawRect(patterns[index].color)
        if(index==6) {
            val cell=size.width/9
            for(row in 0..15) for(col in 0..8) {
                drawRect(if((row+col)%2==0) Color.White else Color.Black,Offset(col*cell,row*cell),Size(cell+1,cell+1))
            }
        }
        if(index==7) drawRect(brush=Brush.horizontalGradient(listOf(Color.Black,Color.Red,Color.Yellow,Color.Green,Color.Cyan,Color.Blue,Color.White)))
    }
    Text("${patterns[index].name} · ${seen.size}/${patterns.size} patterns viewed")
    Button(onClick={index=(index+1)%patterns.size;if(index !in seen)seen.add(index)},modifier=Modifier.fillMaxWidth()) { Text("Next pattern") }
    Text("Only you can assess visible defects. A visually clean screen does not exclude microscopic or intermittent faults.")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=seen.size==patterns.size,onClick={vm.interactiveResult("pixel_check",true,"User-confirmed visual inspection",listOf(Metric("Patterns viewed",seen.size.toString(),evidence=Evidence.USER_CONFIRMED)))}){Text("Looks good")}
        OutlinedButton(onClick={vm.interactiveResult("pixel_check",false,"User-confirmed visual inspection",warning="A visual observation alone does not identify the root cause.")}){Text("Defect observed")}
        TextButton(onClick={vm.interactiveResult("pixel_check",null,"Visual inspection incomplete")}){Text("Unsure")}
    }
}
