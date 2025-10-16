package com.example

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/")
class GameResource(
    val gameEngine: GameEngine
) {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun turn(turn: TurnDTO): List<OrderDTO> {
        return gameEngine.handleTurn(turn)
    }
}