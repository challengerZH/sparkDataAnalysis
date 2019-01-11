/**
  * Created by zhouhua on 2018/12/21.
  */
object a {

  def main(args: Array[String]): Unit = {
    val arr : Array[String] = Array("算法训练平台","模型试用","人脸识别","周界安防","物体识别","钢铁缺陷检测","人群密度分析","关键点标记","行为识别","二维姿态估计","三位姿态估计","蒙皮处理","NLP","工业预测","智能设备","智能质量","智能轧钢","智能炼钢","智能安全","智慧质量","智慧生产","智慧招商","智慧应急","智慧医疗","智慧应急","智慧物流","智慧警务","智慧教育","智慧交通")
    for(i <- arr) {
      //println(i)
    }
    val s = "/bigDataHome|大数据首页"
    println(s.split("\\|")(1))
  }

}
