error id: file:///C:/Users/16593/hello-world/src/main/scala/UserInteraction.scala:1
file:///C:/Users/16593/hello-world/src/main/scala/UserInteraction.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:

offset: 85
uri: file:///C:/Users/16593/hello-world/src/main/scala/UserInteraction.scala
text:
```scala
object UserInteraction {
  def selectDataset(): (Int, String) = {     println("请选择能源类@@型：")
    DATASETS.foreach { case (k, (_, name)) => println(s"$k. $name") }
    
    val input = scala.io.StdIn.readLine("请输入编号：").trim
    DATASETS.getOrElse(input, {
      println("❌ 无效选择")
      sys.exit(1)
    }) }
  
  def selectTimeRange(): (ZonedDateTime, ZonedDateTime) = { /* 原有readTimeRange实现 */ }
  
  def selectTimeDimension(): TimeDimension = { /* 维度选择逻辑 */ }
  
  def confirm(prompt: String): Boolean = { /* 原有readYesNo实现 */ }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 