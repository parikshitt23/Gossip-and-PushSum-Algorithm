import scala.math._
import scala.util.Random
import akka.actor.Actor
import akka.actor.Props
import akka.actor._
import akka.actor.ActorRef
import akka.actor.ActorSystem
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import java.util.concurrent.TimeUnit;
import akka.util

/**
 * @author nikhil
 */

case class initNodes(nodeId: String, nodeArray: Array[Array[Array[ActorRef]]], rumorLimit: Int, meshSize: Int, master: ActorRef, s: Double, w: Double, topology: String, failureParameter: Int)
case object SendGossip
case object TickerGossip
case object TickerPushSum
case object KillNodeGossip
case object KillNodePushSum
case class TimeKeeper3d(nodeStopped: Boolean)
case class initBuildTime(startTime: Long, totalNodes: Int,failureParameter:Int, numNodes:Int)

case class pushSumAlgo(s: Double, w: Double)

class timeCalculator extends Actor {
  var startTime: Long = 0
  var totalNodes: Int = 0
  var stoppedNodes: Int = 0
  var failureParameter:Int = 0
  var numNodes:Int = 0
  var terminatingCondition:Int = 0

  def receive = {
    case initBuildTime(systemTime: Long, nodeCount: Int,failures:Int,inputNodes:Int) => {
      startTime = systemTime
      totalNodes = nodeCount
      failureParameter = failures
      numNodes = inputNodes
    }

    case TimeKeeper3d(newNodeStopped: Boolean) => {
      if(failureParameter!=0){
        terminatingCondition = totalNodes - numNodes/failureParameter
        }else{
          terminatingCondition = totalNodes
        }
      if (newNodeStopped) {
        stoppedNodes += 1
        println("No of Stopped Nodes:" + stoppedNodes + " Time: " + (System.currentTimeMillis - startTime))
        if (stoppedNodes == terminatingCondition) {
          println("Time taken for convergence:" + (System.currentTimeMillis - startTime))
          if(failureParameter!=0){println("NUmber of Failed Nodes: "+(totalNodes - terminatingCondition))}
        }
      }
    }

  }

}

class nodeActor extends Actor {

  var nodeId: String = null
  var nodeArray: Array[Array[Array[ActorRef]]] = null
  var rumorLimit: Int = 10
  var rumourCount: Int = 0
  var meshSize: Int = 0
  var tickerGossip: Cancellable = null
  var stoppedNodes: Int = 0
  var master: ActorRef = null
  var topology: String = null

  var s: Double = 0
  var w: Double = 0
  var swRatioOld: Double = 0
  var swRatioNew: Double = 0
  var pushSumLimit: Double = math.pow(10, -10)
  var tickerPushSum: Cancellable = null
  var terminationCounter: Int = 0
  var failureParameter: Int = 0
  var nodeKillerGossip: Cancellable = null
  var nodeKillerpushSum: Cancellable = null
  var uniqueId:Int = 0
  import context.dispatcher

  def receive = {

    case initNodes(nid: String, nodes: Array[Array[Array[ActorRef]]], rumorCondition: Int, cubeSize: Int, masterFromMain: ActorRef, sum: Double, weight: Double, topoType: String, failure: Int) => {
      nodeId = nid
      nodeArray = nodes
      rumorLimit = rumorCondition
      meshSize = cubeSize
      master = masterFromMain
      s = sum
      w = weight
      topology = topoType
      failureParameter = failure
      //println(s +" "+w)
    }

    case SendGossip => {
      tickerGossip = context.system.scheduler.schedule(FiniteDuration(0, TimeUnit.MILLISECONDS), FiniteDuration(5, TimeUnit.MILLISECONDS), self, TickerGossip)
      nodeKillerGossip = context.system.scheduler.scheduleOnce(FiniteDuration(0, TimeUnit.MILLISECONDS), self, KillNodeGossip)
    }

    case TickerGossip => {
      if (rumourCount < rumorLimit) {
        rumourCount += 1
        var nextNodeCoordinates = Array(0, 0, 0)
        //println(nodeId)
        nextNodeCoordinates = findNextNeighbor
        //println(nodeId+ " sending Gossip to " + nextNodeCoordinates(0) + nextNodeCoordinates(1) + nextNodeCoordinates(2))
        nodeArray(nextNodeCoordinates(0))(nextNodeCoordinates(1))(nextNodeCoordinates(2)) ! SendGossip
      } else if (rumourCount == rumorLimit) {
        //println("Stopping" + nodeId + "  " + rumourCount)
        tickerGossip.cancel()
        context.stop(self)
        master ! TimeKeeper3d(true)
      }

    }

    case KillNodeGossip => {
      if (s.toInt % failureParameter == 0) {
        tickerGossip.cancel()
        context.stop(self)

      }

    }
    case KillNodePushSum => {
      if (uniqueId % failureParameter == 0) {
        tickerPushSum.cancel()
        context.stop(self)
      }
    }

    case pushSumAlgo(sum: Double, weight: Double) => {
      swRatioOld = sum / weight
      rumourCount += 1
      if (rumourCount == 1){uniqueId=s.toInt}
      s += sum;
      w += weight;
      swRatioNew = s / w
      tickerPushSum = context.system.scheduler.schedule(FiniteDuration(0, TimeUnit.MILLISECONDS), FiniteDuration(5, TimeUnit.MILLISECONDS), self, TickerPushSum)
      nodeKillerpushSum = context.system.scheduler.scheduleOnce(FiniteDuration(0, TimeUnit.MILLISECONDS), self, nodeKillerpushSum)
    }

    case TickerPushSum => {
      if (rumourCount == 1 || math.abs((swRatioOld - swRatioNew)) > pushSumLimit) {
        //println("Inside Pushsum...")
        terminationCounter = 0;
        s = s / 2;
        w = w / 2;
        var nextNodeCoordinates = Array(0, 0, 0)
        nextNodeCoordinates = findNextNeighbor
        //println(nodeId + " sending pushSum to " + nextNodeCoordinates(0) + nextNodeCoordinates(1) + nextNodeCoordinates(2) + "  Sum:" + s + "  Weight:" + w)
        nodeArray(nextNodeCoordinates(0))(nextNodeCoordinates(1))(nextNodeCoordinates(2)) ! pushSumAlgo(s, w)

      } else {
        terminationCounter += 1
        if (terminationCounter > 3) {
          //println("Stopping" + nodeId + " " + swRatioNew+"  "+math.abs((swRatioOld - swRatioNew)))
          tickerPushSum.cancel()
          context.stop(self)
          master ! TimeKeeper3d(true)
        }

      }

    }

  }

  def findNextNeighbor: Array[Int] = {
    //println(nodeId)
    //println("nodeId at findNeighbor" + nodeId)
    var nodeCoordinates = Array(0, 0, 0)
    var neighborFinder = Array(1, -1)
    if (topology == "3d") {
      for (i <- 0 to 2) {
        nodeCoordinates(i) = nodeId.split(",")(i).toInt
      }
      //var nextDirection = Random.nextInt(nodeCoordinates.length);
      var nextDirection = Random.shuffle(Array(0, 1, 2).toList).head
      if (nodeCoordinates(nextDirection) > 0 && nodeCoordinates(nextDirection) < meshSize - 1) {
        nodeCoordinates(nextDirection) = nodeCoordinates(nextDirection) + Random.shuffle(Array(1, -1).toList).head
      } else if (nodeCoordinates(nextDirection) == 0)
        nodeCoordinates(nextDirection) = nodeCoordinates(nextDirection) + 1
      else if (nodeCoordinates(nextDirection) == meshSize - 1) {
        nodeCoordinates(nextDirection) = nodeCoordinates(nextDirection) - 1
      }
    } else if (topology == "full") {
      nodeCoordinates(0) = nodeId.toInt

      var nextDirection = Random.nextInt(meshSize);
      if (nextDirection == nodeCoordinates(0)) { nextDirection + 1 }
      nodeCoordinates(0) = nextDirection
    } else if (topology == "line") {
      nodeCoordinates(0) = nodeId.toInt

      if (nodeCoordinates(0) > 0 && nodeCoordinates(0) < meshSize - 1) {
        nodeCoordinates(0) = nodeCoordinates(0) + neighborFinder(Random.nextInt(neighborFinder.length))
      } else if (nodeCoordinates(0) == 0)
        nodeCoordinates(0) = nodeCoordinates(0) + 1
      else if (nodeCoordinates(0) == meshSize - 1) {
        nodeCoordinates(0) = nodeCoordinates(0) - 1
      }
    } else if (topology == "imp3d") {
      for (i <- 0 to 2) {
        nodeCoordinates(i) = nodeId.split(",")(i).toInt
      }
      if (Random.nextInt(8) != 1 || (meshSize % 2 != 0 && nodeCoordinates(0) == meshSize - 1 && nodeCoordinates(1) == meshSize - 1 && nodeCoordinates(2) == meshSize - 1)) {
        var nextDirection = Random.nextInt(nodeCoordinates.length);

        if (nodeCoordinates(nextDirection) > 0 && nodeCoordinates(nextDirection) < meshSize - 1) {
          nodeCoordinates(nextDirection) = nodeCoordinates(nextDirection) + neighborFinder(Random.nextInt(neighborFinder.length))
        } else if (nodeCoordinates(nextDirection) == 0)
          nodeCoordinates(nextDirection) = nodeCoordinates(nextDirection) + 1
        else if (nodeCoordinates(nextDirection) == meshSize - 1) {
          nodeCoordinates(nextDirection) = nodeCoordinates(nextDirection) - 1
        }
      } else if (meshSize % 2 == 0) {
        //println("Inside Random 1")
        for (i <- 0 to 1) {
          if (nodeCoordinates(i) % 2 == 0) { nodeCoordinates(i) += 1 }
          else { nodeCoordinates(i) -= 1 }
        }

      } else if (meshSize % 2 != 0) {
        //println("Inside Random 2")
        if ((nodeCoordinates(0) == meshSize - 1) && (nodeCoordinates(1) == meshSize - 1)) {
          if (nodeCoordinates(2) % 4 == 0 || nodeCoordinates(2) % 4 == 1) nodeCoordinates(2) += 2
          if (nodeCoordinates(2) % 4 == 1 || nodeCoordinates(2) % 4 == 3) nodeCoordinates(2) -= 2
        } else if ((nodeCoordinates(1) == meshSize - 1) && (nodeCoordinates(2) == meshSize - 1)) {
          if (nodeCoordinates(0) % 4 == 0 || nodeCoordinates(0) % 4 == 1) nodeCoordinates(0) += 2
          if (nodeCoordinates(0) % 4 == 1 || nodeCoordinates(0) % 4 == 3) nodeCoordinates(0) -= 2
        } else if ((nodeCoordinates(0) == meshSize - 1) && (nodeCoordinates(2) == meshSize - 1)) {
          if (nodeCoordinates(1) % 4 == 0 || nodeCoordinates(1) % 4 == 1) nodeCoordinates(1) += 2
          if (nodeCoordinates(1) % 4 == 1 || nodeCoordinates(1) % 4 == 3) nodeCoordinates(1) -= 2
        } else if (nodeCoordinates(0) == meshSize - 1) {
          for (i <- 1 to 2) {
            if (nodeCoordinates(i) % 2 == 0) { nodeCoordinates(i) += 1 }
            else { nodeCoordinates(i) -= 1 }
          }
        } else if (nodeCoordinates(1) == meshSize - 1) {
          for {
            i <- 0 to 2
            if i != 1
          } {
            if (nodeCoordinates(i) % 2 == 0) { nodeCoordinates(i) += 1 }
            else { nodeCoordinates(i) -= 1 }
          }
        } else {
          //println("Inside new block")
          for (i <- 0 to 1) {
            if (nodeCoordinates(i) % 2 == 0) { nodeCoordinates(i) += 1 }
            else { nodeCoordinates(i) -= 1 }
          }

        }

      }

    }

    return nodeCoordinates
  }
}

object project2Bonus extends App {
  if (args.length == 0 || args.length != 4) {
    println("Wrong Arguments");
  } else {
    //declare string constants
    val full: String = "full";
    val threeD: String = "3d";
    val line: String = "line";
    val imp3D: String = "imp3d";
    val gossip: String = "gossip";
    val pushSum: String = "push-sum"

    var numNodes: Int = args(0).toInt
    var topology: String = args(1).toLowerCase()
    var algorithm: String = args(2).toLowerCase()
    var failureParameter: Int = args(3).toInt
    var size: Int = 0
    var rumorLimit = 10

    val system = ActorSystem("NodesSystem")
    var master = system.actorOf(Props[timeCalculator])

    if (topology == full || topology == line) {
      var nodes = Array.ofDim[ActorRef](numNodes, numNodes, numNodes)
      var sumAssign: Double = 0

      //println(size)
      for {
        i <- 0 until numNodes
      } nodes(i)(0)(0) = system.actorOf(Props[nodeActor])

      for {
        i <- 0 until numNodes
      } {
        var nodeId = i.toString()
        sumAssign += 1
        nodes(i)(0)(0) ! initNodes(nodeId, nodes, rumorLimit, numNodes, master, sumAssign, 1, topology, failureParameter)
      }

      //initiate Gossip
      if (algorithm == gossip) {
        nodes(0)(0)(0) ! SendGossip
        master ! initBuildTime(System.currentTimeMillis, numNodes,failureParameter,numNodes)
      }
      if (algorithm == pushSum) {
        nodes(0)(0)(0) ! pushSumAlgo(1, 1)
        master ! initBuildTime(System.currentTimeMillis, numNodes,failureParameter,numNodes)
      }

    }

    //Topology 3D or imp3D
    if (topology == threeD || topology == imp3D) {
      size = math.floor(math.cbrt(numNodes.toDouble)).toInt
      var nodes = Array.ofDim[ActorRef](size, size, size)
      var sumAssign: Double = 0
      //Create nodes for 3d topology

      //println(size)
      for {
        i <- 0 until size
        j <- 0 until size
        k <- 0 until size
      } nodes(i)(j)(k) = system.actorOf(Props[nodeActor])

      for {
        i <- 0 until size
        j <- 0 until size
        k <- 0 until size
      } {
        var nodeId = i.toString() + ',' + j.toString() + ',' + k.toString()
        sumAssign += 1
        nodes(i)(j)(k) ! initNodes(nodeId, nodes, rumorLimit, size, master, sumAssign, 1, topology, failureParameter)
      }

      //initiate Gossip
      if (algorithm == gossip) {
        nodes(0)(0)(0) ! SendGossip
        var numberNodes: Int = math.pow(size, 3).toInt
        master ! initBuildTime(System.currentTimeMillis, numberNodes,failureParameter,numberNodes)
      }
      if (algorithm == pushSum) {
        nodes(0)(0)(0) ! pushSumAlgo(0, 0)
        var numberNodes: Int = math.pow(size, 3).toInt
        master ! initBuildTime(System.currentTimeMillis, numberNodes,failureParameter,numberNodes)
      }

    }

  }

}