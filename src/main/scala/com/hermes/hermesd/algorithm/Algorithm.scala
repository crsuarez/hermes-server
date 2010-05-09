package com.hermes.hermesd.algorithm

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap 
import scala.collection.mutable.ArrayBuffer
import java.lang.Comparable
import java.util.PriorityQueue 
import java.util.ArrayList
import com.nodeta.scalandra.serializer.StringSerializer
import com.nodeta.scalandra._

object DBConnection{
	val serialization = new Serialization(
		StringSerializer,
		StringSerializer,
		StringSerializer
	)
	val cassandra = new Client(
		Connection("127.0.0.1", 9160),
		"Keyspace1",
		serialization,
		ConsistencyLevels.one
	)
}

class Node(var dbId: String) extends Comparable[Node]{
	var gScore = 0.0
	var hScore = 0.0
	var fScore = 0.0
	var cameFrom = "-1"

	def compareTo(other: Node): Int = { fScore.compare(other.fScore) }

	def getNeighbors(): Iterator[String] = { DBConnection.cassandra.SuperColumnFamily("Super1")(dbId).keys }

	def aString(complet: Boolean): String = {
		if(complet == true){
			"(gScore, hScore, fScore) = (" + gScore + "," + hScore + "," + fScore + ") --- " +
			aString(false)
		}
		else{
			DBConnection.cassandra.ColumnFamily("Standard1")(dbId)("Lat") + "_" +  DBConnection.cassandra.ColumnFamily("Standard1")(dbId)("Lon")   
		}
	}
}


class AStar(var minCost: Double){

	//def heuristicStimateOfDistance(a: List[Int], b: List[Int]) = minCost * (Math.abs(a(0) - b(0)) + Math.abs(a(1) - b(1)))
	def heuristicStimateOfDistance(aI : String, bI: String): Double = {

		var a = DBConnection.cassandra.ColumnFamily("Standard1")(aI)
		var b = DBConnection.cassandra.ColumnFamily("Standard1")(bI)	
		var r = minCost * (Math.abs(a("Lat").toDouble - b("Lat").toDouble) + Math.abs(a("Lon").toDouble - b("Lon").toDouble))
		r
	}

	def buildPath(current: Node, closedset: HashMap[String, Node]): String = {
		if (current.cameFrom != "-1"){	
			return buildPath(closedset(current.cameFrom), closedset)  + ";" +current.aString(false)
		}
		else{
			return current.aString(false)
		}   
	}

	def calculatePath(start: Map[String, String], goal: Map[String, String], hora: Int): String = {

		var closedset = new HashMap[String, Node]()
		var openset = new PriorityQueue[Node]()
		var startId = NearestNeighbor.find(start,0.00001)(0)._1
		var endId = NearestNeighbor.find(goal,0.00001)(0)._1

		openset.add(new Node(startId))

		while(openset.isEmpty() == false){

			var x:Node = openset.poll()
			if(x.dbId == endId){
				//return buildPath(x,closedset)
								
				closedset(x.dbId) = x
				()
			}
			else{

				closedset(x.dbId) = x
				
				/*
				   El nodo x debe tener un metodo que leyendo la informacion de la base de datos
				   defina una coleccion de sus nodos adyacentes, y regrese ya sea una coleccion
				   de los mismos o un iterador sobre ella.
				   Todo el lio ese de la prelacion de las calles puede manejarse en el cuerpo de
				   ese metodo

				   Talvez sea buena idea definir una clase envoltorio que encapsule el nodo adyacente 
				   y el costo para llegar a el, y devolver una coleccion (o su iterador) de instancias
				   de esa clase
				*/

				var i = x.getNeighbors();
				//var j = x.costsAdjacents.iterator();
				while(i.hasNext) {
					var currentNode:Node = new Node(i.next)
					var dist:Double = DBConnection.cassandra.SuperColumnFamily("Super1")(x.dbId.toString)(currentNode.dbId.toString)(hora.toString).toDouble
					if(closedset.contains(currentNode.dbId) == false){
						var tgScore = x.gScore + dist
						var tIsBetter = false
						if(openset.contains(currentNode) == false){
							openset.add(currentNode)
							tIsBetter = true
						}
						else{
							if(tgScore < currentNode.gScore){
								tIsBetter = true
							}
							else{
								tIsBetter = false
							}
						}
						if(tIsBetter){
							currentNode.cameFrom_=(x.dbId)
							currentNode.gScore_=(tgScore)
							currentNode.hScore_=(heuristicStimateOfDistance(x.dbId, currentNode.dbId))
							currentNode.fScore_=(currentNode.gScore + currentNode.hScore)
						}
					}
				}
			}
		}
		return buildPath(closedset(endId),closedset)
	}
}

