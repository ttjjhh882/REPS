import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import requests._
import ujson._
import os._
import plotly._
import plotly.element._
import plotly.layout._
import plotly.Plotly._
import better.files._
import scala.util.Try
import scala.math.Ordering
import scala.collection.mutable
import scala.annotation.tailrec


// Jiahang Tian
// Zeyu Liu
// Hongrui Zhang
object RenewableEnergyAnalyzer {
  case class EnergyData(time: ZonedDateTime, value: Double)
  case class ApiError(message: String, details: Option[Value] = None)

  private val API_BASE = "https://data.fingrid.fi/api"
  val helsinkiZone = ZoneId.of("Europe/Helsinki") 
  private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(helsinkiZone)
  private val DATASETS = Map(
    "1" -> (245, "Wind power"),
    "2" -> (247, "Solar Power"),
    "3" -> (191, "Hydro power")
  )
  


def main(args: Array[String]): Unit = {
  // initialize the API env
  val apiKey = sys.env.get("FINGRID_API_KEY").getOrElse {
    println("Cannot find FINGRID_API_KEY in .env, please set it.")
    return
  }

  @tailrec
  def mainLoop(): Unit = {
    println("\nSelect Mode：")
    println("1. Historical data analysis")
    println("2. Real-time monitoring (manual mode)")
    println("3. Renewable energy equipment control")
    println("4. Exit")

    scala.io.StdIn.readLine("Please enter the number:").trim match {
      case "1" =>
        val dataset = readDataset()
        val (start, end) = readTimeRange()
        
        fetchData(dataset._1, start, end, apiKey) match {
          case Right(data) if data.nonEmpty =>
            if (readYesNo("Generate chart(y/n)")) {
              generatePlot(data, dataset._2)
            }
            if (readYesNo("Show Basic Statistics(y/n)")) {
              showStatistics(data.map(_.value))
            }
            if (readYesNo("Export CSV file(y/n)")) {
              exportToCsv(data, dataset._2)
            }
            
          case Right(_) =>
            println("There is no data for the specified time range")
            
          case Left(err) =>
            println(s"Data acquisition failed: ${err.message}")
        }
        mainLoop()

      case "2" =>
        startManualMonitoring(apiKey)
        mainLoop()

      case "3" =>
        simulateControlInterface()
        mainLoop()

      case "4" =>
        println("Exiting...")
        return 
      case _ =>
        println("Invalid input")
        mainLoop()
    }
  }

  mainLoop()
}





def startManualMonitoring(apiKey: String): Unit = {
  val dataset = readDataset()
  
  val threshold = {
    print("Please enter the alarm threshold (MW):")
    Try(scala.io.StdIn.readLine().trim.toDouble).getOrElse {
      println("Invalid threshold, use the default value 0.0")
      0.0
    }
  }

  println(
    s"""|Start monitoring ${dataset._2}
        |[y] Get the latest data
        |[n] Stop monitoring
        |========================""".stripMargin)

  @tailrec
  def monitorLoop(): Unit = {
    val input = scala.io.StdIn.readLine("Please enter the command:").trim.toLowerCase
    
    input match {
      case "y" =>
        // 获取最近1小时数据
        val now = ZonedDateTime.now(helsinkiZone)
        val start = now.minusHours(1)
        
        fetchData(dataset._1, start, now, apiKey) match {
          case Right(data) if data.nonEmpty =>
            val latest = data.last.value
            val timeStr = now.format(DATE_FMT)
            println(f"[$timeStr] Current Value:$latest%.2f MW")
            
            if (latest < threshold) {
              println(s"\u001B[31mWarning: The current value is lower than the threshold ${threshold}MW!\u001B[0m")
            }
            
          case Right(_) =>
            println("There is no latest data yet")
            
          case Left(err) =>
            println(s"Data acquisition error: ${err.message}")
        }
        
        monitorLoop()
        
      case "n" =>
        println("Monitoring stopped")
        main(Array.empty) 
        
      case _ =>
        println("Invalid command, please enter[y/n]")
        monitorLoop() 
    }
  }

  monitorLoop()
}

  
  def readDataset(): (Int, String) = {
  @tailrec
  def loop(): (Int, String) = {
    println("Please select the energy type:")
    DATASETS.foreach { case (k, (_, name)) => println(s"$k. $name") }
    val input = scala.io.StdIn.readLine("Please enter the number:").trim
    DATASETS.get(input) match {
      case Some(dataset) => dataset
      case None => 
        println("Invalid input, please try again.")
        loop() 
    }
  }
  loop()
}

def readTimeRange(): (ZonedDateTime, ZonedDateTime) = {
  def parseTime(prompt: String): ZonedDateTime = {
    while (true) {
      val input = scala.io.StdIn.readLine(prompt).trim
      val parsed = Try(LocalDateTime.parse(input, DATE_FMT).atZone(ZoneId.systemDefault()))
      if (parsed.isSuccess) return parsed.get
      else println(s"The time format is incorrect, please use dd/MM/yyyy HH:mm (e.g. 28/03/2024 14:30)")
    }
    null 
  }

  while (true) {
    val start = parseTime("Start time (dd/MM/yyyy HH:mm):")
    val end = parseTime("End time (dd/MM/yyyy HH:mm):")
    if (!end.isBefore(start)) {
      return (
        start.withZoneSameInstant(ZoneId.of("Europe/Helsinki")),
        end.withZoneSameInstant(ZoneId.of("Europe/Helsinki"))
      )
    }
    println("The end time cannot be earlier than the start time")
  }
  
  (null, null)
}

  // fetch data from API
private def fetchData(
  datasetId: Int,
  start: ZonedDateTime,
  end: ZonedDateTime,
  apiKey: String
): Either[ApiError, List[EnergyData]] = {
  val params = Map(
    "startTime" -> start.format(DateTimeFormatter.ISO_INSTANT),
    "endTime" -> end.format(DateTimeFormatter.ISO_INSTANT),
    "pageSize" -> "20000"
  )

  val resp = requests.get(
    s"$API_BASE/datasets/$datasetId/data",
    headers = Map("x-api-key" -> apiKey),
    params = params
  )

  if (resp.statusCode != 200) {
    val details = Try(ujson.read(resp.text())).toOption
    return Left(ApiError(s"HTTP ${resp.statusCode}", details))
  }

  Try {
    val data = ujson.read(resp.text())("data").arr
    data.map { item =>
      EnergyData(
        ZonedDateTime.parse(item("startTime").str).withZoneSameInstant(helsinkiZone), 
        item("value").num
      )
    }.toList
  }.toEither.left.map(e => ApiError(e.getMessage))
}

  // graph plot
  private def generatePlot(data: List[EnergyData], title: String): Unit = {
    //safe title
    val safeTitle = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_")
  
  // finland time zone
    val timestamp = LocalDateTime.now(helsinkiZone)
    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
  
    val fileName = s"${safeTitle}_$timestamp.html"

    val x = data.map(_.time.toInstant.toEpochMilli.toDouble)
    val y = data.map(_.value)
    
    val plot = Seq(
      Scatter()
        .withX(x)
        .withY(y)
        .withMode(ScatterMode(ScatterMode.Lines))
        .withName("power")
    )
    
    val layout = Layout()
      .withTitle(title)
      .withXaxis(
        Axis()
        .withTitle("time")
        .withType(AxisType.Date)
        )
      .withYaxis(Axis().withTitle("MW"))
    
    plotly.Plotly.plot(
      path = fileName,
      traces = plot,
      layout = layout,
      config = plotly.Config(),
      useCdn = true,
      openInBrowser = false,
      addSuffixIfExists = true
    )
    println(s"Chart saved to $fileName")
  }

  // basic statistics
  private def showStatistics(values: List[Double]): Unit = {
  if (values.isEmpty) return

  val sortedValues = values.sorted

  val mean = values.sum / values.size

  val median = if (values.size % 2 == 0) {
    (sortedValues(values.size / 2 - 1) + sortedValues(values.size / 2)) / 2.0
  } else {
    sortedValues(values.size / 2)
  }

  val mode = values.groupBy(identity).mapValues(_.size).maxBy(_._2)._1

  val range = values.max - values.min

  val midrange = (values.max + values.min) / 2.0

  val stats = Map(
    "Average" -> mean,
    "Median" -> median,
    "Mode" -> mode,
    "Range" -> range,
    "Midrange" -> midrange,
    "Max" -> values.max,
    "Min" -> values.min,
    "Data size" -> values.size.toDouble
  )

  println("\nStatistical results:")
  stats.foreach { case (k, v) =>
    println(f"$k: ${if(k.contains("数")) f"$v%.0f" else f"$v%.2f"} MW")
  }
}


import java.time._

  // save data to CSV

private def exportToCsv(data: List[EnergyData], name: String): Unit = {
  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val hourFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH")
  val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  val timestamp = LocalDateTime.now(ZoneId.of("Europe/Helsinki"))
    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
  //name + timestamp
  val baseName = s"${name}_$timestamp" 

  val groupedDataByHour = data.groupBy(d => d.time.format(hourFormatter))
  val groupedDataByDay = data.groupBy(d => d.time.toLocalDate.format(dateFormatter))
  val groupedDataByWeek = data.groupBy { d =>
    d.time.toLocalDate.`with`(DayOfWeek.MONDAY).format(dateFormatter)
  }
  val groupedDataByMonth = data.groupBy(d => d.time.toLocalDate.withDayOfMonth(1).format(dateFormatter))

  // choose grouping method
  println(
    """Please select the grouping method:
      |1. By hour
      |2. By day
      |3. By week
      |4. By month
      |5. No grouping (overall statistics + raw data)""".stripMargin)
  val groupOption = scala.io.StdIn.readLine().trim

  // choose sorting field
  val statFields = Seq("Average", "Median", "Mode", "Range", "Midrange")
  println(
    s"""Please select the sorting field:
       |1. Average
       |2. Median
       |3. Mode
       |4. Range
       |5. Midrange
       |6. No sorting""".stripMargin)
  val sortFieldOption = scala.io.StdIn.readLine().trim

  // choose sorting order
  val (sortFieldOpt, sortOrderOpt) =
    if (sortFieldOption == "6") (None, None)
    else {
      println("Please select the sorting order: 1. Ascending 2. Descending")
      val order = scala.io.StdIn.readLine().trim
      val field = try { Some(statFields(sortFieldOption.toInt - 1)) } catch { case _: Throwable => None }
      (field, Some(order))
    }

  val header = "Date, Average, Median, Mode, Range, Midrange"
  val lines: Seq[String] = groupOption match {
    case "1" | "2" | "3" | "4" =>
      val groupedData = groupOption match {
        case "1" => groupedDataByHour
        case "2" => groupedDataByDay
        case "3" => groupedDataByWeek
        case "4" => groupedDataByMonth
      }
      // statistics
      val summaryStats: Seq[(String, Map[String, Double])] =
        groupedData.toSeq.map { case (dateStr, records) =>
          val values = records.map(_.value)
          val stats = getStats(values)
          (dateStr, stats)
        }
      // sort
      val sortedSummary = (sortFieldOpt, sortOrderOpt) match {
        case (Some(field), Some(order)) if statFields.contains(field) =>
          val filtered = summaryStats.filter(_._2.nonEmpty)
          val compare = (a: (String, Map[String, Double]), b: (String, Map[String, Double])) =>
            order match {
              case "1" => // ascending
                a._2(field).compareTo(b._2(field))
              case "2" => // descending
                b._2(field).compareTo(a._2(field))
              case _   => 0
            }
          filtered.sortWith((a, b) => compare(a, b) < 0)
        case _ =>
          summaryStats.sortBy(_._1)
      }
      val summaryLines = sortedSummary.map { case (dateStr, stats) =>
        if (stats.nonEmpty)
          s"$dateStr, ${stats("Average")}, ${stats("Median")}, ${stats("Mode")}, ${stats("Range")}, ${stats("Midrange")}"
        else
          s"$dateStr, , , , , "
      }
      header +: summaryLines

    case "5" =>
      // no grouping
      val values = data.map(_.value)
      val stats = getStats(values)
      val summaryLine =
        if (stats.nonEmpty)
          s"Overall, ${stats("Average")}, ${stats("Median")}, ${stats("Mode")}, ${stats("Range")}, ${stats("Midrange")}"
        else
          s"Overall, , , , , "
      // raw data
      val originHeader = "\nOriginal data:\nTime, value"
      val originLines = data.sortBy(_.time).map { d =>
        s"${d.time.format(timeFormatter)}, ${d.value}"
      }
      Seq(header, summaryLine, originHeader) ++ originLines

    case _ =>
      println("Invalid selection, default grouping by day")
      val groupedData = groupedDataByDay
      val summaryStats: Seq[(String, Map[String, Double])] =
        groupedData.toSeq.map { case (dateStr, records) =>
          val values = records.map(_.value)
          val stats = getStats(values)
          (dateStr, stats)
        }
      val sortedSummary = summaryStats.sortBy(_._1)
      val summaryLines = sortedSummary.map { case (dateStr, stats) =>
        if (stats.nonEmpty)
          s"$dateStr, ${stats("Average")}, ${stats("Median")}, ${stats("Mode")}, ${stats("Range")}, ${stats("Midrange")}"
        else
          s"$dateStr, , , , , "
      }
      header +: summaryLines
  }

  // save to file
  val file = File(s"$baseName.csv")
  file.overwrite(lines.mkString("\n"))
  println(s"Data exported to ${file.pathAsString}")
}

// statistics calculation
private def getStats(values: List[Double]): Map[String, Double] = {
  if (values.isEmpty) return Map()
  val sortedValues = values.sorted

  val mean = values.sum / values.size

  val median = if (values.size % 2 == 0) {
    (sortedValues(values.size / 2 - 1) + sortedValues(values.size / 2)) / 2.0
  } else {
    sortedValues(values.size / 2)
  }

  // mode calculation
  val mode = values.groupBy(identity).view.mapValues(_.size).toMap.maxBy(_._2)._1

  val range = values.max - values.min
  val midrange = (values.max + values.min) / 2.0

  Map(
    "Average" -> mean,
    "Median" -> median,
    "Mode" -> mode,
    "Range" -> range,
    "Midrange" -> midrange
  )
}

  
private def readYesNo(prompt: String): Boolean = {
  @tailrec
  def loop(): Boolean = {
    val input = scala.io.StdIn.readLine(prompt).trim.toLowerCase
    input match {
      case "y" | "yes" => true
      case "n" | "no"  => false
      case _ =>
        println("Invalid input, please enter y or n.")
        loop() 
    }
  }
  
  loop()
}

private def simulateControlInterface(): Unit = {
  println("\n=== Equipment control simulation interface ===")
  
  val device = readDataset() 
  
  print("Please enter the target power(MW)：")
  val target = Try(scala.io.StdIn.readDouble()).getOrElse(0.0)
  
  println(
    s"""|
        |Operating [${device._2}]...
        |▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌
        |Target power set: ${target}MW
        |(This operation is for demonstration only and does not actually modify the system)
        |""".stripMargin)
  
  Thread.sleep(1000)
  println("Operation completed! Return to main menu\n")
}
}
