object demo {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(57); 
  println("Welcome to the Scala worksheet");$skip(33); val res$0 = 
  "10,11,12".split(",")(2).toInt;System.out.println("""res0: Int = """ + $show(res$0));$skip(31); val res$1 = 
  scala.util.Random.nextInt(9);System.out.println("""res1: Int = """ + $show(res$1));$skip(22); val res$2 = 
  "imp3D".toLowerCase;System.out.println("""res2: String = """ + $show(res$2))}
}
