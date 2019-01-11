import java.text.SimpleDateFormat
import java.util.Properties

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}

/**
  * Created by zhouhua on 2018/12/21.
  */
object MYSQLDataAnalysis {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local").appName("ANA2").getOrCreate()

    //------------读操作------------
    /*
    //1、连接数据库（只读一张表的情况）
    val jdbcDF = spark.read.format("jdbc").option("url", "jdbc:mysql://localhost:3306/database1")
      .option("driver", "com.mysql.jdbc.Driver")
      .option("dbtable", "users")
      .option("user", "root")
      .option("password", "root").load()
    */
    //2、先进行数据源库连接，再读取指定的表
    val properties = new Properties();
    val url = "jdbc:mysql://localhost:3306/database1"
    properties.put("user", "root")
    properties.put("password", "root")
    val jdbcDF = spark.read.jdbc(url, "users",properties)

    jdbcDF.show(100)


    //-----------写操作-------------


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
