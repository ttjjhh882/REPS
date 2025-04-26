error id: 7B24F233620AC68017BC6B6C3469744D
file:///C:/Users/16593/hello-world/src/main/scala/Main.scala
### scala.reflect.internal.Types$TypeError: illegal cyclic reference involving object Predef

occurred in the presentation compiler.



action parameters:
uri: file:///C:/Users/16593/hello-world/src/main/scala/Main.scala
text:
```scala
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import sttp.client3._
import sttp.client3.circe._
import io.circe.generic.auto._
import scala.io.StdIn
import scala.util.{Failure, Success, Try}
import java.io.{File, PrintWriter}
import com.osinka.i18n.Lang
import scala.collection.immutable.ListMap

object EnergyAnalysis {
  // 配置信息
  private val DATASET_MAP = Map(
    "1" -> (245, "风力发电"),
    "2" -> (247, "太阳能发电"),
    "3" -> (253, "水力发电")
  )

  // 定义 API 响应结构
  case class DataPoint(startTime: String, value: Double)
  case class ApiResponse(data: List[DataPoint])

  def main(args: Array[String]): Unit = {
    // 选择数据集
    println("请选择要查询的数据类型：")
    println("1. 风力发电")
    println("2. 太阳能发电")
    println("3. 水力发电")
    val choice = StdIn.readLine("请输入编号（1/2/3）：").trim

    val (datasetId, datasetName) = DATASET_MAP.getOrElse(choice, {
      println("❌ 无效选择，请输入 1、2 或 3。")
      sys.exit(1)
    })

    // 设置时间范围（可根据需要恢复用户输入）
    val startTime = "2024-03-28T00:00:00Z"
    val endTime = "2024-04-02T03:00:00Z"

    // 创建请求
    val request = basicRequest
      .get(uri"https://data.fingrid.fi/api/datasets/$datasetId/data?startTime=$startTime&endTime=$endTime&format=json&oneRowPerTimePeriod=false&pageSize=20000")
      .header("x-api-key", sys.env.getOrElse("FINGRID_API_KEY", {
        println("❌ 未找到 API Key")
        sys.exit(1)
      }))
      .response(asJson[ApiResponse])

    // 发送同步请求
    implicit val backend = HttpURLConnectionBackend()
    val response = request.send()

    response.body match {
      case Left(error) =>
        println(s"❌ 请求失败：${error.getMessage}")
        sys.exit(1)

      case Right(apiResponse) =>
        // 解析时间数据
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dataPoints = apiResponse.data.map { point =>
          val parsedTime = LocalDateTime.parse(
            point.startTime.replace("Z", "+00:00"),
            formatter
          )
          (parsedTime, point.value)
        }

        // 生成统计信息
        val values = dataPoints.map(_._2)
        val stats = Map(
          "mean" -> mean(values),
          "median" -> median(values),
          "mode" -> mode(values),
          "range" -> range(values),
          "midrange" -> midrange(values)
        )

        // 显示统计结果
        println("\n📊 统计分析：")
        stats.foreach { case (k, v) =>
          println(f"${statName(k)}: ${v match {
            case d: Double => f"$d%.2f MWh/h"
            case s: String => s
          }}")
        }

        // 导出 CSV
        if (StdIn.readLine("\n是否导出为 CSV 文件？(y/n)：").trim.equalsIgnoreCase("y")) {
          val filename = s"${datasetName}_data_${startTime.take(10)}_to_${endTime.take(10)}.csv"
            .replace(" ", "_")
          
          val writer = new PrintWriter(new File(filename))
          try {
            writer.println("Time,Forecast (MWh/h)")
            dataPoints.foreach { case (time, value) =>
              writer.println(s"${time.atOffset(ZoneOffset.UTC)},$value")
            }
            writer.println("\n统计指标,数值")
            stats.foreach { case (k, v) =>
              writer.println(s"${statName(k)},$v")
            }
            println(s"✅ 已导出为：$filename")
          } finally {
            writer.close()
          }
        }
    }
  }

  // 统计方法
  private def mean(values: List[Double]): Double = 
    if (values.isEmpty) 0.0 else values.sum / values.size

  private def median(values: List[Double]): Double = {
    val sorted = values.sorted
    val n = sorted.size
    if (n % 2 == 1) sorted(n / 2)
    else (sorted(n / 2 - 1) + sorted(n / 2)) / 2.0
  }

  private def mode(values: List[Double]): Any = {
    val counts = values.groupBy(identity).mapValues(_.size)
    val maxCount = counts.values.maxOption.getOrElse(0)
    val modes = counts.filter(_._2 == maxCount).keys.toList

    modes match {
      case Nil => "无数据"
      case single :: Nil => single
      case _ => "无唯一众数"
    }
  }

  private def range(values: List[Double]): Double = 
    if (values.isEmpty) 0.0 else values.max - values.min

  private def midrange(values: List[Double]): Double = 
    if (values.isEmpty) 0.0 else (values.max + values.min) / 2.0

  // 统计项名称映射
  private def statName(key: String): String = key match {
    case "mean" => "均值 (Mean)"
    case "median" => "中位数 (Median)"
    case "mode" => "众数 (Mode)"
    case "range" => "极差 (Range)"
    case "midrange" => "中程数 (Midrange)"
    case _ => key
  }
}
```


presentation compiler configuration:
Scala version: 2.12.20
Classpath:
<HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scala-lang\scala-library\2.12.20\scala-library-2.12.20.jar [exists ]
Options:





#### Error stacktrace:

```

```
#### Short summary: 

scala.reflect.internal.Types$TypeError: illegal cyclic reference involving object Predef