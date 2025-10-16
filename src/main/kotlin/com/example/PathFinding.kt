package com.example

import kotlin.math.abs

class PathFinding(
    private val acceptableDistanceToTarget: Int = 1
) {

    fun findPath(board: List<List<String>>, units: List<CharacterUnit>, start: Coordinates, target: Coordinates): List<Coordinates>? {
        if (distanceToTarget(start, target) <= acceptableDistanceToTarget) {
            return emptyList()
        }
        val routes = mutableListOf<List<Coordinates>>()
        legalTiles(units, board, start).forEach { legalTile ->
            routes.add(listOf(legalTile))
        }

        var iterations = 0

        while (routes.isNotEmpty() && iterations < 100) {
            iterations++
            val route = popShortestRoute(routes, target)
            if (distanceToTarget(route.last(), target) <= acceptableDistanceToTarget) {
                return route
            }
            legalTiles(units, board, route.last()).forEach { legalTile ->
                routes.add(route + legalTile)
            }
        }

        return null
    }

    private fun popShortestRoute(routes: MutableList<List<Coordinates>>, target: Coordinates): List<Coordinates> {
        val firstRoute = routes.first()
        var (shortestRouteIndex, shortestRouteLength) = Pair(0, firstRoute.size + distanceToTarget(firstRoute.last(), target))
        for (i in 1 until routes.size) {
            val route = routes[i]
            val distance = route.size + distanceToTarget(route.last(), target)
            if (distance < shortestRouteLength) {
                shortestRouteIndex = i
                shortestRouteLength = distance
            }
        }
        return routes.removeAt(shortestRouteIndex)
    }

    private fun legalTiles(units: List<CharacterUnit>, board: List<List<String>>, coordinates: Coordinates): List<Coordinates> {
        val (x, y) = coordinates
        val terrain = board[y][x]
        val legalMoves = mutableListOf<Coordinates>()
        if (terrain != "grass") {
            if (x > 0) {
                val coordinates = Coordinates(x - 1, y)
                if (isSquareAvailable(units, coordinates, board)) {
                    legalMoves.add(coordinates)
                }
            }
            if (x < board[0].size - 1) {
                val coordinates = Coordinates(x + 1, y)
                if (isSquareAvailable(units, coordinates, board)) {
                    legalMoves.add(coordinates)
                }
            }
            if (y > 0) {
                val coordinates = Coordinates(x, y - 1)
                if (isSquareAvailable(units, coordinates, board)) {
                    legalMoves.add(coordinates)
                }
            }
            if (y < board.size - 1) {
                val coordinates = Coordinates(x, y + 1)
                if (isSquareAvailable(units, coordinates, board)) {
                    legalMoves.add(coordinates)
                }
            }
        }
        else {
            for (x1 in (x - 1)..(x + 1)) {
                if (x1 < 0 || x1 > board[0].size - 1) {
                    continue
                }
                for (y1 in (y - 1)..(y + 1)) {
                    if (y1 < 0 || y1 > board.size - 1 || (x1 == x && y1 == y)) {
                        continue
                    }
                    val coordinates = Coordinates(x1, y1)
                    if (isSquareAvailable(units, coordinates, board)) {
                        legalMoves.add(coordinates)
                    }
                }
            }
        }

        return legalMoves
    }

    private fun distanceToTarget(start: Coordinates, target: Coordinates): Int {
        return abs(target.x - start.x) + abs(target.y - start.y)
    }

    private fun isSquareAvailable(units: List<CharacterUnit>, coordinates: Coordinates, board: List<List<String>>): Boolean {
        for (unit in units) {
            if (unit.toCoordinates() == coordinates) {
                return false
            }
        }
        val terrain = board[coordinates.y][coordinates.x]
        return terrain != "hole" && terrain != "thorns"
    }

}