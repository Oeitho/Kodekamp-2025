package com.example

data class TurnDTO(
    val turnNumber: Int,
    val yourId: String,
    val enemyUnits: List<CharacterUnit>,
    val friendlyUnits: List<CharacterUnit>,
    val uuid: String,
    val board: List<List<String>>?,
    val boardSize: BoardSize,
    val moveActionsAvailable: Int,
    val attackActionsAvailable: Int
) {

    fun board(): List<List<String>> {
        if (board != null) {
            return board
        }

        val board = mutableListOf<MutableList<String>>()
        for (_y in 0 until boardSize.h) {
            val row = mutableListOf<String>()
            for (_x in 0 until boardSize.w) {
                row.add("mud")
            }
            board.add(row)
        }
        return board
    }

    fun units(): List<CharacterUnit> {
        return this.friendlyUnits + this.enemyUnits
    }

}

data class CharacterUnit(
    var x: Int,
    var y: Int,
    var moves: Int,
    val maxHealth: Int,
    val attackStrength: Int,
    val id: String,
    val kind: String,
    var health: Int,
    val side: String,
    var armor: Int,
    var attacks: Int,
    val isPiercing: Boolean = false,
    val range: Int = 1
) {

    fun toCoordinates(): Coordinates {
        return Coordinates(x, y)
    }

    fun updateCoordinates(coordinates: Coordinates) {
        this.x = coordinates.x
        this.y = coordinates.y
    }

    fun reduceHealthAndArmor(damage: Int, armorPearcing: Boolean) {
        if (armorPearcing) {
            health -= damage
        } else {
            armor -= if (damage > armor) 0 else damage
            health -= (damage - armor)
        }
    }

    fun reduceAttacks() {
        attacks--
    }

    fun reduceMoves(movesToReduce: Int = 1) {
        moves -= movesToReduce
    }

}

data class BoardSize(
    val w: Int,
    val h: Int
)

data class Coordinates(
    val x: Int,
    val y: Int,
)