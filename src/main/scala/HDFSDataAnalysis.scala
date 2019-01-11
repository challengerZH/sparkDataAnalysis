import java.text.SimpleDateFormat

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.sql.functions._

import scala.collection._

/**
  * Created by zhouhua on 2018/12/18.
  */
object HDFSDataAnalysis {

  def main(args: Array[String]): Unit = {
    //构建上下文
    val spark = SparkSession.builder().master("local").appName("ANA").getOrCreate()
    //读取数据文件并处理
    val rdd1 = spark.sparkContext.textFile("hdfs://bigdata01.com:8020/SG/datas").map(_.split("\t")).map(line => Row(line(1), line(2), line(3).split("\\|")(1), tranTimeToLong(line(4))))
    //指定DF的Schema
    val structType = StructType(Array(StructField("USER_ID", StringType, true), StructField("TOKEN", StringType, true),
      StructField("PATH", StringType, true), StructField("ACCESS_DATE", LongType, true)))
    //创建数据的dataframe
    var df = spark.createDataFrame(rdd1, structType).toDF()
    df = df.sort(df("TOKEN").desc, df("ACCESS_DATE").asc).withColumn("id", row_number() over (Window.orderBy("USER_ID")))
    //
    df.createOrReplaceTempView("sourceData")

    var tmp = 1;
    val arr = new mutable.ArrayBuffer[String]()
    val listData = df.collectAsList()
    for (i <- 0 to listData.size() - 2) {
      val dt = listData.get(i)
      val dt1 = listData.get(i + 1)
      if (dt(1).equals(dt1(1))) {
        var time = dt1(3).toString.toLong - dt(3).toString.toLong
        if (time > 300) {
          if (judge(dt(2).toString))
            time = 300
          else
            time = 0
        }
        arr.append(tmp + "," + time)
      } else {
        arr.append(tmp + "," + 0)
      }
      tmp = tmp + 1
    }
    arr.append(tmp + "," + 0)

    //处理停留时间数据为dataframe并注册临时表
    val residenceTimeRDD = spark.sparkContext.parallelize(arr).map(_.split(",")).map(line => Row(line(0).toInt, line(1).toLong))
    val residenceTimetype = StructType(Array(StructField("id", IntegerType, true), StructField("time", LongType, true)))
    val residenceTimeDF = spark.createDataFrame(residenceTimeRDD, residenceTimetype)
    residenceTimeDF.createOrReplaceTempView("residence_time") //    residenceTimeDF.show(55)

    //第一步，双表jion，得到带停留时间的数据
    var result = spark.sql("select sourceData.*,residence_time.time from sourceData inner join residence_time on  sourceData.id=residence_time.id").sort("id")
    //第二步，过滤停留时间，停留低于12秒的不统计
    result = result.filter("time>=12")
    result.createOrReplaceTempView("resultData")

    //USER数据
    val rddUser = spark.sparkContext.textFile("hdfs://bigdata01.com:8020/SG/datas1").map(_.split("\t")).map(line => Row(line(0), line(1), line(2)))
    //指定DF的Schema
    val structTypeUser = StructType(Array(StructField("ID", StringType, true), StructField("USER_NAME", StringType, true), StructField("ORG_ID", StringType, true)))
    //创建数据的dataframe
    val dfUser = spark.createDataFrame(rddUser, structTypeUser).toDF()
    dfUser.createOrReplaceTempView("user")

    //ORGANIZE数据
    val rddOrg = spark.sparkContext.textFile("hdfs://bigdata01.com:8020/SG/datas2").map(_.split("\t")).map(line => Row(line(0), line(3)))
    //指定DF的Schema
    val structTypeOrg = StructType(Array(StructField("ID", StringType, true), StructField("ORG_NAME", StringType, true)))
    //创建数据的dataframe
    val dfOrg = spark.createDataFrame(rddOrg, structTypeOrg).toDF()
    dfOrg.createOrReplaceTempView("org")

    //连表查询和分组求和
    spark.sql("select user.USER_NAME,org.ORG_NAME,resultData.PATH,resultData.time from resultData,user,org where resultData.USER_ID=user.ID and user.ORG_ID=org.ID").createOrReplaceTempView("lastData")
    val lastResultData = spark.sql("select USER_NAME,ORG_NAME,PATH,Count(PATH) as number,sum(time) as Total_time from lastData group by USER_NAME,ORG_NAME,PATH").sort("USER_NAME")
    lastResultData.repartition(1).write.mode(SaveMode.Overwrite).format("csv").save("hdfs://bigdata01.com:8020/SG/resultdata")
    lastResultData.show(100)
  }

  //时间戳转时间
  def tranTimeToLong(tm: String): Long = {
    val fm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val dt = fm.parse(tm)
    val aa = fm.format(dt)
    val tim: Long = dt.getTime() / 1000
    tim
  }

  //算法平台和解决方案页面判断
  def judge(s: String): Boolean = {
    val arr: Array[String] = Array("算法训练平台", "模型试用", "人脸识别", "周界安防", "物体识别", "钢铁缺陷检测", "人群密度分析", "关键点标记", "行为识别", "二维姿态估计", "三位姿态估计", "蒙皮处理",
      "NLP", "工业预测", "智能设备", "智能质量", "智能轧钢", "智能炼钢", "智能安全", "智慧质量", "智慧生产", "智慧招商", "智慧应急", "智慧医疗", "智慧应急", "智慧物流", "智慧警务", "智慧教育", "智慧交通")
    for (i <- arr) {
      if (i.equals(s)) {
        return true
      }
    }
    return false
  }

}
