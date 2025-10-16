package com.example

import jakarta.enterprise.context.ApplicationScoped
import kotlin.math.abs
import kotlin.math.min

@ApplicationScoped
class GameEngine() {

    fun handleTurn(turn: TurnDTO): List<OrderDTO> {
        var movesLeft = turn.moveActionsAvailable
        var attacksLeft = turn.attackActionsAvailable

        val ordreListe = mutableListOf<OrderDTO>()

        var iterations = 0

        while ((movesLeft > 0 || attacksLeft > 0) && iterations < 10) {
            iterations++
            val ordreListeThisIteration = mutableListOf<OrderDTO>()
            for (friendlyCharacterUnit in turn.friendlyUnits) {
                val closestOpponent = closestOpponentNew(turn, friendlyCharacterUnit, turn.enemyUnits) ?: continue
                if (canAttackUnit(attacksLeft, friendlyCharacterUnit, closestOpponent)) {
                    val orders = attackNumberOfTimes(attacksLeft, friendlyCharacterUnit, closestOpponent)
                    ordreListeThisIteration.addAll(orders)
                    attacksLeft -= orders.size
                    if (attacksLeft <= 0) {
                        break
                    }
                    continue
                } else {
                    if (!canReachOpponent(turn, movesLeft, friendlyCharacterUnit, closestOpponent)) {
                        continue
                    }
                    val (moves, coordinates) = movesToEnemy(turn, friendlyCharacterUnit, closestOpponent)
                    val oldCoordinates = friendlyCharacterUnit.toCoordinates()
                    friendlyCharacterUnit.updateCoordinates(coordinates)
                    if (!canAttackUnit(attacksLeft, friendlyCharacterUnit, closestOpponent)) {
                        friendlyCharacterUnit.updateCoordinates(oldCoordinates)
                        continue
                    }
                    friendlyCharacterUnit.reduceMoves(moves.size)
                    movesLeft -= moves.size
                    ordreListeThisIteration.addAll(moves)
                    val orders = attackNumberOfTimes(attacksLeft, friendlyCharacterUnit, closestOpponent)
                    ordreListeThisIteration.addAll(orders)
                    attacksLeft -= orders.size
                    if (attacksLeft <= 0) {
                        break
                    }
                }

                if (ordreListeThisIteration.isEmpty()) {
                    for (friendlyCharacterUnit in turn.friendlyUnits) {
                        if (friendlyCharacterUnit.moves > 0) {
                            val pathFinding = PathFinding()
                            val path = pathFinding.findPath(turn.board(), turn.units(), friendlyCharacterUnit.toCoordinates(), closestOpponent.toCoordinates())
                            if (path != null && path.isNotEmpty() && friendlyCharacterUnit.moves > 0 && movesLeft > 0) {
                                val firstMove = path.first()
                                ordreListe.add(
                                    OrderDTO(
                                        unit = friendlyCharacterUnit.id,
                                        action = "move",
                                        x = firstMove.x,
                                        y = firstMove.y,
                                    )
                                )
                                friendlyCharacterUnit.reduceMoves()
                                movesLeft -= 1
                            }
                        }
                    }
                }

                ordreListe.addAll(ordreListeThisIteration)
                ordreListeThisIteration.clear()
            }

            ordreListe.addAll(ordreListeThisIteration)
            ordreListeThisIteration.clear()

            if (ordreListe.isEmpty()) {
                for (friendlyCharacterUnit in turn.friendlyUnits) {
                    val closestOpponent = closestOpponent(friendlyCharacterUnit, turn.enemyUnits) ?: continue
                    val pathFinding = PathFinding()
                    val path = pathFinding.findPath(turn.board(), turn.units(), friendlyCharacterUnit.toCoordinates(), closestOpponent.toCoordinates())
                    if (path != null && path.isNotEmpty() && friendlyCharacterUnit.moves > 0 && movesLeft > 0) {
                        val firstMove = path.first()
                        ordreListe.add(
                            OrderDTO(
                                unit = friendlyCharacterUnit.id,
                                action = "move",
                                x = firstMove.x,
                                y = firstMove.y,
                            )
                        )
                        friendlyCharacterUnit.reduceMoves()
                        movesLeft -= 1
                    }
                }
            }
        }

        return ordreListe
    }

    fun movesToEnemy(turnDTO: TurnDTO, friendlyCharacterUnit: CharacterUnit, enemyCharacter: CharacterUnit): Pair<List<OrderDTO>, Coordinates> {
        val enemyCoordinates = enemyCharacter.toCoordinates()
        val friendlyCoordinates = friendlyCharacterUnit.toCoordinates()
        val pathFinding = PathFinding()
        val orders = pathFinding.findPath(turnDTO.board(), turnDTO.units(), friendlyCoordinates, enemyCoordinates)
            ?.map { coordinate ->
                OrderDTO(
                    unit = friendlyCharacterUnit.id,
                    action = "move",
                    x = coordinate.x,
                    y = coordinate.y
                )
            } ?: emptyList()
        val coordinates = if (orders.isNotEmpty()) {
            val lastCoordinates = orders.last()
            Coordinates(lastCoordinates.x, lastCoordinates.y)
        } else {
            friendlyCoordinates
        }
        return Pair(orders, coordinates)
    }

    fun attackNumberOfTimes(attacksLeft: Int, friendly: CharacterUnit, enemy: CharacterUnit): List<OrderDTO> {
        val orders = mutableListOf<OrderDTO>()
        for (attack in 0 until attacksLeft) {
            enemy.reduceHealthAndArmor(friendly.attackStrength, friendly.isPiercing)
            orders.add(OrderDTO(
                friendly.id,
                "attack",
                enemy.x,
                enemy.y
            ))
            friendly.reduceAttacks()
            if (enemy.health <= 0 || friendly.attacks == 0) {
                break
            }
        }

        return orders
    }

    fun distanceToEnemy(friendlyCoordinates: Coordinates, enemyCoordinates: Coordinates): Int {
        return abs(enemyCoordinates.x - friendlyCoordinates.x) + abs(enemyCoordinates.y - friendlyCoordinates.y)
    }

    fun closestOpponent(friendlyCharacterUnit: CharacterUnit, enemyCharacterUnits: List<CharacterUnit>): CharacterUnit? {
        var closestOpponent = enemyCharacterUnits.firstOrNull() ?: return null
        for (enemy in enemyCharacterUnits) {
            if (distanceToEnemy(friendlyCharacterUnit.toCoordinates(), closestOpponent.toCoordinates()) > distanceToEnemy(
                    friendlyCharacterUnit.toCoordinates(),
                    enemy.toCoordinates()
                )
            ) {
                closestOpponent = enemy
            }
        }
        return closestOpponent
    }

    fun closestOpponentNew(turnDTO: TurnDTO, friendlyCharacterUnit: CharacterUnit, enemyCharacterUnits: List<CharacterUnit>): CharacterUnit? {
        val coordinates = friendlyCharacterUnit.toCoordinates()
        val pathFinding = PathFinding()
        val firstEnemy = enemyCharacterUnits.firstOrNull()
        if (firstEnemy == null) {
            return null
        }
        var closestOpponent = Pair(firstEnemy, pathFinding.findPath(turnDTO.board(), turnDTO.units(), coordinates, firstEnemy.toCoordinates())?.size ?: Int.MAX_VALUE)
        for (enemy in enemyCharacterUnits) {
            if (enemy.health <= 0) {
                continue
            }
            val distance = pathFinding.findPath(turnDTO.board(), turnDTO.units(), coordinates, enemy.toCoordinates())?.size ?: Int.MAX_VALUE
            if (distance < closestOpponent.second || closestOpponent.first.health <= 0) {
                closestOpponent = Pair(enemy, distance)
            }
            else if (distance == closestOpponent.second || enemy.health < closestOpponent.first.health) {
                closestOpponent = Pair(enemy, distance)
            }
        }
        return closestOpponent.first
    }




    fun canReachOpponent(
        turn: TurnDTO,
        availableMoves: Int,
        friendlyCharacterUnit: CharacterUnit,
        enemyCharacter: CharacterUnit
    ): Boolean {
        val pathFinding = PathFinding()
        val distanceToEnemy = pathFinding.findPath(turn.board(), turn.units(), friendlyCharacterUnit.toCoordinates(), enemyCharacter.toCoordinates())?.size ?: Int.MAX_VALUE
        return distanceToEnemy <= min(
            friendlyCharacterUnit.moves,
            availableMoves
        )
    }

    fun canAttackUnit(attacksLeft: Int, friendly: CharacterUnit, enemy: CharacterUnit): Boolean {
        if (attacksLeft == 0 || friendly.attacks == 0) {
            return false
        }
        if (friendly.range > 1) {
            if (distanceToEnemy(friendly.toCoordinates(), enemy.toCoordinates()) <= friendly.range) {
                return true
            }
        } else {
            if (distanceToEnemy(friendly.toCoordinates(), enemy.toCoordinates()) > 1) {
                return false
            }
            val canAttackX = abs(friendly.x - enemy.x) <= friendly.range
            if (canAttackX) {
                return true
            }
            val canAttackY = abs(friendly.y - enemy.y) <= friendly.range
            if (canAttackY) {
                return true
            }
        }
        return false
    }
}