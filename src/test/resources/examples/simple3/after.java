package examples.simple;

public class X {
	private void processMovement(megamek.server.Entity entity, megamek.server.MovementData md) {
    boolean infMoveMulti = game.getOptions().booleanOption("inf_move_multi");
    boolean infMoveLast = game.getOptions().booleanOption("inf_move_last");
    if ((infMoveMulti && (turnInfMoved > 0)) && ((!(entity instanceof megamek.server.Infantry)) || (entity.getOwnerId() != turnLastPlayerId))) {
    } else if ((infMoveLast && (turnInfMoved > 0)) && (!(entity instanceof megamek.server.Infantry))) {
    }
}
}