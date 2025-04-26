error id: file:///C:/Users/16593/hello-world/src/main/scala/FileExporter.scala:3
file:///C:/Users/16593/hello-world/src/main/scala/FileExporter.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:

offset: 120
uri: file:///C:/Users/16593/hello-world/src/main/scala/FileExporter.scala
text:
```scala
object FileExporter {
  object FileExporter {
  def exportAsCsv(data: List[EnergyData], filename: String): Unit = {
    @@val file = File("energy_data.csv")
    val lines = Seq("时间(UTC),功率(MW)") ++ 
      data.map(d => s"${d.time},${d.value}")
    
    file.overwrite(lines.mkString("\n"))
    println(s"✅ 数据已导出至 ${file.pathAsString}")
  }
}
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 